package com.ruijie.listenevent.service;

import com.ruijie.listenevent.dao.GroupMemberMapper;
import com.ruijie.listenevent.entity.GroupMemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberService {
    @Autowired
    GroupMemberMapper groupMemberMapper;

    public void saveGroupMemberEvent(String type, String member, String executor, String date,String group) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
//        groupMemberEntity.setMember(member);
//        groupMemberEntity.setExecutor(executor);
//        groupMemberEntity.setDate(date);
//        groupMemberEntity.setGroupName(group);
        groupMemberMapper.insert(groupMemberEntity);
    }
    public void saveGroupMemberEvent(String type) {
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
        groupMemberMapper.insert(groupMemberEntity);
    }

}
