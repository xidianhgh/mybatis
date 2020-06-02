package com.ruijie.listenevent.message.qywx.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>Title:      </p>
 * <p>Description: 企业微信消息父类
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 11:09       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Data
@Accessors(chain = true)
public class QywxMessageRequest {
    private String touser;
    private String toparty;
    private String totag;
    private int agentid =1000124;
    private String msgtype;


    @JsonProperty("enable_id_trans")
    private int enableIdTrans = 0;
    @JsonProperty("enable_duplicate_check")
    private int enableDuplicateCheck =0;

}
