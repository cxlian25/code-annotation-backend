package org.example.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.CommentDetailLevel;
import org.example.service.CommentPromptTemplateService;
import org.example.service.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekLlmClient implements LlmClient {
    private final PlaceholderLlmClient fallbackClient;
    private final CommentPromptTemplateService promptTemplateService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    public DeepSeekLlmClient(PlaceholderLlmClient fallbackClient,
                             CommentPromptTemplateService promptTemplateService) {
        this.fallbackClient = fallbackClient;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeDetailLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }

        try {
            CommentPromptTemplateService.PromptPair prompt = promptTemplateService.build(modelInput, safeDetailLevel);
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", prompt.systemPrompt()),
                            Map.of("role", "user", "content", prompt.userPrompt())
                    ),
                    "stream", false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/chat/completions", entity, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                return fallbackClient.generateComment(modelInput, safeDetailLevel);
            }

            JsonNode contentNode = body.path("choices").path(0).path("message").path("content");
            if (contentNode.isTextual() && !contentNode.asText().isBlank()) {
                return contentNode.asText().trim();
            }

            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        } catch (Exception ex) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }
    }
}