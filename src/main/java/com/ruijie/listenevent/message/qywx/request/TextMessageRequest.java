package com.ruijie.listenevent.message.qywx.request;

import lombok.Data;

@Data
public class TextMessageRequest extends QywxMessageRequest {
    @Data
    public class Content {
        String content;
    }

    Content text = new Content();
}
