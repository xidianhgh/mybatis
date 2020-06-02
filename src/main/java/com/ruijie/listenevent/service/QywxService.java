package com.ruijie.listenevent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.listenevent.common.constant.ErrorConstant;
import com.ruijie.listenevent.dto.EventModel;
import com.ruijie.listenevent.message.MessageSendService;
import com.ruijie.listenevent.message.qywx.request.TextMessageRequest;
import com.ruijie.listenevent.message.qywx.request.TextcardMessagRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QywxService {
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    @Qualifier("QywxMessageSendService")
    MessageSendService messageSendService;

    public void sendMessage(String msg) {
        EventModel model = new EventModel();
        String error = "";
        TextcardMessagRequest textcardMessagRequest = new TextcardMessagRequest();
        textcardMessagRequest.setTouser("hguohua")
                .setMsgtype("textcard")
                .setToparty("")
                .setTotag("");

        /**格式定义*/
        textcardMessagRequest.setTextCard("你有一个新的评审通知", msg, "urlll", "详情");

        /**发送消息。包括错误信息的收集*/
        String json = null;
        try {
            json = objectMapper.writeValueAsString(textcardMessagRequest);
        } catch (JsonProcessingException e) {
            log.error("json序列化失败,{}-{}", e.getMessage(), e.getCause());
            model.setExt("content", textcardMessagRequest.toString());
            error = this.getClass().getName() + "-json序列化失败";
        }
        model.setExt(ErrorConstant.WRAPPER_DATA_ERROR, error);
        /**发送信息*/
        messageSendService.send(model, json);
    }

    public void sendMessageTxt(String msg,String toUser) {
        EventModel model = new EventModel();
        String error = "";
        TextMessageRequest txtMsgRequest = new TextMessageRequest();
        txtMsgRequest.setTouser(toUser)
                .setMsgtype("text")
                .setToparty("")
                .setTotag("");

        /**格式定义*/
        txtMsgRequest.getText().setContent(msg);

        /**发送消息。包括错误信息的收集*/
        String json = null;
        try {
            json = objectMapper.writeValueAsString(txtMsgRequest);
        } catch (JsonProcessingException e) {
            log.error("json序列化失败,{}-{}", e.getMessage(), e.getCause());
            model.setExt("content", txtMsgRequest.toString());
            error = this.getClass().getName() + "-json序列化失败";
        }
        model.setExt(ErrorConstant.WRAPPER_DATA_ERROR, error);
        /**发送信息*/
        messageSendService.send(model, json);
    }

}
