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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodexLlmClient implements LlmClient {
    private final PlaceholderLlmClient fallbackClient;
    private final CommentPromptTemplateService promptTemplateService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${codex.api-key:}")
    private String apiKey;

    @Value("${codex.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${codex.model:}")
    private String model;

    @Value("${codex.enable-thinking:true}")
    private boolean enableThinking;

    public CodexLlmClient(PlaceholderLlmClient fallbackClient,
                          CommentPromptTemplateService promptTemplateService) {
        this.fallbackClient = fallbackClient;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeDetailLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;

        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }

        try {
            CommentPromptTemplateService.PromptPair prompt = promptTemplateService.build(modelInput, safeDetailLevel);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", List.of(
                    Map.of(
                            "role", "system",
                            "content", List.of(
                                    Map.of("type", "input_text", "text", prompt.systemPrompt())
                            )
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "input_text", "text", prompt.userPrompt())
                            )
                    )
            ));
            requestBody.put("store", false);
            requestBody.put("stream", false);
            if (!enableThinking) {
                requestBody.put("reasoning", Map.of("effort", "minimal"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/responses", entity, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) {
                return fallbackClient.generateComment(modelInput, safeDetailLevel);
            }

            String outputText = extractOutputText(body);
            if (outputText != null && !outputText.isBlank()) {
                return outputText;
            }

            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        } catch (Exception ex) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }
    }

    private String extractOutputText(JsonNode responseBody) {
        JsonNode outputTextNode = responseBody.path("output_text");
        if (outputTextNode.isTextual() && !outputTextNode.asText().isBlank()) {
            return outputTextNode.asText().trim();
        }
        if (outputTextNode.isArray()) {
            StringBuilder merged = new StringBuilder();
            for (JsonNode item : outputTextNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    if (merged.length() > 0) {
                        merged.append('\n');
                    }
                    merged.append(item.asText().trim());
                }
            }
            if (merged.length() > 0) {
                return merged.toString();
            }
        }

        JsonNode output = responseBody.path("output");
        if (output.isArray()) {
            StringBuilder merged = new StringBuilder();
            for (JsonNode messageNode : output) {
                JsonNode content = messageNode.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentNode : content) {
                    if (!"output_text".equals(contentNode.path("type").asText())) {
                        continue;
                    }
                    String text = contentNode.path("text").asText("");
                    if (!text.isBlank()) {
                        if (merged.length() > 0) {
                            merged.append('\n');
                        }
                        merged.append(text.trim());
                    }
                }
            }
            if (merged.length() > 0) {
                return merged.toString();
            }
        }

        JsonNode contentNode = responseBody.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual() && !contentNode.asText().isBlank()) {
            return contentNode.asText().trim();
        }
        return null;
    }
}