package org.example.dto;

public class GenerateAnnotationResponse {
    private String modelInput;
    private String generatedComment;

    public GenerateAnnotationResponse() {
    }

    public GenerateAnnotationResponse(String modelInput, String generatedComment) {
        this.modelInput = modelInput;
        this.generatedComment = generatedComment;
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
}
