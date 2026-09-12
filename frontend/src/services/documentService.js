import { getAuthHeaders, notifySessionExpired } from "./authService";

const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

async function parseJsonResponse(response) {
  if (!response.ok) {
    if (response.status === 401) {
      notifySessionExpired();
    }

    let errorMessage = "Request failed";

    try {
      const errorBody = await response.json();
      errorMessage = errorBody.message || errorMessage;
    } catch {
      errorMessage = `Request failed with status ${response.status}`;
    }

    throw new Error(errorMessage);
  }

  return response.json();
}

async function fetchWithAuth(path, options = {}) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeaders(),
      ...(options.headers || {}),
    },
  });

  return parseJsonResponse(response);
}

export function getDocuments() {
  return fetchWithAuth("/documents", { method: "GET" });
}

export function getDocumentById(documentId) {
  return fetchWithAuth(`/documents/${documentId}`, { method: "GET" });
}

export function createDocument(payload) {
  return fetchWithAuth("/documents", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function shareDocument(documentId, email, role) {
  return fetchWithAuth(`/documents/${documentId}/share`, {
    method: "POST",
    body: JSON.stringify({ email, role }),
  });
}

export function getDocumentHistory(documentId) {
  return fetchWithAuth(`/documents/${documentId}/history`, { method: "GET" });
}

export async function deleteDocument(documentId) {
  const response = await fetch(`${apiBaseUrl}/documents/${documentId}`, {
    method: "DELETE",
    headers: {
      ...getAuthHeaders(),
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      notifySessionExpired();
    }

    let errorMessage = "Unable to delete the document.";

    try {
      const errorBody = await response.json();
      errorMessage = errorBody.message || errorMessage;
    } catch {
      errorMessage = `Request failed with status ${response.status}`;
    }

    throw new Error(errorMessage);
  }
}
