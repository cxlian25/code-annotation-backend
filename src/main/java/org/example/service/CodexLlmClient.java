package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.CommentDetailLevel;
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
public class CodexLlmClient implements LlmClient {
    private static final String SYSTEM_PROMPT = "你是一名专业的 Java 代码注释助手。";
    private static final String CONCISE_PROMPT_SUFFIX = "\n\n要求：生成一条简洁的中文代码注释（1 句话），突出核心作用。仅返回注释文本，不要返回注释格式符。";
    private static final String DETAILED_PROMPT_SUFFIX = "\n\n要求：生成一条详细的中文代码注释（2-4 句话），尽量覆盖设计意图、关键逻辑，以及可见的输入输出或副作用。仅返回注释文本，不要返回注释格式符。";

    private final PlaceholderLlmClient fallbackClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${codex.api-key:}")
    private String apiKey;

    @Value("${codex.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${codex.model:}")
    private String model;

    public CodexLlmClient(PlaceholderLlmClient fallbackClient) {
        this.fallbackClient = fallbackClient;
    }

    @Override
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeDetailLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;

        if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", List.of(
                                            Map.of("type", "input_text", "text", buildSystemPrompt(safeDetailLevel))
                                    )
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "input_text", "text", buildUserPrompt(modelInput, safeDetailLevel))
                                    )
                            )
                    ),
                    "store", false,
                    "stream", false
            );

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

    private String buildSystemPrompt(CommentDetailLevel detailLevel) {
        if (detailLevel == CommentDetailLevel.DETAILED) {
            return SYSTEM_PROMPT + " 当请求详细注释时，请给出结构更完整、信息更充分的说明。";
        }
        return SYSTEM_PROMPT + " 当请求简洁注释时，请给出短小直接的说明。";
    }

    private String buildUserPrompt(String modelInput, CommentDetailLevel detailLevel) {
        String basePrompt = "输入 JSON 包含 targetCode、context、ast 和 commentDetailLevel：\n" + modelInput;
        if (detailLevel == CommentDetailLevel.DETAILED) {
            return basePrompt + DETAILED_PROMPT_SUFFIX;
        }
        return basePrompt + CONCISE_PROMPT_SUFFIX;
    }
}
