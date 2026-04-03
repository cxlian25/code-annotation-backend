package org.example.service;

import org.example.dto.CommentDetailLevel;
import org.example.service.ai.CodexLlmClient;
import org.example.service.ai.DeepSeekLlmClient;
import org.example.service.ai.PlaceholderLlmClient;
import org.example.service.ai.QwenLlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Primary
public class RoutingLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(RoutingLlmClient.class);

    private final DeepSeekLlmClient deepSeekLlmClient;
    private final CodexLlmClient codexLlmClient;
    private final QwenLlmClient qwenLlmClient;
    private final PlaceholderLlmClient placeholderLlmClient;

    @Value("${llm.provider:deepseek}")
    private String provider;

    public RoutingLlmClient(DeepSeekLlmClient deepSeekLlmClient,
                            CodexLlmClient codexLlmClient,
                            QwenLlmClient qwenLlmClient,
                            PlaceholderLlmClient placeholderLlmClient) {
        this.deepSeekLlmClient = deepSeekLlmClient;
        this.codexLlmClient = codexLlmClient;
        this.qwenLlmClient = qwenLlmClient;
        this.placeholderLlmClient = placeholderLlmClient;
    }

    @Override
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        String selected = provider == null ? "deepseek" : provider.trim().toLowerCase(Locale.ROOT);
        return switch (selected) {
            case "codex", "openai" -> codexLlmClient.generateComment(modelInput, detailLevel);
            case "qwen", "dashscope" -> qwenLlmClient.generateComment(modelInput, detailLevel);
            case "placeholder" -> placeholderLlmClient.generateComment(modelInput, detailLevel);
            case "deepseek" -> deepSeekLlmClient.generateComment(modelInput, detailLevel);
            default -> {
                log.warn("Unknown llm.provider='{}', fallback to deepseek", provider);
                yield deepSeekLlmClient.generateComment(modelInput, detailLevel);
            }
        };
    }
}
