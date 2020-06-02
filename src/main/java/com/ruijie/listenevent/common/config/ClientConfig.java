package com.ruijie.listenevent.common.config;


import com.ruijie.listenevent.common.GerritClient;
import com.ruijie.listenevent.message.qywx.QywxPushMsgClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2019/11/27 16:02       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
@Configuration
public class ClientConfig {


    @Bean
    public QywxPushMsgClient getQywxPushMsgClient(){
        return new QywxPushMsgClient("https://qyapi.weixin.qq.com/",1000124,"wx8226b85848470887","AmYACf77mCLBHcpIXoWpwXbdXgBVWG40RhL7ilbUgac");
    }

    @Bean
    public GerritClient getGerritClient(){
        return new GerritClient();
    }



}
