package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruijie.listenevent.service.PlanExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Plan-and-Execute + ReAct 混合模式控制器
 *
 * 混合模式流程：
 * 1. 规划（Plan）：LLM 将用户问题分解为多个子任务
 * 2. 执行（Execute + ReAct）：逐步执行每个子任务，每步通过 ReAct 循环（思考→行动→观察）调用工具
 * 3. 综合（Synthesize）：汇总所有步骤结果，生成最终答案
 */
@RestController
public class ReactController {

    @Autowired
    private PlanExecuteService planExecuteService;

    /**
     * Plan-and-Execute + ReAct 混合模式接口
     * POST http://localhost:9999/ai/plan-execute
     * Body: {"msg": "用户问题"}
     *
     * 执行流程：
     * 1. 规划阶段：调用 Ollama 模型将用户问题分解为多个可执行子任务
     * 2. 执行阶段：逐步执行每个子任务，每步通过 ChatClient 进行 ReAct 循环，
     *    可调用已注册的工具（知识库搜索、天气查询等），后续步骤能感知前面步骤的执行结果
     * 3. 综合阶段：调用 Ollama 模型汇总所有步骤结果，生成最终综合答案
     */
    @PostMapping("/ai/plan-execute")
    public JSONObject planExecute(@RequestBody JSONObject req) {
        String msg = req.getString("msg");

        JSONObject result = new JSONObject();
        try {
            String answer = planExecuteService.execute(msg);
            result.put("success", true);
            result.put("answer", answer);
            result.put("question", msg);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行失败: " + e.getMessage());
        }
        return result;
    }
}
