package com.ruijie.listenevent.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson.JSONObject;
import com.ruijie.listenevent.service.AiAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class WebhooksController {
    @Autowired
    AiAgentService agentService;


    @PostMapping("/test")
    public String test(@RequestBody JSONObject req) throws GraphRunnerException {
        agentService.createAgent();
        String msg = agentService.getAnswer(req.getString("msg"));
        return msg;
    }

}
