package com.realtimecollab.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentPresenceResponse {
    private Long documentId;
    private List<DocumentPresenceMessage> activeUsers;
    private LocalDateTime updatedAt;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public List<DocumentPresenceMessage> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(List<DocumentPresenceMessage> activeUsers) {
        this.activeUsers = activeUsers;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
