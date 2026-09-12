package com.realtimecollab.config;

import com.realtimecollab.dto.DocumentPresenceResponse;
import com.realtimecollab.service.DocumentPresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceEventListener {
    private final DocumentPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPresenceEventListener(
            DocumentPresenceService presenceService,
            SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        DocumentPresenceResponse response = presenceService.leave(event.getSessionId());

        if (response != null) {
            messagingTemplate.convertAndSend(
                    "/topic/documents/" + response.getDocumentId() + "/presence",
                    response
            );
        }
    }
}
