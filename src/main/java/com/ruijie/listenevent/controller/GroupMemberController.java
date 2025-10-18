package com.ruijie.listenevent.controller;

import com.ruijie.listenevent.entity.GroupMemberEntity;
import com.ruijie.listenevent.service.GroupMemberService;
import com.ruijie.listenevent.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/groupMember")
public class GroupMemberController {
    @Autowired
    GroupMemberService groupMemberService;

    @GetMapping("")
    public Map<String, Object> getInfo() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", groupMemberService.getMembers());
        return map;
    }

    @PostMapping("")
    public void test(@RequestBody GroupMemberEntity req) {

        String currentDate = DateUtils.getCurrentTime();
        req.setDate(currentDate);
        groupMemberService.saveGroupMemberEvent(req);
    }

    @PostMapping("/transaction")
    @Transactional(rollbackFor = Exception.class)
    public void testTransaction(@RequestBody GroupMemberEntity req) {

        String currentDate = DateUtils.getCurrentTime();
        groupMemberService.testTransaction(req.getType(), req.getMember(), req.getExecutor(),
                currentDate, req.getGroupId());
    }

}
