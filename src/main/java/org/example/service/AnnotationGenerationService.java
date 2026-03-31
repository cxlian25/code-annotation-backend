package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.GenerateAnnotationRequest;
import org.example.dto.GenerateAnnotationResponse;
import org.example.model.AstNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AnnotationGenerationService {
    private final AstParserService astParserService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AnnotationGenerationService(AstParserService astParserService, LlmClient llmClient, ObjectMapper objectMapper) {
        this.astParserService = astParserService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public GenerateAnnotationResponse generate(GenerateAnnotationRequest request) {
        AstNode ast = astParserService.parseToAst(request.getTargetCode());

        Map<String, Object> modelInputMap = new HashMap<>();
        modelInputMap.put("targetCode", request.getTargetCode());
        modelInputMap.put("context", request.getContext() == null ? "" : request.getContext());
        modelInputMap.put("ast", ast);

        String modelInput = toJson(modelInputMap);
        String generatedComment = llmClient.generateComment(modelInput);

        return new GenerateAnnotationResponse(modelInput, generatedComment);
    }

    public String generateComment(String targetCode, String context) {
        GenerateAnnotationRequest request = new GenerateAnnotationRequest();
        request.setTargetCode(targetCode);
        request.setContext(context);
        return generate(request).getGeneratedComment();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON payload", e);
        }
    }
}
