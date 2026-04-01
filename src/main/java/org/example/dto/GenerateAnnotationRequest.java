package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateAnnotationRequest {
    @NotBlank(message = "targetCode must not be blank")
    private String targetCode;

    private String context;

    private CommentDetailLevel commentDetailLevel = CommentDetailLevel.CONCISE;

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public CommentDetailLevel getCommentDetailLevel() {
        return commentDetailLevel;
    }

    public void setCommentDetailLevel(CommentDetailLevel commentDetailLevel) {
        this.commentDetailLevel = commentDetailLevel == null ? CommentDetailLevel.CONCISE : commentDetailLevel;
    }
}