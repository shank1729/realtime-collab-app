package com.realtimecollab.mapper;

import com.realtimecollab.dto.CollaboratorSummaryResponse;
import com.realtimecollab.dto.DocumentSocketMessage;
import com.realtimecollab.dto.DocumentRequest;
import com.realtimecollab.dto.DocumentResponse;
import com.realtimecollab.dto.DocumentVersionResponse;
import com.realtimecollab.entity.Document;
import com.realtimecollab.entity.DocumentAccess;
import com.realtimecollab.entity.DocumentVersion;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public Document toEntity(DocumentRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        return document;
    }

    public DocumentResponse toResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setContent(document.getContent());
        if (document.getOwner() != null) {
            response.setOwnerId(document.getOwner().getId());
            response.setOwnerEmail(document.getOwner().getEmail());
            response.setOwnerName(document.getOwner().getName());
        }
        response.setCollaborators(document.getCollaboratorAccesses().stream()
                .map(this::toCollaboratorSummary)
                .toList());
        response.setVersion(document.getVersion());
        if (document.getLastEditedBy() != null) {
            response.setLastEditedById(document.getLastEditedBy().getId());
            response.setLastEditedByName(document.getLastEditedBy().getName());
            response.setLastEditedByEmail(document.getLastEditedBy().getEmail());
        }
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    public DocumentSocketMessage toSocketMessage(Document document) {
        DocumentSocketMessage message = new DocumentSocketMessage();
        message.setId(document.getId());
        message.setContent(document.getContent());
        message.setVersion(document.getVersion());
        message.setUpdatedAt(document.getUpdatedAt());
        return message;
    }

    public DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        DocumentVersionResponse response = new DocumentVersionResponse();
        response.setId(version.getId());
        response.setDocumentId(version.getDocument().getId());
        response.setDocumentVersionNumber(version.getDocumentVersionNumber());
        response.setContentSnapshot(version.getContentSnapshot());
        if (version.getEditedBy() != null) {
            response.setEditedById(version.getEditedBy().getId());
            response.setEditedByName(version.getEditedBy().getName());
            response.setEditedByEmail(version.getEditedBy().getEmail());
        }
        response.setEditedAt(version.getEditedAt());
        return response;
    }

    private CollaboratorSummaryResponse toCollaboratorSummary(DocumentAccess access) {
        CollaboratorSummaryResponse summary = new CollaboratorSummaryResponse();
        var user = access.getUser();
        summary.setId(user.getId());
        summary.setName(user.getName());
        summary.setEmail(user.getEmail());
        summary.setRole(access.getRole().name());
        return summary;
    }
}
