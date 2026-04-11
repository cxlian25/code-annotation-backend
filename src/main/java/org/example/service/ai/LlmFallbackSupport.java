package org.example.service.ai;

final class LlmFallbackSupport {
    private static final int MAX_PREVIEW_LENGTH = 300;

    private LlmFallbackSupport() {
    }

    static String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_PREVIEW_LENGTH) + "...";
    }

    static String describeException(Throwable throwable) {
        if (throwable == null) {
            return "未知异常";
        }

        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String simpleName = root.getClass().getSimpleName();
        String message = preview(root.getMessage());
        if (message.isBlank()) {
            return simpleName;
        }
        return simpleName + " - " + message;
    }
}
