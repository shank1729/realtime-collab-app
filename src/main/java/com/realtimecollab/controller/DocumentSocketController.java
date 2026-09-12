package com.realtimecollab.controller;

import com.realtimecollab.dto.DocumentEditMessage;
import com.realtimecollab.dto.DocumentPresenceResponse;
import com.realtimecollab.dto.DocumentSocketMessage;
import com.realtimecollab.entity.Document;
import com.realtimecollab.mapper.DocumentMapper;
import com.realtimecollab.service.DocumentPresenceService;
import com.realtimecollab.service.DocumentService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class DocumentSocketController {

    private final DocumentService documentService;
    private final DocumentPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentMapper documentMapper;

    public DocumentSocketController(
            DocumentService documentService,
            DocumentPresenceService presenceService,
            SimpMessagingTemplate messagingTemplate,
            DocumentMapper documentMapper) {
        this.documentService = documentService;
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
        this.documentMapper = documentMapper;
    }

    @MessageMapping("/editDocument")
    public void handleDocumentEdit(DocumentEditMessage updatedDoc, Principal principal) {
        requirePrincipal(principal);

        Document savedDocument = documentService.updateDocumentContent(
                updatedDoc.getId(),
                updatedDoc.getContent(),
                principal.getName()
        );

        DocumentSocketMessage message = documentMapper.toSocketMessage(savedDocument);
        messagingTemplate.convertAndSend("/topic/documents/" + savedDocument.getId(), message);
    }

    @MessageMapping("/documents/{documentId}/presence/join")
    public void joinDocumentPresence(
            @DestinationVariable Long documentId,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        requirePrincipal(principal);
        broadcastPresence(documentId, presenceService.join(
                documentId,
                headerAccessor.getSessionId(),
                principal.getName()
        ));
    }

    @MessageMapping("/documents/{documentId}/presence/typing")
    public void markTyping(
            @DestinationVariable Long documentId,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        requirePrincipal(principal);
        broadcastPresence(documentId, presenceService.markTyping(
                documentId,
                headerAccessor.getSessionId(),
                principal.getName()
        ));
    }

    @MessageMapping("/documents/{documentId}/presence/viewing")
    public void markViewing(
            @DestinationVariable Long documentId,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        requirePrincipal(principal);
        broadcastPresence(documentId, presenceService.markViewing(
                documentId,
                headerAccessor.getSessionId(),
                principal.getName()
        ));
    }

    private void broadcastPresence(Long documentId, DocumentPresenceResponse response) {
        messagingTemplate.convertAndSend("/topic/documents/" + documentId + "/presence", response);
    }

    private void requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("WebSocket message requires authentication.");
        }
    }
}
