package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.EvaluateDatasetRequest;
import org.example.dto.EvaluateDatasetResponse;
import org.example.dto.GenerateAnnotationRequest;
import org.example.dto.GenerateAnnotationResponse;
import org.example.service.AnnotationGenerationService;
import org.example.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/annotation")
public class AnnotationController {
    private static final Logger log = LoggerFactory.getLogger(AnnotationController.class);
    private static final int LOG_PREVIEW_LEN = 240;

    private final AnnotationGenerationService annotationGenerationService;
    private final EvaluationService evaluationService;

    public AnnotationController(AnnotationGenerationService annotationGenerationService, EvaluationService evaluationService) {
        this.annotationGenerationService = annotationGenerationService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @PostMapping("/generate")
    public GenerateAnnotationResponse generate(@Valid @RequestBody GenerateAnnotationRequest request) {
        log.info("[/generate] request targetCode={}, context={}", abbreviate(request.getTargetCode()), abbreviate(request.getContext()));

//        return new GenerateAnnotationResponse("222","一加一等于二");
        GenerateAnnotationResponse response = annotationGenerationService.generate(request);

        log.info(
                "[/generate] response generatedComment={}, modelInputLength={}",
                abbreviate(response.getGeneratedComment()),
                safeLength(response.getModelInput())
        );

        return response;
    }

    @PostMapping("/evaluate")
    public EvaluateDatasetResponse evaluate(@RequestBody(required = false) EvaluateDatasetRequest request) {
        EvaluateDatasetRequest safeRequest = request == null ? new EvaluateDatasetRequest() : request;
        log.info("[/evaluate] request maxSamples={}", safeRequest.getMaxSamples());

        EvaluateDatasetResponse response = evaluationService.evaluate(safeRequest);

        log.info(
                "[/evaluate] response sampleCount={}, bleu={}, meteor={}, rougeL={}",
                response.getSampleCount(),
                response.getBleu(),
                response.getMeteor(),
                response.getRougeL()
        );

        return response;
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= LOG_PREVIEW_LEN) {
            return normalized;
        }
        return normalized.substring(0, LOG_PREVIEW_LEN) + "...";
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
