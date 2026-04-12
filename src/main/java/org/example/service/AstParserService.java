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

    /**
     * 该方法将输入的代码字符串解析为AST节点，依次尝试编译单元、类成员、语句和表达式解析，若均失败则返回原始代码节点。
     */
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

    /**
     * 解析给定的 Java 代码字符串并返回其对应的 AST 根节点，若解析失败则返回 null。
     */
    private AstNode parseCompilationUnit(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    /**
     * 该方法解析给定的Java代码字符串，若成功则返回对应的AST节点，否则返回null。
     */
    private AstNode parseBodyDeclaration(String code) {
        ParseResult<BodyDeclaration<?>> result = javaParser.parse(ParseStart.CLASS_BODY, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    /**
     * 解析给定的Java代码字符串为语句节点，并在解析成功时转换为AstNode对象，否则返回null。
     */
    private AstNode parseStatement(String code) {
        ParseResult<Statement> result = javaParser.parse(ParseStart.STATEMENT, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    /**
     * 解析给定的Java代码字符串为表达式AST节点，解析失败时返回null。
     */
    private AstNode parseExpression(String code) {
        ParseResult<Expression> result = javaParser.parse(ParseStart.EXPRESSION, Providers.provider(code));
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return toAstNode(result.getResult().get(), 0);
        }
        return null;
    }

    /**
     * 将给定的节点递归转换为AstNode对象，当深度达到MAX_DEPTH时停止递归并返回一个无子节点的AstNode。
     */
    private AstNode toAstNode(Node node, int depth) {
        if (depth >= MAX_DEPTH) {
            return new AstNode(node.getClass().getSimpleName(), shorten(node.toString()), Collections.emptyList());
        }

        List<AstNode> children = node.getChildNodes().stream()
                .map(child -> toAstNode(child, depth + 1))
                .toList();

        return new AstNode(node.getClass().getSimpleName(), shorten(node.toString()), children);
    }

    /**
     * 将输入字符串规范化为单个空格并修剪后，若长度超过最大限制则截断并添加省略号。
     */
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
