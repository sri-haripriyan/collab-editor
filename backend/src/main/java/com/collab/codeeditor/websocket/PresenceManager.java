package com.collab.codeeditor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PresenceManager {

    private static final String PRESENCE_KEY_PREFIX = "room:%s:presence";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getPresenceKey(String roomCode) {
        return String.format(PRESENCE_KEY_PREFIX, roomCode);
    }

    public void addUser(String roomCode, UserPresence presence) {
        String key = getPresenceKey(roomCode);
        try {
            String valueJson = objectMapper.writeValueAsString(presence);
            redisTemplate.opsForHash().put(key, presence.getUserId().toString(), valueJson);
        } catch (Exception e) {
            // Log serialization errors
        }
    }

    public void removeUser(String roomCode, Long userId) {
        String key = getPresenceKey(roomCode);
        redisTemplate.opsForHash().delete(key, userId.toString());
    }

    public List<UserPresence> getUsersInRoom(String roomCode) {
        String key = getPresenceKey(roomCode);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        List<UserPresence> presenceList = new ArrayList<>();
        
        for (Object value : entries.values()) {
            try {
                UserPresence presence = objectMapper.readValue((String) value, UserPresence.class);
                presenceList.add(presence);
            } catch (Exception e) {
                // Log deserialization errors
            }
        }
        return presenceList;
    }

    public void updateMicState(String roomCode, Long userId, boolean isMuted) {
        String key = getPresenceKey(roomCode);
        Object existing = redisTemplate.opsForHash().get(key, userId.toString());
        if (existing != null) {
            try {
                UserPresence presence = objectMapper.readValue((String) existing, UserPresence.class);
                presence.setMuted(isMuted);
                String updatedJson = objectMapper.writeValueAsString(presence);
                redisTemplate.opsForHash().put(key, userId.toString(), updatedJson);
            } catch (Exception e) {
                // Log
            }
        }
    }

    public void updateRole(String roomCode, Long userId, String role) {
        String key = getPresenceKey(roomCode);
        Object existing = redisTemplate.opsForHash().get(key, userId.toString());
        if (existing != null) {
            try {
                UserPresence presence = objectMapper.readValue((String) existing, UserPresence.class);
                presence.setRole(role);
                String updatedJson = objectMapper.writeValueAsString(presence);
                redisTemplate.opsForHash().put(key, userId.toString(), updatedJson);
            } catch (Exception e) {
                // Log
            }
        }
    }

    public UserPresence getUser(String roomCode, Long userId) {
        String key = getPresenceKey(roomCode);
        Object val = redisTemplate.opsForHash().get(key, userId.toString());
        if (val != null) {
            try {
                return objectMapper.readValue((String) val, UserPresence.class);
            } catch (Exception e) {
                // Log
            }
        }
        return null;
    }
}
