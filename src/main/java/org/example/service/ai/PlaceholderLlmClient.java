package org.example.service.ai;

import org.example.dto.CommentDetailLevel;
import org.example.service.LlmClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlaceholderLlmClient implements LlmClient {
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?[\\w<>\\[\\]]+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");

    @Override
    public String generateComment(String modelInput, CommentDetailLevel detailLevel) {
        String mode = detailLevel == CommentDetailLevel.DETAILED ? "detailed" : "concise";
        String methodName = extractMethodName(modelInput);
        if (methodName != null) {
            return "[LLM placeholder][" + mode + "] Method " + methodName + " handles the target business logic. Replace this with a real model call.";
        }
        return "[LLM placeholder][" + mode + "] AST and context were received and processed. Replace this with a real model call.";
    }

    private String extractMethodName(String input) {
        Matcher matcher = METHOD_NAME_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}