import { render, screen } from "@testing-library/react";
import App from "./App";

jest.mock("./components/AuthScreen", () => () => <div>Auth screen</div>);
jest.mock("./services/authService", () => ({
  getStoredSession: jest.fn(() => null),
  getSessionExpiredEventName: jest.fn(() => "collab-session-expired"),
  saveSession: jest.fn(),
  clearSession: jest.fn(),
}));

test("renders the auth screen when no session exists", () => {
  render(<App />);
  expect(screen.getByText(/auth screen/i)).toBeInTheDocument();
});
