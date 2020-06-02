package com.ruijie.listenevent.message.qywx;


import com.github.dozermapper.core.Mapper;
import com.ruijie.listenevent.dto.EventModel;
import com.ruijie.listenevent.message.MessageSendService;
import com.ruijie.listenevent.message.qywx.response.PushMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


/**
 * <p>Title:      </p>
 * <p>Description: 企业微信发送消息服务
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 9:50       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Slf4j
@Service("QywxMessageSendService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class QywxMessageSendService implements MessageSendService {

    @Autowired
    QywxPushMsgClient qywxPushMsgClient;


    @Override
    public void send(EventModel eventModel, String json) {

        PushMessageResponse response = qywxPushMsgClient.send(json);

    }
}
