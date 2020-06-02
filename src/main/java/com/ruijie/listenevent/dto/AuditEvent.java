package com.ruijie.listenevent.dto;

import lombok.Data;


@Data
public class AuditEvent {

    String uuid;
    String when;
    String sessionId;
    String who;
    String method;
    String what;
    String params;
    String result;
    String timeAtStart;
    String elapsed;



}
