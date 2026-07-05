package com.collab.codeeditor.websocket;

import com.collab.codeeditor.dto.WsMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publish(String roomCode, WsMessageDto message) {
        String topic = "room:" + roomCode;
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(topic, jsonMessage);
        } catch (Exception e) {
            // Log serialization errors
        }
    }
}
