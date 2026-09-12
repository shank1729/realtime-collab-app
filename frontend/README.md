# Realtime Collab Frontend

React frontend for a JWT-protected realtime document collaboration app.

## Features

- Sign in and register through the backend auth API.
- Store the current JWT session in local storage.
- Load, create, share, delete, and edit documents through authenticated API calls.
- Connect to the document WebSocket after sign-in.
- Display document access roles for owners, editors, and viewers.
- Keep viewer access read-only in the UI while the backend remains responsible for security.
- Show active document presence, including who is viewing and who is typing.

## Backend Contract

The frontend expects the backend at `REACT_APP_API_BASE_URL`, defaulting to `http://localhost:8080`.

Shared documents return collaborators shaped like:

```json
{
  "id": 1,
  "name": "Teammate",
  "email": "teammate@example.com",
  "role": "EDITOR"
}
```

Sharing a document sends:

```json
{
  "email": "teammate@example.com",
  "role": "VIEWER"
}
```

Supported collaborator roles are `EDITOR` and `VIEWER`.

Document presence is delivered over STOMP after the authenticated WebSocket connects:

- Subscribe to `/topic/documents/{documentId}` for document updates.
- Subscribe to `/topic/documents/{documentId}/presence` for active-user presence.
- Send joins to `/app/documents/{documentId}/presence/join`.
- Send editable-user typing updates to `/app/documents/{documentId}/presence/typing`.
- Send viewing updates to `/app/documents/{documentId}/presence/viewing`.

Presence messages look like:

```json
{
  "documentId": 1,
  "activeUsers": [
    {
      "userId": 2,
      "name": "Teammate",
      "email": "teammate@example.com",
      "status": "TYPING",
      "joinedAt": "2026-04-29T10:00:00Z",
      "lastSeenAt": "2026-04-29T10:01:00Z"
    }
  ],
  "updatedAt": "2026-04-29T10:01:00Z"
}
```

Supported presence statuses are `VIEWING` and `TYPING`.

## Environment

Create `.env` in this folder when you need non-default backend URLs:

```env
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_WS_URL=http://localhost:8080/ws
```

## Available Scripts

### `npm start`

Runs the app in development mode. Open [http://localhost:3000](http://localhost:3000).

### `npm test -- --watchAll=false --runInBand`

Runs the test suite once in a single process.

### `npm run build`

Builds the production bundle into the `build` folder.
