import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const WebSocketContext = createContext(null);
const wsUrl = process.env.REACT_APP_WS_URL || "http://localhost:8080/ws";

function buildAuthenticatedWsUrl(token) {
  if (!token) {
    return wsUrl;
  }

  const separator = wsUrl.includes("?") ? "&" : "?";
  return `${wsUrl}${separator}token=${encodeURIComponent(token)}`;
}

export const WebSocketProvider = ({ children, token }) => {
  const [connected, setConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const [presenceMessage, setPresenceMessage] = useState(null);
  const clientRef = useRef(null);
  const documentSubscriptionRef = useRef(null);
  const presenceSubscriptionRef = useRef(null);

  const publishJson = useCallback((destination, payload = {}) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination,
        body: JSON.stringify(payload),
      });
    } else {
      console.warn("WebSocket not connected.");
    }
  }, []);

  useEffect(() => {
    if (!token) {
      setConnected(false);
      setPresenceMessage(null);
      return undefined;
    }

    const socket = new SockJS(buildAuthenticatedWsUrl(token));
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error("Broker error:", frame.headers["message"]);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (documentSubscriptionRef.current) {
        documentSubscriptionRef.current.unsubscribe();
      }
      if (presenceSubscriptionRef.current) {
        presenceSubscriptionRef.current.unsubscribe();
      }
      client.deactivate();
    };
  }, [token]);

  const subscribeToDocument = useCallback((documentId) => {
    if (!clientRef.current?.connected || !documentId) {
      return;
    }

    if (documentSubscriptionRef.current) {
      documentSubscriptionRef.current.unsubscribe();
    }
    if (presenceSubscriptionRef.current) {
      presenceSubscriptionRef.current.unsubscribe();
    }
    setPresenceMessage(null);

    documentSubscriptionRef.current = clientRef.current.subscribe(
      `/topic/documents/${documentId}`,
      (message) => {
        const body = JSON.parse(message.body);
        setLastMessage(body);
      }
    );

    presenceSubscriptionRef.current = clientRef.current.subscribe(
      `/topic/documents/${documentId}/presence`,
      (message) => {
        const body = JSON.parse(message.body);
        setPresenceMessage(body);
      }
    );

    publishJson(`/app/documents/${documentId}/presence/join`);
  }, [publishJson]);

  const sendUpdate = useCallback((documentUpdate) => {
    publishJson("/app/editDocument", documentUpdate);
  }, [publishJson]);

  const sendPresenceTyping = useCallback((documentId) => {
    if (documentId) {
      publishJson(`/app/documents/${documentId}/presence/typing`);
    }
  }, [publishJson]);

  const sendPresenceViewing = useCallback((documentId) => {
    if (documentId) {
      publishJson(`/app/documents/${documentId}/presence/viewing`);
    }
  }, [publishJson]);

  return (
    <WebSocketContext.Provider
      value={{
        connected,
        lastMessage,
        presenceMessage,
        sendPresenceTyping,
        sendPresenceViewing,
        sendUpdate,
        subscribeToDocument,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => useContext(WebSocketContext);
