package com.ruijie.listenevent.controller;

import com.ruijie.listenevent.entity.GroupMemberEntity;
import com.ruijie.listenevent.service.GroupMemberService;
import com.ruijie.listenevent.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class GroupMemberController {
    @Autowired
    GroupMemberService groupMemberService;

    @PostMapping("/groups")
    public void test(@RequestBody GroupMemberEntity req) {

        String currentDate = DateUtils.getCurrentTime();
        groupMemberService.saveGroupMemberEvent(req.getType(), req.getMember(), req.getExecutor(),
                currentDate, req.getGroupName());
    }

    @PostMapping("/transaction")
    @Transactional(rollbackFor = Exception.class)
    public void testTransaction(@RequestBody GroupMemberEntity req) {

        String currentDate = DateUtils.getCurrentTime();
        groupMemberService.testTransaction(req.getType(), req.getMember(), req.getExecutor(),
                currentDate, req.getGroupName());
    }

}
