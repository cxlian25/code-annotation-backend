package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.CommentDetailLevel;
import org.example.dto.GenerateAnnotationRequest;
import org.example.dto.GenerateAnnotationResponse;
import org.example.model.AstNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationGenerationServiceTest {

    @Mock
    private AstParserService astParserService;

    @Mock
    private LlmClient llmClient;

    private AnnotationGenerationService annotationGenerationService;

    @BeforeEach
    void setUp() {
        annotationGenerationService = new AnnotationGenerationService(astParserService, llmClient, new ObjectMapper());
    }

    @Test
    void generateShouldExposeFallbackMetadata() {
        GenerateAnnotationRequest request = new GenerateAnnotationRequest();
        request.setTargetCode("public void test() {}");
        request.setContext("demo");
        request.setCommentDetailLevel(CommentDetailLevel.DETAILED);

        when(astParserService.parseToAst(anyString()))
                .thenReturn(new AstNode("MethodDeclaration", "public void test() {}", List.of()));
        when(llmClient.generateCommentResult(anyString(), eq(CommentDetailLevel.DETAILED)))
                .thenReturn(LlmGenerationResult.fallback(
                        "[LLM placeholder][detailed] Method test handles the target business logic. Replace this with a real model call.",
                        "DeepSeek(deepseek-chat)",
                        "placeholder",
                        "HTTP 401 - invalid api key"
                ));

        GenerateAnnotationResponse response = annotationGenerationService.generate(request);

        assertEquals("DeepSeek(deepseek-chat)", response.getLlmRequestedProvider());
        assertEquals("placeholder", response.getLlmActualProvider());
        assertTrue(response.isLlmFallbackUsed());
        assertEquals("HTTP 401 - invalid api key", response.getLlmFallbackReason());
        assertTrue(response.getGeneratedComment().startsWith("[LLM placeholder]"));
    }
}
