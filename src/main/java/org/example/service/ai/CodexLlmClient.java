package org.example.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.CommentDetailLevel;
import org.example.service.CommentPromptTemplateService;
import org.example.service.LlmClient;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodexLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(CodexLlmClient.class);

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

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 codex.api-key");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 codex.base-url");
        }
        if (model == null || model.isBlank()) {
            return fallbackWithReason(modelInput, safeDetailLevel, "未配置 codex.model");
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
                return fallbackWithReason(modelInput, safeDetailLevel, "接口响应体为空");
            }

            String outputText = extractOutputText(body);
            if (outputText != null && !outputText.isBlank()) {
                return outputText;
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

    private String fallbackWithReason(String modelInput, CommentDetailLevel detailLevel, String reason) {
        log.warn("调用候选大模型 {} 失败，已回退到占位实现。原因：{}", providerLabel(), reason);
        return fallbackClient.generateComment(modelInput, detailLevel);
    }

    private String fallbackWithReason(String modelInput, CommentDetailLevel detailLevel, String reason, Exception ex) {
        log.warn("调用候选大模型 {} 失败，已回退到占位实现。原因：{}", providerLabel(), reason, ex);
        return fallbackClient.generateComment(modelInput, detailLevel);
    }

    private String providerLabel() {
        return "Codex(" + safeModelName() + ")";
    }

    private String safeModelName() {
        return model == null || model.isBlank() ? "未配置模型" : model.trim();
    }
}
