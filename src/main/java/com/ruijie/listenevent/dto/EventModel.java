package com.ruijie.listenevent.dto;

import java.util.HashMap;
import java.util.Map;


public class EventModel {
    private EventType type;
    /**触发人，谁提交了，谁触发了这个行为*/
    private String actorName;

    private int entityType;

    private int entityId;

    /**介绍认识谁*/
    private String entityOwnerName;

    private Map<String, String> exts = new HashMap<String, String>();

    public EventModel() {

    }

    public EventModel setExt(String key, String value) {
        exts.put(key, value);
        return this;
    }

    public EventModel(EventType type) {
        this.type = type;
    }

    public String getExt(String key) {
        return exts.get(key);
    }

    public EventType getType() {
        return type;
    }

    public EventModel setType(EventType type) {
        this.type = type;
        return this;
    }

    public String getActorName() {
        return actorName;
    }

    public EventModel setActorName(String actorName) {
        this.actorName = actorName;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public EventModel setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public EventModel setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityOwnerName() {
        return entityOwnerName;
    }

    public EventModel setEntityOwnerName(String entityOwnerName) {
        this.entityOwnerName = entityOwnerName;
        return this;
    }

    public Map<String, String> getExts() {
        return exts;
    }

    public EventModel setExts(Map<String, String> exts) {
        this.exts = exts;
        return this;
    }
}
