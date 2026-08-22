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

    /**
     * 异步启动 Plan-and-Execute 任务（可被停止）
     * POST http://localhost:9999/ai/plan-execute/start
     * Body: {"msg": "用户问题"}
     *
     * 返回 taskId 用于后续停止或查询状态
     */
    @PostMapping("/ai/plan-execute/start")
    public JSONObject planExecuteStart(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String taskId = planExecuteService.startTask(msg);

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("taskId", taskId);
        result.put("question", msg);
        result.put("message", "任务已启动，可通过 taskId 停止或查询状态");
        return result;
    }

    /**
     * 停止正在执行的 Plan-and-Execute 任务
     * POST http://localhost:9999/ai/plan-execute/stop
     * Body: {"taskId": "task-1"}
     *
     * 返回：
     * - allSteps：任务的所有步骤列表
     * - completedSteps：已执行完成的步骤列表
     * - stoppedAtStep：在哪个步骤停止
     * - completed：任务是否已完成
     * - cancelled：任务是否被取消
     */
    @PostMapping("/ai/plan-execute/stop")
    public JSONObject planExecuteStop(@RequestBody JSONObject req) {
        String taskId = req.getString("taskId");

        JSONObject result = new JSONObject();
        try {
            JSONObject status = planExecuteService.stopTask(taskId);
            if (status == null) {
                result.put("success", false);
                result.put("message", "任务不存在: " + taskId);
            } else {
                result.put("success", true);
                result.putAll(status);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "停止任务失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 查询 Plan-and-Execute 任务状态
     * GET http://localhost:9999/ai/plan-execute/status?taskId=task-1
     */
    @GetMapping("/ai/plan-execute/status")
    public JSONObject planExecuteStatus(@RequestParam String taskId) {
        JSONObject result = new JSONObject();
        try {
            JSONObject status = planExecuteService.getTaskStatus(taskId);
            if (status == null) {
                result.put("success", false);
                result.put("message", "任务不存在: " + taskId);
            } else {
                result.put("success", true);
                result.putAll(status);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询状态失败: " + e.getMessage());
        }
        return result;
    }
}
