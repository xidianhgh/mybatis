package com.ruijie.listenevent.message.qywx.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/3/2 11:01       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Data
public class AccessTokenResponse {
    private int errcode;
    private String errmsg;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private int expiresIn;

}
