package com.collab.codeeditor.config;

import com.collab.codeeditor.websocket.RoomWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public WebSocketConfig(RoomWebSocketHandler roomWebSocketHandler) {
        this.roomWebSocketHandler = roomWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomWebSocketHandler, "/ws/room")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}

