package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhookss")
public class WebhooksController {

    @PostMapping("/groups")
    public void test(@RequestBody JSONObject req) {

        System.out.println(req.toJSONString());
    }

}
