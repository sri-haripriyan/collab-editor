package com.collab.codeeditor.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalSessionRegistry {

    // Map: RoomCode -> (SessionId -> Session)
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // Map: SessionId -> RoomCode
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    // Map: SessionId -> UserId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    public void register(String roomCode, Long userId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        sessionToRoom.put(session.getId(), roomCode);
        sessionToUser.put(session.getId(), userId);
    }

    public void unregister(WebSocketSession session) {
        String sessionId = session.getId();
        String roomCode = sessionToRoom.remove(sessionId);
        sessionToUser.remove(sessionId);
        
        if (roomCode != null) {
            Map<String, WebSocketSession> sessions = roomSessions.get(roomCode);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomCode);
                }
            }
        }
    }

    public Map<String, WebSocketSession> getSessionsInRoom(String roomCode) {
        return roomSessions.getOrDefault(roomCode, Map.of());
    }

    public String getRoomCode(String sessionId) {
        return sessionToRoom.get(sessionId);
    }

    public Long getUserId(String sessionId) {
        return sessionToUser.get(sessionId);
    }
}
