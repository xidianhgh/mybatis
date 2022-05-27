package com.ruijie.listenevent.dao;

import com.ruijie.listenevent.entity.GroupEntity;
import org.apache.ibatis.annotations.Param;

public interface GroupMapper {
    void insert(GroupEntity groupMemberEntity);

    void createNewTable(@Param("tableName")String tableName);
}
