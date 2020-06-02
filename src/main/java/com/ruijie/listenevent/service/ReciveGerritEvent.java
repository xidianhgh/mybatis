//package com.ruijie.com.ruijie.listenevent.com.ruijie.listenevent.dao.service;
//
//import cn.com.ruijie.code.manage.async.EventModel;
//import cn.com.ruijie.code.manage.async.EventProducer;
//import cn.com.ruijie.code.manage.utils.DateUtil;
//import cn.com.ruijie.code.notification.domain.Message;
//import cn.com.ruijie.code.notification.message.MessageSendService;
//import cn.com.ruijie.code.notification.message.qywx.request.TextcardMessagRequest;
//import com.baomidou.mybatisplus.core.toolkit.StringUtils;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//
///**
// * <p>Title:      </p>
// * <p>Description:
// * <p>Copyright: Copyright (c) 2019     </p>
// * <p>Company: Ruijie Co., Ltd.          </p>
// * <p>Create Time: 2020/3/5 17:50       </p>
// *
// * @author lijianhua
// * <p>Update Time:                      </p>
// * <p>Updater:                          </p>
// * <p>Update Comments:                  </p>
// */
//@Slf4j
//@Service
//@ConditionalOnProperty(value = "CodeReviwManage.listener.gerrit.power")
//public class ReciveGerritEvent {
//
//    @Autowired
//    GerritEventFactoryMap gerritEventFactoryMap;
//    @Autowired
//    ObjectMapper objectMapper;
//    @Autowired
//    EventProducer eventProducer;
//    @Autowired
//    @Qualifier("QywxMessageSendService")
//    MessageSendService messageSendService;
//
//    @RabbitListener(queues = "${CodeReviwManage.listener.gerrit.queue}")
//    public void receiveMessage(byte[] text) throws IOException {    // 进行消息接收处理
//        JsonNode jsonNode = objectMapper.readTree(text);
//        IGerritEventFactory gerritEventFactory = gerritEventFactoryMap.getEventFactory(jsonNode.get("type").asText());
//        if (gerritEventFactory != null) {
//            EventModel eventModel = gerritEventFactory.createInstance(jsonNode.toString()).wrapperPushMessage();
//            eventProducer.fireEvent(eventModel);
//        }
//        send("x");
//    }
//
//    private void send(String msg) {
//        EventModel model = new EventModel();
//        String error = "";
//        TextcardMessagRequest textcardMessagRequest = new TextcardMessagRequest();
//        textcardMessagRequest.setTouser("hguohua")
//                .setMsgtype("textcard")
//                .setToparty("")
//                .setTotag("");
//
//        /**格式定义*/
//
//        textcardMessagRequest.setTextCard("你有一个新的评审通知", "description", "urlll", "详情");
////        }
//
//        /**发送消息。包括错误信息的收集*/
//        String json = null;
//        try {
//            json = objectMapper.writeValueAsString(textcardMessagRequest);
//        } catch (JsonProcessingException e) {
//            log.error("json序列化失败,{}-{}", e.getMessage(), e.getCause());
//            model.setExt("content", textcardMessagRequest.toString());
//            error = this.getClass().getName() + "-json序列化失败";
//        }
//        model.setExt(Message.WRAPPER_DATA_ERROR, error);
//        /**发送信息*/
//        messageSendService.send(model, json);
//    }
//
//}
