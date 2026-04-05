package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MeteorScoreService {
    private static final Logger log = LoggerFactory.getLogger(MeteorScoreService.class);

    private final MetricCalculator metricCalculator;
    private final ObjectMapper objectMapper;

    @Value("${meteor.mode:python}")
    private String meteorMode;

    @Value("${meteor.python.command:python}")
    private String pythonCommand;

    @Value("${meteor.python.script:scripts/meteor_batch.py}")
    private String pythonScriptPath;

    @Value("${meteor.python.timeout-ms:120000}")
    private long pythonTimeoutMs;

    @Value("${meteor.python.site-packages:.meteor-py/site-packages}")
    private String pythonSitePackagesPath;

    public MeteorScoreService(MetricCalculator metricCalculator, ObjectMapper objectMapper) {
        this.metricCalculator = metricCalculator;
        this.objectMapper = objectMapper;
    }

    public double averageMeteor(List<String> references, List<String> candidates) {
        if (references == null || candidates == null) {
            return 0.0;
        }

        int total = Math.min(references.size(), candidates.size());
        if (total == 0) {
            return 0.0;
        }

        List<String> refSlice = references.subList(0, total);
        List<String> candSlice = candidates.subList(0, total);

        if (!"python".equalsIgnoreCase(meteorMode)) {
            return averageMeteorJava(refSlice, candSlice);
        }

        Double pythonScore = averageMeteorPython(refSlice, candSlice);
        if (pythonScore != null) {
            return pythonScore;
        }

        log.warn("Python METEOR failed, fallback to Java METEOR implementation.");
        return averageMeteorJava(refSlice, candSlice);
    }

    private double averageMeteorJava(List<String> references, List<String> candidates) {
        int total = Math.min(references.size(), candidates.size());
        if (total == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int i = 0; i < total; i++) {
            sum += metricCalculator.meteor(references.get(i), candidates.get(i));
        }
        return sum / total;
    }

    private Double averageMeteorPython(List<String> references, List<String> candidates) {
        Path tempInput = null;
        try {
            Path scriptPath = resolveScriptPath();
            if (!Files.exists(scriptPath)) {
                log.warn("Python METEOR script not found: {}", scriptPath);
                return null;
            }

            tempInput = Files.createTempFile("meteor-input-", ".json");
            Map<String, Object> payload = new HashMap<>();
            payload.put("references", references);
            payload.put("candidates", candidates);
            Files.writeString(tempInput, objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8);

            List<String> command = buildCommand();
            command.add(scriptPath.toString());
            command.add(tempInput.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            applyPythonPath(processBuilder);
            Process process = processBuilder.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                output = sb.toString();
            }

            boolean finished = process.waitFor(pythonTimeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Python METEOR process timed out after {} ms", pythonTimeoutMs);
                return null;
            }

            if (process.exitValue() != 0) {
                log.warn("Python METEOR process exited with code {}: {}", process.exitValue(), output);
                return null;
            }

            JsonNode root = objectMapper.readTree(output);
            if (!root.has("average")) {
                log.warn("Python METEOR output does not contain average field: {}", output);
                return null;
            }

            return root.path("average").asDouble();
        } catch (Exception ex) {
            log.warn("Failed to calculate METEOR by Python", ex);
            return null;
        } finally {
            if (tempInput != null) {
                try {
                    Files.deleteIfExists(tempInput);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<String> buildCommand() {
        String cmd = pythonCommand == null || pythonCommand.isBlank() ? "python" : pythonCommand.trim();
        List<String> command = splitCommandLine(cmd);
        if (command.isEmpty()) {
            command.add("python");
        }
        return command;
    }

    private List<String> splitCommandLine(String commandLine) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(ch);
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private Path resolveScriptPath() {
        Path configured = Paths.get(pythonScriptPath);
        if (configured.isAbsolute()) {
            return configured;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
    }

    private void applyPythonPath(ProcessBuilder processBuilder) {
        Path sitePackagesPath = resolvePath(pythonSitePackagesPath);
        if (!Files.exists(sitePackagesPath)) {
            return;
        }

        Map<String, String> env = processBuilder.environment();
        String existing = env.getOrDefault("PYTHONPATH", "");
        String separator = System.getProperty("path.separator");
        String merged = existing == null || existing.isBlank()
                ? sitePackagesPath.toString()
                : sitePackagesPath + separator + existing;
        env.put("PYTHONPATH", merged);
    }

    private Path resolvePath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return Paths.get(System.getProperty("user.dir"));
        }
        Path path = Paths.get(configuredPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }
}
