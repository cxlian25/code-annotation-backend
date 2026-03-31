package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateAnnotationRequest {
    @NotBlank(message = "targetCode must not be blank")
    private String targetCode;

    private String context;

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
}
