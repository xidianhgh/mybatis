package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruijie.listenevent.service.GroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhookss")
public class WebhooksController {
    @Autowired
    GroupMemberService groupMemberService;

    @PostMapping("/groups")
    public void test(@RequestBody JSONObject req) {

        groupMemberService.saveGroupMemberEvent(req.getString("type"));
        System.out.println(req.toJSONString());
    }

}
