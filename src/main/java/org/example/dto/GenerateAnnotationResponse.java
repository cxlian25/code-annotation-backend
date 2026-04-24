package org.example.dto;

public class GenerateAnnotationResponse {

    //输入信息
    private String modelInput;

    //注释
    private String generatedComment;

    private String llmRequestedProvider;

    private String llmActualProvider;

    private boolean llmFallbackUsed;

    private String llmFallbackReason;

    public GenerateAnnotationResponse() {
    }

    public GenerateAnnotationResponse(String modelInput, String generatedComment) {
        this(modelInput, generatedComment, "", "", false, "");
    }

    public GenerateAnnotationResponse(
            String modelInput,
            String generatedComment,
            String llmRequestedProvider,
            String llmActualProvider,
            boolean llmFallbackUsed,
            String llmFallbackReason
    ) {
        this.modelInput = modelInput;
        this.generatedComment = generatedComment;
        this.llmRequestedProvider = llmRequestedProvider;
        this.llmActualProvider = llmActualProvider;
        this.llmFallbackUsed = llmFallbackUsed;
        this.llmFallbackReason = llmFallbackReason;
    }

    public String getModelInput() {
        return modelInput;
    }

    public void setModelInput(String modelInput) {
        this.modelInput = modelInput;
    }

    public String getGeneratedComment() {
        return generatedComment;
    }

    public void setGeneratedComment(String generatedComment) {
        this.generatedComment = generatedComment;
    }

    public String getLlmRequestedProvider() {
        return llmRequestedProvider;
    }

    public void setLlmRequestedProvider(String llmRequestedProvider) {
        this.llmRequestedProvider = llmRequestedProvider;
    }

    public String getLlmActualProvider() {
        return llmActualProvider;
    }

    public void setLlmActualProvider(String llmActualProvider) {
        this.llmActualProvider = llmActualProvider;
    }

    public boolean isLlmFallbackUsed() {
        return llmFallbackUsed;
    }

    public void setLlmFallbackUsed(boolean llmFallbackUsed) {
        this.llmFallbackUsed = llmFallbackUsed;
    }

    public String getLlmFallbackReason() {
        return llmFallbackReason;
    }

    public void setLlmFallbackReason(String llmFallbackReason) {
        this.llmFallbackReason = llmFallbackReason;
    }
}
