package org.example.service;

import org.example.dto.CommentDetailLevel;
import org.springframework.stereotype.Service;

@Service
public class CommentPromptTemplateService {

    public PromptPair build(String modelInput, CommentDetailLevel detailLevel) {
        CommentDetailLevel safeLevel = detailLevel == null ? CommentDetailLevel.CONCISE : detailLevel;
        return new PromptPair(buildSystemPrompt(safeLevel), buildUserPrompt(modelInput, safeLevel));
    }

    private String buildSystemPrompt(CommentDetailLevel detailLevel) {
        String role = """
                你是一名面向企业后端代码的 Java 注释生成专家。
                你的目标是为目标方法生成一条准确的中文注释。
                优先保证事实正确，不追求花哨表达。
                不得编造输入中不存在的方法行为。
                """;

        String outputRules = """

                输出规则：
                1) 仅返回注释文本，不要 markdown、代码块或标签。
                2) 使用中文。
                3) 聚焦方法意图、关键逻辑。
                4) 避免机械复述语法。
                5) 不确定时使用保守表述。
                """;

        if (detailLevel == CommentDetailLevel.DETAILED) {
            return role + outputRules + """

                    详细模式：
                    - 2 到 4 句话。
                    - 覆盖设计意图和关键决策点。
                    - 仅在输入明确可见时提及重要输入输出或异常行为。
                    - 简单介绍目标代码具体逻辑。
                    - 不要忽略上下文信息。
                    """;
        }

        return role + outputRules + """

                简洁模式：
                - 1 句话。
                - 语言紧凑，聚焦核心作用。
                """;
    }

    private String buildUserPrompt(String modelInput, CommentDetailLevel detailLevel) {
        String task = """
                输入 JSON 包含：targetCode、context、ast、commentDetailLevel。
                现在请生成最终中文注释。

                检查清单：
                - 该方法的业务或技术目的是什么？
                - 它执行了哪些关键操作？
                - 是否存在可见约束、副作用或边界处理？
                """;

        String detailHint = detailLevel == CommentDetailLevel.DETAILED
                ? "\n模式提示：详细（2-4 句话）。\n"
                : "\n模式提示：简洁（1 句话）。\n";

        return task + detailHint + "\n输入 JSON：\n" + modelInput;
    }

    public record PromptPair(String systemPrompt, String userPrompt) {
    }
}