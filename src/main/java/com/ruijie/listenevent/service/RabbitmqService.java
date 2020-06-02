package com.ruijie.listenevent.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@ConditionalOnProperty(value = "listenEvent.listener.gerrit.power")
public class RabbitmqService {


    @RabbitListener(queues = "${listenEvent.listener.gerrit.queue}")
    public void receiveMessage(byte[] text) throws Exception {    // 进行消息接收处理
        String msg = new String(text);
        JSONObject msgJson = JSON.parseObject(msg);


    }


}
