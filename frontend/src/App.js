import { useEffect, useState } from "react";
import "./App.css";
import AuthScreen from "./components/AuthScreen";
import DocumentEditor from "./components/DocumentEditor";
import { WebSocketProvider } from "./context/WebSocketContext";
import {
  clearSession,
  getSessionExpiredEventName,
  getStoredSession,
  saveSession,
} from "./services/authService";

function App() {
  const [session, setSession] = useState(() => getStoredSession());
  const [authMessage, setAuthMessage] = useState("");

  const handleAuthSuccess = (nextSession) => {
    saveSession(nextSession);
    setSession(nextSession);
    setAuthMessage("");
  };

  const handleLogout = () => {
    clearSession();
    setSession(null);
    setAuthMessage("");
  };

  useEffect(() => {
    const handleSessionExpired = () => {
      clearSession();
      setSession(null);
      setAuthMessage("Your session expired. Please sign in again.");
    };

    const eventName = getSessionExpiredEventName();
    window.addEventListener(eventName, handleSessionExpired);

    return () => {
      window.removeEventListener(eventName, handleSessionExpired);
    };
  }, []);

  return (
    <div className="app-shell">
      {session ? (
        <WebSocketProvider token={session.token}>
          <DocumentEditor session={session} onLogout={handleLogout} />
        </WebSocketProvider>
      ) : (
        <AuthScreen onAuthSuccess={handleAuthSuccess} initialMessage={authMessage} />
      )}
    </div>
  );
}

export default App;
