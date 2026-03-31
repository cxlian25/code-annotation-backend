package org.example.model;

import java.util.List;

public record AstNode(String nodeType, String snippet, List<AstNode> children) {
}
