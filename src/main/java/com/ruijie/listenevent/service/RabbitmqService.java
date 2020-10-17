package com.ruijie.listenevent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
//@ConditionalOnProperty(value = "listenEvent.listener.gerrit.power")
public class RabbitmqService {


//    @RabbitListener(queues = "${listenEvent.listener.gerrit.queue}")
//    public void receiveMessage(byte[] text) throws Exception {    // 进行消息接收处理
//        String msg = new String(text);
//        JSONObject msgJson = JSON.parseObject(msg);
//
//
//    }


}
