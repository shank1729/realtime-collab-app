package com.realtimecollab.service;

import com.realtimecollab.dto.DocumentPresenceMessage;
import com.realtimecollab.dto.DocumentPresenceResponse;
import com.realtimecollab.entity.User;
import com.realtimecollab.exception.ResourceNotFoundException;
import com.realtimecollab.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentPresenceService {
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final Map<Long, Map<String, PresenceSession>> sessionsByDocument = new ConcurrentHashMap<>();
    private final Map<String, Long> documentBySession = new ConcurrentHashMap<>();

    public DocumentPresenceService(UserRepository userRepository, DocumentService documentService) {
        this.userRepository = userRepository;
        this.documentService = documentService;
    }

    public DocumentPresenceResponse join(Long documentId, String sessionId, String email) {
        User user = getUser(email);
        documentService.getById(documentId, email);

        LocalDateTime now = LocalDateTime.now();
        sessionsByDocument
                .computeIfAbsent(documentId, ignored -> new ConcurrentHashMap<>())
                .put(sessionId, new PresenceSession(documentId, sessionId, user, "VIEWING", now, now));
        documentBySession.put(sessionId, documentId);

        return snapshot(documentId);
    }

    public DocumentPresenceResponse markTyping(Long documentId, String sessionId, String email) {
        return updateStatus(documentId, sessionId, email, "TYPING");
    }

    public DocumentPresenceResponse markViewing(Long documentId, String sessionId, String email) {
        return updateStatus(documentId, sessionId, email, "VIEWING");
    }

    public DocumentPresenceResponse leave(String sessionId) {
        Long documentId = documentBySession.remove(sessionId);

        if (documentId == null) {
            return null;
        }

        Map<String, PresenceSession> documentSessions = sessionsByDocument.get(documentId);
        if (documentSessions != null) {
            documentSessions.remove(sessionId);
            if (documentSessions.isEmpty()) {
                sessionsByDocument.remove(documentId);
            }
        }

        return snapshot(documentId);
    }

    public DocumentPresenceResponse snapshot(Long documentId) {
        Map<String, PresenceSession> sessions = sessionsByDocument.getOrDefault(documentId, Map.of());
        List<DocumentPresenceMessage> activeUsers = sessions.values().stream()
                .map(PresenceSession::toMessage)
                .sorted(Comparator.comparing(DocumentPresenceMessage::getName))
                .toList();

        DocumentPresenceResponse response = new DocumentPresenceResponse();
        response.setDocumentId(documentId);
        response.setActiveUsers(activeUsers);
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private DocumentPresenceResponse updateStatus(
            Long documentId,
            String sessionId,
            String email,
            String status) {
        User user = getUser(email);
        documentService.getById(documentId, email);

        Map<String, PresenceSession> documentSessions =
                sessionsByDocument.computeIfAbsent(documentId, ignored -> new ConcurrentHashMap<>());
        PresenceSession session = documentSessions.get(sessionId);

        LocalDateTime now = LocalDateTime.now();
        if (session == null) {
            session = new PresenceSession(documentId, sessionId, user, status, now, now);
            documentSessions.put(sessionId, session);
            documentBySession.put(sessionId, documentId);
        } else {
            session.setStatus(status);
            session.setLastSeenAt(now);
        }

        return snapshot(documentId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private static class PresenceSession {
        private final Long documentId;
        private final String sessionId;
        private final User user;
        private String status;
        private final LocalDateTime joinedAt;
        private LocalDateTime lastSeenAt;

        private PresenceSession(
                Long documentId,
                String sessionId,
                User user,
                String status,
                LocalDateTime joinedAt,
                LocalDateTime lastSeenAt) {
            this.documentId = documentId;
            this.sessionId = sessionId;
            this.user = user;
            this.status = status;
            this.joinedAt = joinedAt;
            this.lastSeenAt = lastSeenAt;
        }

        private DocumentPresenceMessage toMessage() {
            DocumentPresenceMessage message = new DocumentPresenceMessage();
            message.setUserId(user.getId());
            message.setName(user.getName());
            message.setEmail(user.getEmail());
            message.setStatus(status);
            message.setJoinedAt(joinedAt);
            message.setLastSeenAt(lastSeenAt);
            return message;
        }

        private void setStatus(String status) {
            this.status = status;
        }

        private void setLastSeenAt(LocalDateTime lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
        }
    }
}
