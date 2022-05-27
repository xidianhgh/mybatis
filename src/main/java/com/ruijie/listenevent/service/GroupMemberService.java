package com.ruijie.listenevent.service;

import com.ruijie.listenevent.dao.GroupMemberMapper;
import com.ruijie.listenevent.entity.GroupMemberEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupMemberService {
    @Autowired
    GroupMemberMapper groupMemberMapper;

    @Transactional
    public void saveGroupMemberEvent(GroupMemberEntity req) {
        createTable();
        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        BeanUtils.copyProperties(req,groupMemberEntity);
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
        groupMemberEntity.setExecutor("测试事务");
        groupMemberEntity.setDate(date);
        groupMemberEntity.setGroupId(1);
        groupMemberMapper.insert(groupMemberEntity);
    }

    @Transactional
    public void testTransaction(String type, String member, String executor, String date, int groupId){
//        saveData(type, member, executor, date, group);
//        saveGroupMemberEventError(type, member, executor, date, group);

        GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
        groupMemberEntity.setType(type);
        groupMemberEntity.setMember(member);
        groupMemberEntity.setExecutor(executor);
        groupMemberEntity.setDate(date);
        groupMemberEntity.setGroupId(1);
        groupMemberMapper.insert(groupMemberEntity);

        GroupMemberEntity groupMemberEntityt = new GroupMemberEntity();
        groupMemberEntityt.setType(type);
        groupMemberEntityt.setExecutor("测试事务");
        groupMemberEntityt.setDate(date);
        groupMemberEntityt.setGroupId(1);
        groupMemberMapper.insert(groupMemberEntityt);
    }

}
