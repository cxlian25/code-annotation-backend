package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Primary
public class DeepSeekLlmClient implements LlmClient {
    private static final String SYSTEM_PROMPT = "你是专业的 Java 代码注释助手。请基于给定的代码、AST 和上下文，生成准确、简洁、可读性高的中文注释。";

    private final PlaceholderLlmClient fallbackClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    public DeepSeekLlmClient(PlaceholderLlmClient fallbackClient) {
        this.fallbackClient = fallbackClient;
    }

    @Override
    public String generateComment(String modelInput) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackClient.generateComment(modelInput);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", buildUserPrompt(modelInput))
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
                return fallbackClient.generateComment(modelInput);
            }

            JsonNode contentNode = body.path("choices").path(0).path("message").path("content");
            if (contentNode.isTextual() && !contentNode.asText().isBlank()) {
                return contentNode.asText().trim();
            }

            return fallbackClient.generateComment(modelInput);
        } catch (Exception ex) {
            return fallbackClient.generateComment(modelInput);
        }
    }


    private String buildUserPrompt(String modelInput) {
        return "输入数据（JSON，包含 targetCode、context、ast）：\n"
                + modelInput
                + "\n\n请只返回一条中文代码注释，不要包含多余解释，无需添加注解格式。";
    }
}