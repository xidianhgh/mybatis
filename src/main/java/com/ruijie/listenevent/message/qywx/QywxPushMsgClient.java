package com.ruijie.listenevent.message.qywx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.ruijie.listenevent.common.HttpClient;
import com.ruijie.listenevent.message.qywx.response.AccessTokenResponse;
import com.ruijie.listenevent.message.qywx.response.PushMessageResponse;
import com.ruijie.listenevent.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;

/**
 * <p>Title:      </p>
 * <p>Description: 企业微信推送消息客户端,实现推送的领域模型
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/2/28 13:54       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Slf4j
public class QywxPushMsgClient {
    private String uri;
    private int AgentId;
    private String corpid;
    private String corpsecret;
    private String access_token = "HLKyDBf2f0lgPkTVGJjSDW9a6hEF4oWSIx6jmiAg0kZUUD-CQvwjIoQie6GSPLfdU1OH_77wzCjAZSfRHuptTkYsG25t7uVmeoySsC_reWY6kNWXFX72oDRmCqqF6SR_fmGHHqAt2FXw886wLA2s4BCsuSJpVYgHkn4QSfEWQtOv-GNjFE_5X-feme29989R_nkLARBporecohh9lCK7PQ";
    private HttpClient httpClient;

    @Autowired
    ObjectMapper objectMapper;

    public QywxPushMsgClient(String uri,int AgentId,String corpid,String corpsecret){
        this.uri = uri;
        this.AgentId = AgentId;
        this.corpid  = corpid;
        this.corpsecret = corpsecret;
        this.httpClient = new HttpClient(new OkHttpClient(),true);
    }

    public void getAccessToken(){
        HashMap<String,String> maps = Maps.newHashMap();
        String url = WxServeConstant.TOKEN_URL+"?corpid="+this.corpid+"&corpsecret="+this.corpsecret;
        String path = UrlUtils.join(uri.toString(),url);
        try {
            String result =  httpClient.get(path,maps);
            AccessTokenResponse accessTokenResponse = objectMapper.readValue(result,AccessTokenResponse.class);
            this.access_token = accessTokenResponse.getAccessToken();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("获取access_token异常:{}",e.getMessage());
        }
    }


    public PushMessageResponse send(String json){
        PushMessageResponse pushMessageResponse = new PushMessageResponse();
        HashMap<String,String> maps = Maps.newHashMap();
        String path = UrlUtils.join(uri,WxServeConstant.PUSH_MESSAGE_URL+this.access_token);
        String reponse = null;
        try {
            reponse = httpClient.post(path,json,maps);
            pushMessageResponse = objectMapper.readValue(reponse,PushMessageResponse.class);
            /**token过期，重新申请，重发*/
            if(pushMessageResponse.getErrcode() == 42001 || pushMessageResponse.getErrcode() == 40014){
                log.info("access_token过期");
                getAccessToken();
                path = UrlUtils.join(uri,WxServeConstant.PUSH_MESSAGE_URL+this.access_token);
                reponse = httpClient.post(path,json,maps);
                pushMessageResponse = objectMapper.readValue(reponse,PushMessageResponse.class);
            }


            if(pushMessageResponse.getErrcode() != 0){
                log.error("发送企业微信信息失败，原因{}",pushMessageResponse.getErrmsg());
            }
            return pushMessageResponse;
        } catch (IOException e) {
            pushMessageResponse.setErrcode(9999).setErrmsg("IOException send");
           log.error("企业微信服务器连接错误:{}-{}",e.getMessage(),e.getCause());
        }
        return pushMessageResponse;
    }
}
