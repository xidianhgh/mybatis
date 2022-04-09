package com.ruijie.listenevent.service;

import com.ruijie.listenevent.dao.GroupMemberMapper;
import com.ruijie.listenevent.entity.GroupMemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMemberService {
    @Autowired
    GroupMemberMapper groupMemberMapper;

    @Transactional
    public void saveGroupMemberEvent(String type, String member, String executor, String date, String group) {
        createTable();
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
        groupMemberEntity.setMember(member);
        groupMemberEntity.setExecutor(executor);
        groupMemberEntity.setDate(date);
        groupMemberEntity.setGroupName(group);
        groupMemberMapper.insert(groupMemberEntity);
    }

    public void createTable() {
        groupMemberMapper.createNewTable("group_member");
    }

    @Transactional
    public void saveGroupMemberEventError(String type, String member, String executor, String date, String group) {
        createTable();
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
//        groupMemberEntity.setMember(member);
        groupMemberEntity.setExecutor(executor);
        groupMemberEntity.setDate(date);
        groupMemberEntity.setGroupName("测试事务");
        groupMemberMapper.insert(groupMemberEntity);
    }

    @Transactional
    public void testTransaction(String type, String member, String executor, String date, String group){
//        saveGroupMemberEvent(type, member, executor, date, group);
//        saveGroupMemberEventError(type, member, executor, date, group);

        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
        groupMemberEntity.setMember(member);
        groupMemberEntity.setExecutor(executor);
        groupMemberEntity.setDate(date);
        groupMemberEntity.setGroupName(group);
        groupMemberMapper.insert(groupMemberEntity);

        GroupMemberEntity groupMemberEntityt = new GroupMemberEntity();
        groupMemberEntityt.setType(type);
        groupMemberEntityt.setExecutor(executor);
        groupMemberEntityt.setDate(date);
        groupMemberEntityt.setGroupName("测试事务");
        groupMemberMapper.insert(groupMemberEntityt);
    }

}
