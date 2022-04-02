package com.ruijie.listenevent.dao;

import com.ruijie.listenevent.entity.GroupMemberEntity;
import org.apache.ibatis.annotations.Param;

public interface GroupMemberMapper {

    void insert(GroupMemberEntity groupMemberEntity);

   void createNewTable(@Param("tableName")String tableName);


}
