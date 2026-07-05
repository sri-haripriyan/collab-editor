package com.collab.codeeditor.websocket;

import com.collab.codeeditor.dto.WsMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RedisRoomSubscriber implements MessageListener {

    @Autowired
    private LocalSessionRegistry sessionRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        
        try {
            WsMessageDto wsMessage = objectMapper.readValue(body, WsMessageDto.class);
            String roomCode = wsMessage.getRoomId();
            
            Map<String, WebSocketSession> localSessions = sessionRegistry.getSessionsInRoom(roomCode);
            if (localSessions.isEmpty()) {
                return;
            }

            String jsonPayload = objectMapper.writeValueAsString(wsMessage);
            TextMessage textMessage = new TextMessage(jsonPayload);

            // Check if it's a targeted WebRTC signal
            Map<String, Object> payload = wsMessage.getPayload();
            Long targetUserId = null;
            if (payload != null && payload.containsKey("targetUserId")) {
                Object targetIdObj = payload.get("targetUserId");
                if (targetIdObj instanceof Number) {
                    targetUserId = ((Number) targetIdObj).longValue();
                } else if (targetIdObj instanceof String) {
                    targetUserId = Long.parseLong((String) targetIdObj);
                }
            }

            for (WebSocketSession session : localSessions.values()) {
                if (!session.isOpen()) {
                    continue;
                }

                Long sessionUserId = sessionRegistry.getUserId(session.getId());
                if (sessionUserId == null) {
                    continue;
                }

                // If targeted signal, send only to target.
                if (targetUserId != null) {
                    if (sessionUserId.equals(targetUserId)) {
                        sendToSession(session, textMessage);
                    }
                } else {
                    // Otherwise, broadcast to everyone except the sender
                    if (!sessionUserId.equals(wsMessage.getSenderId())) {
                        sendToSession(session, textMessage);
                    }
                }
            }

        } catch (Exception e) {
            // Log subscription parsing errors
        }
    }

    private void sendToSession(WebSocketSession session, TextMessage message) {
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            // Log connection send error
        }
    }
}
