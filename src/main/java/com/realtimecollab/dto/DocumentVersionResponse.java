package com.realtimecollab.dto;

import java.time.LocalDateTime;

public class DocumentVersionResponse {
    private Long id;
    private Long documentId;
    private Long documentVersionNumber;
    private String contentSnapshot;
    private Long editedById;
    private String editedByName;
    private String editedByEmail;
    private LocalDateTime editedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getDocumentVersionNumber() {
        return documentVersionNumber;
    }

    public void setDocumentVersionNumber(Long documentVersionNumber) {
        this.documentVersionNumber = documentVersionNumber;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot) {
        this.contentSnapshot = contentSnapshot;
    }

    public Long getEditedById() {
        return editedById;
    }

    public void setEditedById(Long editedById) {
        this.editedById = editedById;
    }

    public String getEditedByName() {
        return editedByName;
    }

    public void setEditedByName(String editedByName) {
        this.editedByName = editedByName;
    }

    public String getEditedByEmail() {
        return editedByEmail;
    }

    public void setEditedByEmail(String editedByEmail) {
        this.editedByEmail = editedByEmail;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
