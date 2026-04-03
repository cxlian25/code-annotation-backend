package org.example.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.CommentDetailLevel;
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
    private static final String SYSTEM_PROMPT = "你是一名专业的 Java 代码注释助手。";
    private static final String CONCISE_PROMPT_SUFFIX = "\n\n要求：生成一条简洁的中文代码注释（1 句话），突出核心作用。仅返回注释文本，不要返回注释格式符。";
    private static final String DETAILED_PROMPT_SUFFIX = "\n\n要求：生成一条详细的中文代码注释（2-4 句话），尽量覆盖设计意图、关键逻辑，以及可见的输入输出或副作用。仅返回注释文本，不要返回注释格式符。";

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
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeDetailLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackClient.generateComment(modelInput, safeDetailLevel);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", buildSystemPrompt(safeDetailLevel)),
                            Map.of("role", "user", "content", buildUserPrompt(modelInput, safeDetailLevel))
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

    private String buildSystemPrompt(CommentDetailLevel detailLevel) {
        if (detailLevel == CommentDetailLevel.DETAILED) {
            return SYSTEM_PROMPT + " 当请求详细注释时，请给出结构更完整、信息更充分的说明。";
        }
        return SYSTEM_PROMPT + " 当请求简洁注释时，请给出短小直接的说明。";
    }

    private String buildUserPrompt(String modelInput, CommentDetailLevel detailLevel) {
        String basePrompt = "输入 JSON 包含 targetCode、context、ast 和 commentDetailLevel：\n"
                + modelInput;

        if (detailLevel == CommentDetailLevel.DETAILED) {
            return basePrompt + DETAILED_PROMPT_SUFFIX;
        }
        return basePrompt + CONCISE_PROMPT_SUFFIX;
    }
}
