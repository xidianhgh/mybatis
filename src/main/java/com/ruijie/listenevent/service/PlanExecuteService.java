package com.ruijie.listenevent.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Plan-and-Execute + ReAct 混合模式服务
 *
 * 执行流程：
 * 1. 规划阶段（Planner）：调用 Ollama 模型将用户问题分解为多个可执行子任务
 * 2. 执行阶段（Executor + ReAct）：逐步执行每个子任务，
 *    每步通过 ChatClient 进行「思考→行动→观察」循环，可调用已注册的工具（知识库搜索、天气查询等），
 *    后续步骤能感知前面步骤的执行结果，实现上下文传递
 * 3. 综合阶段（Synthesizer）：汇总所有步骤结果，调用 Ollama 模型生成最终综合答案
 *
 * 全程使用 Ollama 模型（application.yml 中配置的 qwen3:8b），不使用 DashScope
 */
@Service
public class PlanExecuteService {

    private static final Logger log = LoggerFactory.getLogger(PlanExecuteService.class);

    /** 全程使用 Ollama 模型（规划 + 综合） */
    private final ChatModel ollamaChatModel;

    /** 步骤执行使用 ChatClient（已注册所有 MCP 工具，模型可自主决定调用） */
    private final ChatClient chatClient;

    /** 短期记忆，用于步骤执行时隔离对话上下文 */
    private final MessageWindowChatMemory chatMemory;

    /** 规划阶段最大子任务数（防止 LLM 生成过多步骤） */
    private static final int MAX_PLAN_STEPS = 5;

    /** 执行阶段最大步数（超出时提前终止，进入综合阶段） */
    private static final int MAX_EXECUTE_STEPS = 5;

    public PlanExecuteService(
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            ChatClient chatClient,
            MessageWindowChatMemory chatMemory) {
        this.ollamaChatModel = ollamaChatModel;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * Plan-and-Execute + ReAct 主流程
     *
     * @param userQuery 用户原始问题
     * @return 最终综合答案
     */
    public String execute(String userQuery) {
        log.info("====== Plan-and-Execute 开始 | 问题: {} ======", userQuery);

        // ====== 第一阶段：规划 ======
        List<Step> plan = plan(userQuery);
        log.info("规划完成，共 {} 个子任务", plan.size());
        for (Step step : plan) {
            log.info("  [{}] {} (类型: {})", step.getStepId(), step.getContent(), step.getType());
        }

        // ====== 第二阶段：逐步执行（ReAct） ======
        List<StepResult> results = executeSteps(plan, userQuery);

        // ====== 第三阶段：综合 ======
        String finalAnswer = synthesize(userQuery, results);
        log.info("====== Plan-and-Execute 完成 ======");

        return finalAnswer;
    }

    // ==================== 第一阶段：规划 ====================

    /**
     * 规划阶段：调用 Ollama 模型将用户问题分解为可执行子任务列表
     */
    private List<Step> plan(String userQuery) {
        String planningPrompt = """
                你是一个任务规划专家。请将以下用户问题分解为具体的、可执行的子任务步骤。

                用户问题：%s

                要求：
                1. 每个步骤应该是一个具体、可执行的子任务
                2. 步骤之间可以有依赖关系，后续步骤可以利用前面步骤的结果
                3. 步骤数量控制在 5 步以内，只保留关键步骤
                4. 每个步骤标记类型：
                   - "knowledge"：需要从知识库检索信息
                   - "weather"：需要查询天气
                   - "reasoning"：需要逻辑推理或综合分析

                请严格按照以下 JSON 格式返回，不要包含任何其他内容：
                ```json
                [
                  {"stepId": "1", "content": "子任务描述", "type": "knowledge"},
                  {"stepId": "2", "content": "子任务描述", "type": "reasoning"}
                ]
                ```
                """;

        String response = ollamaChatModel.call(new Prompt(planningPrompt.formatted(userQuery)))
                .getResult().getOutput().getText();

        return parsePlan(response);
    }

    /**
     * 解析 LLM 返回的规划 JSON，提取步骤列表
     * 兼容 markdown 代码块包裹的情况，解析失败时兜底为单步执行
     */
    private List<Step> parsePlan(String planJson) {
        List<Step> steps = new ArrayList<>();
        try {
            String jsonStr = planJson;
            int start = jsonStr.indexOf('[');
            int end = jsonStr.lastIndexOf(']');
            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }

            JSONArray array = JSON.parseArray(jsonStr);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                steps.add(new Step(
                        obj.getString("stepId"),
                        obj.getString("content"),
                        obj.getString("type")
                ));
            }
        } catch (Exception e) {
            log.warn("规划结果解析失败，将原始问题作为单步执行: {}", e.getMessage());
            steps.add(new Step("1", "直接回答用户问题", "reasoning"));
        }

