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
            return new EvaluateDatasetResponse(0, 0.0, 0.0, 0.0);
        }

        // 平均 BLEU 分数（衡量生成注释与参考注释的 n-gram 重合度）
        double bleuSum = 0.0;
        // 平均 METEOR 分数（综合考虑精确率、召回率和片段连续性）
        double meteorSum = 0.0;
        // 平均 ROUGE-L 分数（基于最长公共子序列的相似度）
        double rougeLSum = 0.0;

        for (int i = 0; i < total; i++) {
            String code = codeSamples.get(i);
            String reference = references.get(i);
            String prediction = annotationGenerationService.generateComment(code, null);

            bleuSum += metricCalculator.bleu(reference, prediction);
            meteorSum += metricCalculator.meteor(reference, prediction);
            rougeLSum += metricCalculator.rougeL(reference, prediction);
        }

        return new EvaluateDatasetResponse(
                total,
                round(bleuSum / total),
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
