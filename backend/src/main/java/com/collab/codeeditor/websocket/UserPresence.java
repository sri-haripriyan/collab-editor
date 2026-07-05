package com.collab.codeeditor.websocket;

public class UserPresence {
    private Long userId;
    private String username;
    private String role;
    private boolean isMuted;
    private String color;

    public UserPresence() {
    }

    public UserPresence(Long userId, String username, String role, boolean isMuted, String color) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.isMuted = isMuted;
        this.color = color;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
