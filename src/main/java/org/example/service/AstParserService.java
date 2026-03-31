package org.example.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.Providers;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import org.example.model.AstNode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AstParserService {
    private static final int MAX_DEPTH = 8;
    private static final int MAX_SNIPPET_LEN = 120;

    private final JavaParser javaParser = new JavaParser();

    public AstNode parseToAst(String code) {
        String safeCode = code == null ? "" : code.trim();
        if (safeCode.isEmpty()) {
            return new AstNode("EmptyCode", "", Collections.emptyList());
        }

        AstNode parsed = parseCompilationUnit(safeCode);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseBodyDeclaration(safeCode);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseStatement(safeCode);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseExpression(safeCode);
        if (parsed != null) {
            return parsed;
        }

        return new AstNode("RawCode", shorten(safeCode), Collections.emptyList());
    }

    private AstNode parseCompilationUnit(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    private AstNode parseBodyDeclaration(String code) {
        ParseResult<BodyDeclaration<?>> result = javaParser.parse(ParseStart.CLASS_BODY, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    private AstNode parseStatement(String code) {
        ParseResult<Statement> result = javaParser.parse(ParseStart.STATEMENT, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    private AstNode parseExpression(String code) {
        ParseResult<Expression> result = javaParser.parse(ParseStart.EXPRESSION, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    private AstNode toAstNode(Node node, int depth) {
        if (depth >= MAX_DEPTH) {
            return new AstNode(node.getClass().getSimpleName(), shorten(node.toString()), Collections.emptyList());
        }

        List<AstNode> children = node.getChildNodes().stream()
                .map(child -> toAstNode(child, depth + 1))
                .toList();

        return new AstNode(node.getClass().getSimpleName(), shorten(node.toString()), children);
    }

    private String shorten(String snippet) {
        if (snippet == null) {
            return "";
        }
        String normalized = snippet.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_SNIPPET_LEN) {
            return normalized;
        }
        return normalized.substring(0, MAX_SNIPPET_LEN) + "...";
    }
}
