package com.collab.codeeditor.websocket;

import com.collab.codeeditor.config.JwtUtils;
import com.collab.codeeditor.config.UserDetailsImpl;
import com.collab.codeeditor.dto.WsMessageDto;
import com.collab.codeeditor.model.Room;
import com.collab.codeeditor.model.RoomPermission;
import com.collab.codeeditor.model.RoomRole;
import com.collab.codeeditor.model.User;
import com.collab.codeeditor.repository.RoomPermissionRepository;
import com.collab.codeeditor.repository.RoomRepository;
import com.collab.codeeditor.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomPermissionRepository roomPermissionRepository;

    @Autowired
    private LocalSessionRegistry sessionRegistry;

    @Autowired
    private PresenceManager presenceManager;

    @Autowired
    private RedisPublisher redisPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[] NICE_COLORS = {
            "#FF5733", "#33FF57", "#3357FF", "#F3FF33", "#FF33F3", 
            "#33FFF3", "#FFA833", "#A833FF", "#33FFA8", "#FF3366"
    };

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = parseQueryParams(session.getUri());
        String token = params.get("token");
        String code = params.get("code");

        if (token == null || code == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        User user = null;
        try {
            String username = jwtUtils.extractUsername(token);
            UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            if (jwtUtils.validateToken(token, userDetails)) {
                user = userDetails.getUser();
            }
        } catch (Exception e) {
            // Auth failed
        }

        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Optional<Room> roomOpt = roomRepository.findByCode(code);
        if (roomOpt.isEmpty() || !roomOpt.get().isActive()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Room room = roomOpt.get();

        // Determine user role in room
        String role = "VIEWER";
        if (room.getOwner().getId().equals(user.getId())) {
            role = "OWNER";
        } else {
            Optional<RoomPermission> permissionOpt = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), user.getId());
            if (permissionOpt.isPresent()) {
                role = permissionOpt.get().getRole().name();
            } else {
                // By default grant editor
                RoomPermission rp = new RoomPermission(room, user, RoomRole.EDITOR);
                roomPermissionRepository.save(rp);
                role = "EDITOR";
            }
        }

        // Register session locally
        sessionRegistry.register(code, user.getId(), session);

        // Assign randomized pastel color
        Random random = new Random();
        String color = NICE_COLORS[random.nextInt(NICE_COLORS.length)];

        // Save presence details in Redis
        UserPresence presence = new UserPresence(user.getId(), user.getUsername(), role, false, color);
        presenceManager.addUser(code, presence);

        // Send confirmation to the joining user with their role and the active participants
        List<UserPresence> activeUsers = presenceManager.getUsersInRoom(code);
        Map<String, Object> joinAckPayload = new HashMap<>();
        joinAckPayload.put("role", role);
        joinAckPayload.put("activeUsers", activeUsers);
        
        WsMessageDto ackMessage = new WsMessageDto("JOIN_ACK", code, user.getId(), user.getUsername(), joinAckPayload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackMessage)));

        // Broadcast user joined to other users
        Map<String, Object> joinBroadcastPayload = new HashMap<>();
        joinBroadcastPayload.put("user", presence);
        
        WsMessageDto joinBroadcast = new WsMessageDto("USER_JOINED", code, user.getId(), user.getUsername(), joinBroadcastPayload);
        redisPublisher.publish(code, joinBroadcast);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        WsMessageDto wsMessage = objectMapper.readValue(payload, WsMessageDto.class);
        
        String roomCode = sessionRegistry.getRoomCode(session.getId());
        Long userId = sessionRegistry.getUserId(session.getId());

        if (roomCode == null || userId == null || !roomCode.equals(wsMessage.getRoomId())) {
            return; // Session is not associated with this room code
        }

        wsMessage.setSenderId(userId); // Ensure senderId is authenticated

        String type = wsMessage.getType();
        if ("MIC_STATE".equals(type)) {
            Boolean isMuted = (Boolean) wsMessage.getPayload().get("isMuted");
            if (isMuted != null) {
                presenceManager.updateMicState(roomCode, userId, isMuted);
                redisPublisher.publish(roomCode, wsMessage);
            }
        } else if ("PERMISSIONS_UPDATE".equals(type)) {
            // Verify if sender is Owner
            Room room = roomRepository.findByCode(roomCode).orElse(null);
            if (room != null && room.getOwner().getId().equals(userId)) {
                String targetUsername = (String) wsMessage.getPayload().get("username");
                String newRole = (String) wsMessage.getPayload().get("role"); // 'EDITOR', 'VIEWER'

                if (targetUsername != null && newRole != null) {
                    User targetUser = userRepository.findByUsername(targetUsername).orElse(null);
                    if (targetUser != null && !targetUser.getId().equals(userId)) {
                        RoomRole roleEnum = RoomRole.valueOf(newRole.toUpperCase());
                        
                        RoomPermission rp = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), targetUser.getId())
                                .orElse(new RoomPermission(room, targetUser, roleEnum));
                        rp.setRole(roleEnum);
                        roomPermissionRepository.save(rp);

                        presenceManager.updateRole(roomCode, targetUser.getId(), newRole);

                        // Broadcast to all clients
                        redisPublisher.publish(roomCode, wsMessage);
                    }
                }
            }
        } else {
            // For standard YJS_SYNC, CURSOR_UPDATE, SIGNAL (WebRTC), and CHAT_MESSAGE
            // simply broadcast to Redis topic for multi-instance sync
            redisPublisher.publish(roomCode, wsMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomCode = sessionRegistry.getRoomCode(session.getId());
        Long userId = sessionRegistry.getUserId(session.getId());

        if (roomCode != null && userId != null) {
            // Clean up session mappings
            sessionRegistry.unregister(session);
            
            // Remove user presence from Redis
            presenceManager.removeUser(roomCode, userId);

            // Broadcast user left
            WsMessageDto leftMessage = new WsMessageDto("USER_LEFT", roomCode, userId, null, Map.of());
            redisPublisher.publish(roomCode, leftMessage);
        }
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> queryParams = new HashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyVal = pair.split("=");
                if (keyVal.length > 1) {
                    queryParams.put(keyVal[0], keyVal[1]);
                } else if (keyVal.length == 1) {
                    queryParams.put(keyVal[0], "");
                }
            }
        }
        return queryParams;
    }
}
