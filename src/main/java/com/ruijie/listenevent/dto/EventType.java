package com.ruijie.listenevent.dto;

public enum EventType {

    PIPELINE_CODE_REVIEW_PUSH(3),
    CODE_REVIEW_PUSH(4),
    COMMENT_ADD(5),
    REF_UPDATE(6),
    VOTE_DELETE(6),
    REVIEWER_ADD(7),
    DEFAULT(8);
    private int value;
    EventType(int value) { this.value = value; }
    public int getValue() { return value; }

    public static EventType queryByType(int value){
        if( value < 3){
            return  null;
        }
        for (EventType eventType : EventType.values()) {
            if (eventType.getValue() == value) {
                return eventType;
            }
        }
        return null;
    }

}
