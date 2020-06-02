package com.ruijie.listenevent.message;


import com.ruijie.listenevent.dto.EventModel;

/**
 * <p>Title:      </p>
 * <p>Description: 发送消息服务
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 9:50       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
public interface MessageSendService {

    void send(EventModel eventModel, String json);
}
