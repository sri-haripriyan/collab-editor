package com.collab.codeeditor.dto;

import java.util.Map;

public class WsMessageDto {
    private String type;
    private String roomId;
    private Long senderId;
    private String senderName;
    private Map<String, Object> payload;

    public WsMessageDto() {
    }

    public WsMessageDto(String type, String roomId, Long senderId, String senderName, Map<String, Object> payload) {
        this.type = type;
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.payload = payload;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
