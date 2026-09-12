const apiBaseUrl = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";
const sessionStorageKey = "collab_auth_session";
const sessionExpiredEventName = "collab-session-expired";

export function notifySessionExpired() {
  clearSession();
  window.dispatchEvent(new CustomEvent(sessionExpiredEventName));
}

async function parseJsonResponse(response, { expireSessionOnUnauthorized = false } = {}) {
  if (!response.ok) {
    if (response.status === 401 && expireSessionOnUnauthorized) {
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

async function postJson(path, payload) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return parseJsonResponse(response);
}

export function getStoredSession() {
  const rawSession = localStorage.getItem(sessionStorageKey);

  if (!rawSession) {
    return null;
  }

  try {
    const session = JSON.parse(rawSession);
    return session?.token ? session : null;
  } catch {
    clearSession();
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(sessionStorageKey, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(sessionStorageKey);
}

export function getSessionExpiredEventName() {
  return sessionExpiredEventName;
}

export function getAuthHeaders() {
  const session = getStoredSession();

  if (!session?.token) {
    return {};
  }

  return {
    Authorization: `Bearer ${session.token}`,
  };
}

export function login(payload) {
  return postJson("/auth/login", payload);
}

export function register(payload) {
  return postJson("/auth/register", payload);
}
