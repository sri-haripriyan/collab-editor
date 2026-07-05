package com.collab.codeeditor.dto;

import com.collab.codeeditor.model.CodeSnapshot;

import java.time.LocalDateTime;

public class SnapshotDto {
    private Long id;
    private String content;
    private String language;
    private LocalDateTime savedAt;
    private String savedByUsername;

    public SnapshotDto() {
    }

    public SnapshotDto(CodeSnapshot snapshot) {
        this.id = snapshot.getId();
        this.content = snapshot.getContent();
        this.language = snapshot.getLanguage();
        this.savedAt = snapshot.getSavedAt();
        this.savedByUsername = snapshot.getSavedBy() != null ? snapshot.getSavedBy().getUsername() : "System";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public String getSavedByUsername() {
        return savedByUsername;
    }

    public void setSavedByUsername(String savedByUsername) {
        this.savedByUsername = savedByUsername;
    }
}
