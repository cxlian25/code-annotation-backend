package org.example.service;

public record LlmGenerationResult(
        String generatedComment,
        String requestedProvider,
        String actualProvider,
        boolean fallbackUsed,
        String fallbackReason
) {
    public LlmGenerationResult {
        generatedComment = generatedComment == null ? "" : generatedComment;
        requestedProvider = normalize(requestedProvider);
        actualProvider = normalize(actualProvider);
        fallbackReason = normalize(fallbackReason);
    }

    public static LlmGenerationResult success(String generatedComment, String provider) {
        String normalizedProvider = normalize(provider);
        return new LlmGenerationResult(generatedComment, normalizedProvider, normalizedProvider, false, "");
    }

    public static LlmGenerationResult fallback(
            String generatedComment,
            String requestedProvider,
            String actualProvider,
            String fallbackReason
    ) {
        return new LlmGenerationResult(generatedComment, requestedProvider, actualProvider, true, fallbackReason);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
