package org.example.service;

import org.example.dto.EvaluateDatasetRequest;
import org.example.dto.EvaluateDatasetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {
    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final AnnotationGenerationService annotationGenerationService;
    private final MetricCalculator metricCalculator;
    private final MeteorScoreService meteorScoreService;

    @Value("${llm.provider:}")
    private String llmModel;

    public EvaluationService(AnnotationGenerationService annotationGenerationService,
                             MetricCalculator metricCalculator,
                             MeteorScoreService meteorScoreService) {
        this.annotationGenerationService = annotationGenerationService;
        this.metricCalculator = metricCalculator;
        this.meteorScoreService = meteorScoreService;
    }

    public EvaluateDatasetResponse evaluate(EvaluateDatasetRequest request) {
        List<String> codeSamples = readAllLines("test.token.code");
        List<String> references = readAllLines("test.token.nl");

        int total = Math.min(codeSamples.size(), references.size());
        if (request != null && request.getMaxSamples() != null && request.getMaxSamples() > 0) {
            total = Math.min(total, request.getMaxSamples());
        }

        if (total == 0) {
            return new EvaluateDatasetResponse(0, 0.0, 0.0, 0.0);
        }

        List<String> evaluatedReferences = new ArrayList<>(total);
        List<String> predictions = new ArrayList<>(total);

        log.info("开始测试");

        double timeSum = 0.0;
        for (int i = 0; i < total; i++) {
            log.info("开始处理第 {} 行数据", i + 1);
            String code = codeSamples.get(i);
            String reference = references.get(i);

            long start = System.nanoTime();
            String prediction = annotationGenerationService.generateComment(code, null);
            log.info("输出： {}", prediction);
            long end = System.nanoTime();
            timeSum += end - start;

            if ("codex".equalsIgnoreCase(llmModel)) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("Failed to sleep", e);
                }
            }

            evaluatedReferences.add(reference);
            predictions.add(prediction);

            if ("codex".equalsIgnoreCase(llmModel) && i > 0 && i % 30 == 0) {
                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                    log.error("Failed to sleep", e);
                }
            }
        }

        log.info("{} 行数据已全部获取完毕",total);
        log.info("大模型平均耗时： {}", timeSum / total / 1_000_000.0);

        double corpusBleu = metricCalculator.bleuCorpus(evaluatedReferences, predictions);
        double corpusRougeL = metricCalculator.rougeLCorpus(evaluatedReferences, predictions);
        double meteorAvg = meteorScoreService.averageMeteor(evaluatedReferences, predictions);

        return new EvaluateDatasetResponse(
                total,
                round(corpusBleu),
                round(meteorAvg),
                round(corpusRougeL)
        );
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private List<String> readAllLines(String resourceName) {
        ClassPathResource resource = new ClassPathResource(resourceName);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read dataset file: " + resourceName, e);
        }
        return lines;
    }
}