package org.example.service;

import org.example.dto.CommentDetailLevel;

public interface LlmClient {
    String generateComment(String modelInput, CommentDetailLevel detailLevel);
}