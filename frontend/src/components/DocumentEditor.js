import React, { useEffect, useMemo, useRef, useState } from "react";
import { useWebSocket } from "../context/WebSocketContext";
import {
  createDocument,
  deleteDocument,
  getDocumentById,
  getDocumentHistory,
  getDocuments,
  shareDocument,
} from "../services/documentService";

const starterDocumentPayload = {
  title: "Getting Started",
  content: "Start collaborating on your first shared document.",
};

const roleLabels = {
  OWNER: "Owner",
  EDITOR: "Editor",
  VIEWER: "Viewer",
};

const presenceLabels = {
  TYPING: "Typing",
  VIEWING: "Viewing",
};

function normalizeRole(role) {
  const rawRole =
    typeof role === "string"
      ? role
      : role?.name || role?.value || role?.role || "";

  return rawRole.toUpperCase().replace(/^ROLE_/, "");
}

function normalizeStatus(status) {
  return status ? status.toUpperCase() : "";
}

export default function DocumentEditor({ session, onLogout }) {
  const {
    connected,
    lastMessage,
    presenceMessage,
    sendPresenceTyping,
    sendPresenceViewing,
    sendUpdate,
    subscribeToDocument,
  } = useWebSocket();
  const [documents, setDocuments] = useState([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [updatedAt, setUpdatedAt] = useState("");
  const [collaborators, setCollaborators] = useState([]);
  const [versionNumber, setVersionNumber] = useState(null);
  const [lastEditedByName, setLastEditedByName] = useState("");
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [syncStatus, setSyncStatus] = useState("Loading documents...");
  const [shareEmail, setShareEmail] = useState("");
  const [shareRole, setShareRole] = useState("EDITOR");
  const [shareStatus, setShareStatus] = useState("");
  const debounceTimerRef = useRef(null);

  const selectedDocument = useMemo(
    () => documents.find((document) => document.id === selectedDocumentId) || null,
    [documents, selectedDocumentId]
  );

  const currentUserRole = useMemo(() => {
    if (!selectedDocument) {
      return "VIEWER";
    }

    if (
      selectedDocument.ownerEmail?.toLowerCase() === session.email?.toLowerCase() ||
      selectedDocument.ownerId === session.id
    ) {
      return "OWNER";
    }

    const collaborator = (selectedDocument.collaborators || []).find(
      (person) => person.email?.toLowerCase() === session.email?.toLowerCase()
    );

    return normalizeRole(collaborator?.role) || "VIEWER";
  }, [selectedDocument, session.email, session.id]);

  const canEditDocument = currentUserRole === "OWNER" || currentUserRole === "EDITOR";
  const canManageDocument = currentUserRole === "OWNER";
  const isViewer = currentUserRole === "VIEWER";
  const currentRoleLabel = roleLabels[currentUserRole] || "Viewer";
  const activeUsers = useMemo(() => {
    if (presenceMessage?.documentId !== selectedDocumentId) {
      return [];
    }

    return (presenceMessage.activeUsers || []).filter(
      (user) => user.email?.toLowerCase() !== session.email?.toLowerCase()
    );
  }, [presenceMessage, selectedDocumentId, session.email]);

  useEffect(() => {
    const loadWorkspace = async () => {
      try {
        setLoading(true);
        setError("");

        let workspaceDocuments = await getDocuments();
        if (workspaceDocuments.length === 0) {
          const createdDocument = await createDocument(starterDocumentPayload);
          workspaceDocuments = [createdDocument];
        }

        setDocuments(workspaceDocuments);
        setSelectedDocumentId(workspaceDocuments[0].id);
        setSyncStatus("Workspace loaded");
      } catch (loadError) {
        setError(loadError.message || "Unable to load your documents.");
        setSyncStatus("Could not load workspace");
      } finally {
        setLoading(false);
      }
    };

    loadWorkspace();
  }, []);

  useEffect(() => {
    const loadDocument = async () => {
      if (!selectedDocumentId) {
        return;
      }

      try {
        const [document, documentHistory] = await Promise.all([
          getDocumentById(selectedDocumentId),
          getDocumentHistory(selectedDocumentId),
        ]);
        setTitle(document.title || "Untitled document");
        setContent(document.content || "");
        setUpdatedAt(document.updatedAt || document.createdAt || "");
        setCollaborators(document.collaborators || []);
        setVersionNumber(document.version ?? null);
        setLastEditedByName(document.lastEditedByName || "");
        setHistory(documentHistory);
        setDocuments((prev) =>
          prev.map((workspaceDocument) =>
            workspaceDocument.id === document.id
              ? { ...workspaceDocument, ...document }
              : workspaceDocument
          )
        );
        setShareStatus("");
        setSyncStatus("Document loaded");
        if (connected) {
          sendPresenceViewing(selectedDocumentId);
        }
      } catch (loadError) {
        setError(loadError.message || "Unable to load the selected document.");
      }
    };

    loadDocument();
    if (connected) {
      subscribeToDocument(selectedDocumentId);
    }
  }, [connected, selectedDocumentId, sendPresenceViewing, subscribeToDocument]);

  useEffect(() => {
    if (selectedDocumentId && lastMessage && lastMessage.id === selectedDocumentId) {
      setContent(lastMessage.content || "");
      setUpdatedAt(lastMessage.updatedAt || new Date().toISOString());
      setVersionNumber(lastMessage.version ?? versionNumber);
      setSyncStatus(isViewer ? "Read-only access" : "Synced live");
    }
  }, [isViewer, lastMessage, selectedDocumentId, versionNumber]);

  useEffect(() => {
    if (isViewer) {
      setSyncStatus("Read-only access");
    }
  }, [isViewer, selectedDocumentId]);

  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  const handleChange = (event) => {
    if (!canEditDocument) {
      setSyncStatus("Read-only access");
      return;
    }

    const nextContent = event.target.value;
    setContent(nextContent);
    setSyncStatus(connected ? "Typing..." : "Offline changes");

    if (connected) {
      sendPresenceTyping(selectedDocumentId);
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(() => {
      if (selectedDocumentId) {
        sendUpdate({
          id: selectedDocumentId,
          content: nextContent,
        });
      }
      setUpdatedAt(new Date().toISOString());
      setSyncStatus(connected ? "Live update sent" : "Waiting for connection");
      if (connected) {
        sendPresenceViewing(selectedDocumentId);
      }
    }, 300);
  };

  const handleCreateDocument = async () => {
    try {
      const newDocument = await createDocument({
        title: `Shared Notes ${documents.length + 1}`,
        content: "Start writing here...",
      });
      setDocuments((prev) => [newDocument, ...prev]);
      setSelectedDocumentId(newDocument.id);
      setSyncStatus("New document created");
    } catch (createError) {
      setError(createError.message || "Unable to create a new document.");
    }
  };

  const handleShare = async (event) => {
    event.preventDefault();
    if (!selectedDocumentId || !canManageDocument) {
      setShareStatus("Only the document owner can share this document.");
      return;
    }

    try {
      const updatedDocument = await shareDocument(
        selectedDocumentId,
        shareEmail,
        shareRole
      );
      if (process.env.NODE_ENV === "development") {
        console.info("Share request sent", {
          documentId: selectedDocumentId,
          email: shareEmail,
          role: shareRole,
        });
        console.info("Share response received", updatedDocument);
      }
      setCollaborators(updatedDocument.collaborators || []);
      setDocuments((prev) =>
        prev.map((document) =>
          document.id === updatedDocument.id ? updatedDocument : document
        )
      );
      setShareStatus(`Shared with ${shareEmail} as ${roleLabels[shareRole]}.`);
      setShareEmail("");
      setShareRole("EDITOR");
    } catch (shareError) {
      setShareStatus(shareError.message || "Unable to share the document.");
    }
  };

  const handleDeleteDocument = async () => {
    if (!selectedDocumentId || !canManageDocument) {
      setError("Only the document owner can delete this document.");
      return;
    }

    const confirmed = window.confirm(
      "Delete this document? This action cannot be undone."
    );

    if (!confirmed) {
      return;
    }

    try {
      await deleteDocument(selectedDocumentId);

      const remainingDocuments = documents.filter(
        (document) => document.id !== selectedDocumentId
      );

      if (remainingDocuments.length === 0) {
        const starterDocument = await createDocument(starterDocumentPayload);
        setDocuments([starterDocument]);
        setSelectedDocumentId(starterDocument.id);
      } else {
        setDocuments(remainingDocuments);
        setSelectedDocumentId(remainingDocuments[0].id);
      }

      setShareStatus("");
      setSyncStatus("Document deleted");
    } catch (deleteError) {
      setError(deleteError.message || "Unable to delete the document.");
    }
  };

  const formattedUpdatedAt = updatedAt
    ? new Date(updatedAt).toLocaleString()
    : "Not available";

  if (loading) {
    return (
      <main className="editor-layout">
        <section className="editor-card">
          <p className="editor-banner">Loading workspace...</p>
        </section>
      </main>
    );
  }

  if (error) {
    return (
      <main className="editor-layout">
        <section className="editor-card">
          <p className="editor-banner editor-banner-error">{error}</p>
        </section>
      </main>
    );
  }

  return (
    <main className="workspace-layout">
      <aside className="workspace-sidebar">
        <div className="workspace-sidebar-header">
          <p className="editor-kicker">Shared workspace</p>
          <h2>Your documents</h2>
          <button className="workspace-button" type="button" onClick={handleCreateDocument}>
            New document
          </button>
        </div>

        <div className="workspace-list">
          {documents.map((document) => (
            <button
              key={document.id}
              type="button"
              className={
                document.id === selectedDocumentId
                  ? "workspace-list-item workspace-list-item-active"
                  : "workspace-list-item"
              }
              onClick={() => setSelectedDocumentId(document.id)}
            >
              <strong>{document.title}</strong>
              <span>{document.ownerName || "Owner unavailable"}</span>
            </button>
          ))}
        </div>
      </aside>

      <section className="editor-card workspace-editor-card">
        <div className="editor-header">
          <div>
            <p className="editor-kicker">Real-time collaboration</p>
            <h1>{title}</h1>
            <p className="editor-meta">
              Signed in as {session.name} ({session.email})
            </p>
            <p className="editor-meta">
              Document #{selectedDocumentId} | Last updated {formattedUpdatedAt}
            </p>
            <p className="editor-meta">
              Version {versionNumber ?? "n/a"} | Last edited by {lastEditedByName || "Unknown"}
            </p>
          </div>
          <div className="editor-status-group">
            <span className={`editor-pill editor-pill-role-${currentUserRole.toLowerCase()}`}>
              {currentRoleLabel}
            </span>
            <span
              className={
                connected
                  ? "editor-pill editor-pill-connected"
                  : "editor-pill editor-pill-disconnected"
              }
            >
              {connected ? "Connected" : "Disconnected"}
            </span>
            <span className="editor-pill editor-pill-neutral">{syncStatus}</span>
            {canManageDocument ? (
              <button
                className="editor-pill editor-pill-danger"
                onClick={handleDeleteDocument}
                type="button"
              >
                Delete document
              </button>
            ) : null}
            <button
              className="editor-pill editor-pill-action"
              onClick={onLogout}
              type="button"
            >
              Sign out
            </button>
          </div>
        </div>

        <div className="workspace-meta-row">
          <div className="workspace-owner-panel">
            <span className="workspace-section-title">Document owner</span>
            <strong>{selectedDocument?.ownerName || "Unknown"}</strong>
            <span>{selectedDocument?.ownerEmail || "Unknown"}</span>
            {isViewer ? (
              <p className="workspace-readonly-note">
                You can view this document, but editing and sharing are owner-controlled.
              </p>
            ) : null}
          </div>
          <div className="workspace-collaborators">
            <span className="workspace-section-title">Collaborators</span>
            <div className="workspace-chip-row">
              {collaborators.length > 0 ? (
                collaborators.map((collaborator) => (
                  <span key={collaborator.id} className="workspace-chip">
                    <span>{collaborator.name}</span>
                    <small>{roleLabels[normalizeRole(collaborator.role)] || "Viewer"}</small>
                  </span>
                ))
              ) : (
                <span className="workspace-empty-text">No collaborators yet</span>
              )}
            </div>
          </div>
        </div>

        {canManageDocument ? (
          <form className="workspace-share-form" onSubmit={handleShare}>
            <label className="auth-field workspace-share-field">
              <span>Share with teammate email</span>
              <input
                type="email"
                value={shareEmail}
                onChange={(event) => setShareEmail(event.target.value)}
                placeholder="teammate@example.com"
                required
              />
            </label>
            <label className="auth-field workspace-role-field">
              <span>Role</span>
              <select
                value={shareRole}
                onChange={(event) => setShareRole(event.target.value)}
              >
                <option value="EDITOR">Editor</option>
                <option value="VIEWER">Viewer</option>
              </select>
            </label>
            <button className="workspace-button" type="submit">
              Share document
            </button>
          </form>
        ) : (
          <p className="editor-banner workspace-banner">
            Only the owner can share or delete this document.
          </p>
        )}
        {shareStatus ? <p className="workspace-empty-text">{shareStatus}</p> : null}

        <div className="workspace-editor-grid">
          <div>
            <label className="editor-label" htmlFor="document-content">
              Document content
            </label>
            <textarea
              id="document-content"
              value={content}
              onChange={handleChange}
              disabled={!canEditDocument}
              rows={14}
              className={canEditDocument ? "editor-textarea" : "editor-textarea editor-textarea-readonly"}
              placeholder={
                canEditDocument
                  ? "Start typing to broadcast live updates..."
                  : "Viewer access is read-only."
              }
            />
          </div>

          <aside className="workspace-side-panel">
            <section className="workspace-presence-card">
              <h3>Active now</h3>
              <div className="workspace-presence-list">
                {activeUsers.length > 0 ? (
                  activeUsers.map((user) => {
                    const status = normalizeStatus(user.status);
                    return (
                      <div key={user.userId || user.email} className="workspace-presence-item">
                        <span
                          className={
                            status === "TYPING"
                              ? "workspace-presence-dot workspace-presence-dot-typing"
                              : "workspace-presence-dot"
                          }
                        />
                        <div>
                          <strong>{user.name || user.email}</strong>
                          <span>{presenceLabels[status] || "Viewing"}</span>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <p className="workspace-empty-text">No other active viewers.</p>
                )}
              </div>
            </section>

            <section className="workspace-history-card">
              <h3>Version history</h3>
              <div className="workspace-history-list">
                {history.length > 0 ? (
                  history.map((entry) => (
                    <div key={entry.id} className="workspace-history-item">
                      <strong>Version {entry.documentVersionNumber}</strong>
                      <span>
                        {entry.editedByName || "Unknown"} •{" "}
                        {new Date(entry.editedAt).toLocaleString()}
                      </span>
                    </div>
                  ))
                ) : (
                  <p className="workspace-empty-text">No history available yet.</p>
                )}
              </div>
            </section>
          </aside>
        </div>
      </section>
    </main>
  );
}
