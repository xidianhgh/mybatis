package com.ruijie.listenevent.service;

import com.ruijie.listenevent.dao.GroupMapper;
import com.ruijie.listenevent.entity.GroupEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
//@Transactional
public class GroupService {
    @Autowired
    GroupMapper groupMapper;

    @Transactional
    public void saveData(GroupEntity req) {
        createTable();
        GroupEntity groupEntity = new GroupEntity();
        BeanUtils.copyProperties(req, groupEntity);
        groupMapper.insert(groupEntity);
    }

    public void createTable() {
        groupMapper.createNewTable("group_info");
    }

    public void saveTransactional(GroupEntity req) {
        createTable();
        GroupEntity groupEntity = new GroupEntity();
        BeanUtils.copyProperties(req, groupEntity);
        groupMapper.insert(groupEntity);
        GroupEntity groupEntity1 = new GroupEntity();
        BeanUtils.copyProperties(req, groupEntity1);
        groupEntity1.setGroupName("xxx");
        groupMapper.insert(groupEntity1);
        GroupEntity groupEntity2=null;
        groupEntity2.setGroupName("a");
    }

}
