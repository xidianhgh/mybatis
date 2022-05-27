package com.ruijie.listenevent.controller;

import com.ruijie.listenevent.entity.GroupEntity;
import com.ruijie.listenevent.service.GroupService;
import com.ruijie.listenevent.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/group")
public class GroupController {
    @Autowired
    GroupService groupService;

    @PostMapping("")
    public void test(@RequestBody GroupEntity req) {
        String currentDate = DateUtils.getCurrentTime();
        req.setDate(new Date());
        groupService.saveData(req);
    }

}
