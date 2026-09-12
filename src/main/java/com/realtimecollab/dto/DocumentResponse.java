package com.realtimecollab.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentResponse {
    private Long id;
    private String title;
    private String content;
    private Long ownerId;
    private String ownerEmail;
    private String ownerName;
    private List<CollaboratorSummaryResponse> collaborators;
    private Long version;
    private Long lastEditedById;
    private String lastEditedByName;
    private String lastEditedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public List<CollaboratorSummaryResponse> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<CollaboratorSummaryResponse> collaborators) {
        this.collaborators = collaborators;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getLastEditedById() {
        return lastEditedById;
    }

    public void setLastEditedById(Long lastEditedById) {
        this.lastEditedById = lastEditedById;
    }

    public String getLastEditedByName() {
        return lastEditedByName;
    }

    public void setLastEditedByName(String lastEditedByName) {
        this.lastEditedByName = lastEditedByName;
    }

    public String getLastEditedByEmail() {
        return lastEditedByEmail;
    }

    public void setLastEditedByEmail(String lastEditedByEmail) {
        this.lastEditedByEmail = lastEditedByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
