package com.ruijie.listenevent.message.qywx.response;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 11:04       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Data
@Accessors(chain = true)
public class PushMessageResponse {
    private int errcode;
    private String errmsg;

    /*userid1|userid2**/
    private String invaliduser;

    private String invalidparty;

    private String invalidtag;


}
