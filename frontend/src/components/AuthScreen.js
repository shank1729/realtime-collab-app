import React, { useState } from "react";
import { login, register } from "../services/authService";

export default function AuthScreen({ onAuthSuccess, initialMessage = "" }) {
  const [mode, setMode] = useState("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(initialMessage);

  const isLogin = mode === "login";

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const payload = isLogin ? { email, password } : { name, email, password };
      const session = isLogin ? await login(payload) : await register(payload);
      onAuthSuccess(session);
    } catch (submitError) {
      setError(submitError.message || "Authentication failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-card">
        <p className="auth-kicker">Layer 2 authentication</p>
        <h1>{isLogin ? "Sign in to your workspace" : "Create your account"}</h1>
        <p className="auth-copy">
          This project now protects documents behind JWT-based authentication.
        </p>

        {error ? <p className="auth-error">{error}</p> : null}

        <form className="auth-form" onSubmit={handleSubmit}>
          {!isLogin ? (
            <label className="auth-field">
              <span>Name</span>
              <input
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Enter your name"
                required
              />
            </label>
          ) : null}

          <label className="auth-field">
            <span>Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
            />
          </label>

          <label className="auth-field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Minimum 6 characters"
              required
            />
          </label>

          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? "Please wait..." : isLogin ? "Sign in" : "Create account"}
          </button>
        </form>

        <button
          className="auth-toggle"
          type="button"
          onClick={() => {
            setError("");
            setMode(isLogin ? "register" : "login");
          }}
        >
          {isLogin
            ? "Need an account? Register"
            : "Already have an account? Sign in"}
        </button>
      </section>
    </main>
  );
}
