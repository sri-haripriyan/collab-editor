package com.collab.codeeditor.dto;

import com.collab.codeeditor.model.Room;

import java.time.LocalDateTime;

public class RoomDto {
    private Long id;
    private String code;
    private String name;
    private Long ownerId;
    private String ownerUsername;
    private String role; // 'OWNER', 'EDITOR', 'VIEWER'
    private LocalDateTime createdAt;

    public RoomDto() {
    }

    public RoomDto(Room room, String role) {
        this.id = room.getId();
        this.code = room.getCode();
        this.name = room.getName();
        this.ownerId = room.getOwner().getId();
        this.ownerUsername = room.getOwner().getUsername();
        this.role = role;
        this.createdAt = room.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
