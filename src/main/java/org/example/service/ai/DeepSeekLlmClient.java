package org.example.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.CommentDetailLevel;
import org.example.service.CommentPromptTemplateService;
import org.example.service.LlmClient;
import org.example.service.LlmGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmClient.class);

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
    public LlmGenerationResult generateCommentResult(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeDetailLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 deepseek.api-key");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 deepseek.base-url");
        }
        if (model == null || model.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 deepseek.model");
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
                return fallbackWithReason(modelInput, safeDetailLevel, "接口响应体为空");
            }

            JsonNode contentNode = body.path("choices").path(0).path("message").path("content");
            if (contentNode.isTextual() && !contentNode.asText().isBlank()) {
                return LlmGenerationResult.success(contentNode.asText().trim(), providerLabel());
            }

            return fallbackWithReason(
                    modelInput,
                    safeDetailLevel,
                    "接口响应中未找到有效文本内容，响应片段：" + LlmFallbackSupport.preview(body.toString())
            );
        } catch (RestClientResponseException ex) {
            return fallbackWithReason(
                    modelInput,
                    safeDetailLevel,
                    "HTTP " + ex.getRawStatusCode() + "，响应内容：" + LlmFallbackSupport.preview(ex.getResponseBodyAsString()),
                    ex
            );
        } catch (ResourceAccessException ex) {
            return fallbackWithReason(
                    modelInput,
                    safeDetailLevel,
                    "网络访问异常：" + LlmFallbackSupport.describeException(ex),
                    ex
            );
        } catch (Exception ex) {
            return fallbackWithReason(
                    modelInput,
                    safeDetailLevel,
                    "调用异常：" + LlmFallbackSupport.describeException(ex),
                    ex
            );
        }
    }

    private LlmGenerationResult fallbackWithReason(String modelInput, CommentDetailLevel detailLevel, String reason) {
        log.warn("调用候选大模型 {} 失败，已回退到占位实现。原因：{}", providerLabel(), reason);
        return buildFallbackResult(modelInput, detailLevel, reason);
    }

    private LlmGenerationResult fallbackWithReason(String modelInput, CommentDetailLevel detailLevel, String reason, Exception ex) {
        log.warn("调用候选大模型 {} 失败，已回退到占位实现。原因：{}", providerLabel(), reason, ex);
        return buildFallbackResult(modelInput, detailLevel, reason);
    }

    private LlmGenerationResult buildFallbackResult(String modelInput, CommentDetailLevel detailLevel, String reason) {
        LlmGenerationResult fallbackResult = fallbackClient.generateCommentResult(modelInput, detailLevel);
        return LlmGenerationResult.fallback(
                fallbackResult.generatedComment(),
                providerLabel(),
                fallbackResult.actualProvider(),
                reason
        );
    }

    private String providerLabel() {
        return "DeepSeek(" + safeModelName() + ")";
    }

    private String safeModelName() {
        return model == null || model.isBlank() ? "未配置模型" : model.trim();
    }
}
