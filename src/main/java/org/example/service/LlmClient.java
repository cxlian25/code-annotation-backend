package org.example.service;

import org.example.dto.CommentDetailLevel;

public interface LlmClient {
    LlmGenerationResult generateCommentResult(String modelInput, CommentDetailLevel detailLevel);

    default String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        return generateCommentResult(modelInput, detailLevel).generatedComment();
    }
}