        if (steps.isEmpty()) {
            steps.add(new Step("1", "直接回答用户问题", "reasoning"));
        }
        // 代码层面兜底：即使 LLM 不遵守步数约束，也强制截断
        if (steps.size() > MAX_PLAN_STEPS) {
            log.warn("规划步骤数({})超过上限({})，截断为前 {} 步", steps.size(), MAX_PLAN_STEPS, MAX_PLAN_STEPS);
            steps = new ArrayList<>(steps.subList(0, MAX_PLAN_STEPS));
        }
        return steps;
    }

    // ==================== 第二阶段：逐步执行（ReAct） ====================

    /**
     * 执行阶段：逐步执行每个子任务
     *
     * 每一步通过 ChatClient 进行 ReAct 循环：
     * - 思考（Thought）：分析当前子任务需要什么信息
     * - 行动（Action）：调用可用工具（知识库搜索、天气查询等）获取信息
     * - 观察（Observation）：获取工具返回的结果
     *
     * 使用独立的 conversationId 隔离计划执行的对话记忆，避免污染主会话的 Redis 短期记忆
     * 后续步骤会携带前面步骤的执行结果作为上下文，实现步骤间的信息传递
     */
    private List<StepResult> executeSteps(List<Step> plan, String originalQuery) {
        List<StepResult> results = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        // 为本次计划执行生成独立的会话 ID，避免污染主对话的短期记忆
        String planConversationId = "plan-exec-" + UUID.randomUUID();
        MessageChatMemoryAdvisor planMemoryAdvisor = MessageChatMemoryAdvisor
                .builder(chatMemory)
                .conversationId(planConversationId)
                .build();

        int executeLimit = Math.min(plan.size(), MAX_EXECUTE_STEPS);
        boolean truncated = plan.size() > MAX_EXECUTE_STEPS;

        for (int i = 0; i < executeLimit; i++) {
            Step step = plan.get(i);
            log.info("执行步骤 [{}/{}] {}: {}", i + 1, executeLimit, step.getStepId(), step.getContent());

            try {
                // 构建带上下文的步骤问题：当前子任务 + 前面步骤的执行结果
                StringBuilder stepQuestion = new StringBuilder();
                stepQuestion.append("请完成以下子任务：").append(step.getContent());

                if (i > 0 && !contextBuilder.isEmpty()) {
                    stepQuestion.append("\n\n【之前步骤的执行结果（供参考）】\n")
                            .append(contextBuilder);
                }

                stepQuestion.append("\n\n原始问题：").append(originalQuery);

                // 通过 ChatClient 执行（ReAct 循环：模型自主决定是否需要调用工具）
                // chatClient 已注册所有 MCP 工具，模型在推理时可自动调用
                String result = chatClient.prompt()
                        .advisors(planMemoryAdvisor)
                        .user(stepQuestion.toString())
                        .call()
                        .content();

                log.info("步骤 [{}] 执行完成", step.getStepId());
                log.debug("步骤 [{}] 结果: {}", step.getStepId(), result);

                results.add(new StepResult(step.getStepId(), step.getContent(), result, true));

                // 累积执行上下文，供后续步骤参考
                contextBuilder.append("步骤 ")
                        .append(step.getStepId())
                        .append("（")
                        .append(step.getContent())
                        .append("）结果：")
                        .append(result)
                        .append("\n\n");

            } catch (Exception e) {
                log.error("步骤 [{}] 执行失败: {}", step.getStepId(), e.getMessage(), e);
                results.add(new StepResult(step.getStepId(), step.getContent(),
                        "执行失败: " + e.getMessage(), false));
            }
        }

        if (truncated) {
            log.warn("已达最大执行步数限制({})，提前终止，剩余 {} 步未执行",
                    MAX_EXECUTE_STEPS, plan.size() - MAX_EXECUTE_STEPS);
        }

        // 执行完成后清理本次计划的对话记忆，不残留 Redis
        try {
            chatMemory.clear(planConversationId);
        } catch (Exception e) {
            log.debug("清理计划执行记忆失败（不影响结果）: {}", e.getMessage());
        }

        return results;
    }

    // ==================== 第三阶段：综合 ====================

    /**
     * 综合阶段：调用 Ollama 模型汇总所有步骤结果，生成最终答案
     */
    private String synthesize(String userQuery, List<StepResult> results) {
        StringBuilder contextBuilder = new StringBuilder();
        for (StepResult result : results) {
            contextBuilder.append("【步骤 ").append(result.stepId).append("】\n")
                    .append("子任务：").append(result.content).append("\n")
                    .append("执行结果：").append(result.result).append("\n\n");
        }

        String synthesisPrompt = """
                你是一个综合总结专家。以下是针对用户问题的分步执行结果。
                请基于这些中间结果，生成最终的综合答案。

                用户原始问题：%s

                各步骤执行结果：
                %s

                要求：
                1. 综合所有步骤的结果，给出完整、准确的最终答案
                2. 如果某个步骤的结果为"无法回答"或"执行失败"，请在最终答案中如实说明
                3. 答案要条理清晰，逻辑连贯
                4. 不要编造步骤中没有的信息
                """.formatted(userQuery, contextBuilder);

        String finalAnswer = ollamaChatModel.call(new Prompt(synthesisPrompt))
                .getResult().getOutput().getText();

        log.info("综合答案生成完成");
        return finalAnswer;
    }

    // ==================== 内部数据结构 ====================

    /** 计划步骤 */
    public static class Step {
        private String stepId;
        private String content;
        private String type;

        public Step() {}

        public Step(String stepId, String content, String type) {
            this.stepId = stepId;
            this.content = content;
            this.type = type;
        }

        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /** 步骤执行结果 */
    public record StepResult(String stepId, String content, String result, boolean success) {}
}
