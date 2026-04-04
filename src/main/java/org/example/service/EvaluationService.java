package org.example.service;

import org.example.dto.EvaluateDatasetRequest;
import org.example.dto.EvaluateDatasetResponse;
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
    private final AnnotationGenerationService annotationGenerationService;
    private final MetricCalculator metricCalculator;

    public EvaluationService(AnnotationGenerationService annotationGenerationService, MetricCalculator metricCalculator) {
        this.annotationGenerationService = annotationGenerationService;
        this.metricCalculator = metricCalculator;
    }

    public EvaluateDatasetResponse evaluate(EvaluateDatasetRequest request) {
        List<String> codeSamples = readAllLines("test.token.code");
        List<String> references = readAllLines("test.token.nl");

        int total = Math.min(codeSamples.size(), references.size());
        if (request != null && request.getMaxSamples() != null && request.getMaxSamples() > 0) {
            total = Math.min(total, request.getMaxSamples());
        }

        if (total == 0) {
            return new EvaluateDatasetResponse(0, 0.0, 0.0, 0.0, 0.0);
        }

        double sentenceBleuSum = 0.0;
        double meteorSum = 0.0;
        double rougeLSum = 0.0;

        List<String> evaluatedReferences = new ArrayList<>(total);
        List<String> predictions = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            String code = codeSamples.get(i);
            String reference = references.get(i);
            String prediction = annotationGenerationService.generateComment(code, null);

            evaluatedReferences.add(reference);
            predictions.add(prediction);

            sentenceBleuSum += metricCalculator.bleu(reference, prediction);
            meteorSum += metricCalculator.meteor(reference, prediction);
            rougeLSum += metricCalculator.rougeL(reference, prediction);
        }

        double corpusBleu = metricCalculator.bleuCorpus(evaluatedReferences, predictions);
        double sentenceBleu = sentenceBleuSum / total;

        return new EvaluateDatasetResponse(
                total,
                round(corpusBleu),
                round(sentenceBleu),
                round(meteorSum / total),
                round(rougeLSum / total)
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
