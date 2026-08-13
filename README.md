# codedocs

Ask questions about any GitHub repository and get answers grounded in its actual code, with citations back to the exact files and lines they came from.

Point it at a repo, it clones and indexes the codebase (chunked + embedded into pgvector), and you can then chat with it — the assistant answers using retrieval over your code rather than general knowledge, and every claim links back to its source chunk.

## Stack

**Backend** — `backend/`
- Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Actuator)
- PostgreSQL + [pgvector](https://github.com/pgvector/pgvector) for embedding storage/similarity search, via Hibernate's vector support
- Flyway for schema migrations
- [JGit](https://www.eclipse.org/jgit/) for cloning/syncing repositories
- Google Gemini for embeddings (`gemini-embedding-001`) and chat completion (`gemini-2.5-flash`)
- Bucket4j for rate limiting

**Frontend** — `frontend/`
- React 19 + TypeScript, built with Vite
- Tailwind CSS 4
- `react-markdown` + `rehype-highlight` for rendering answers with syntax-highlighted code blocks

## API

All endpoints are under `/api`:

| Area | Routes |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me` |
| Repositories | `GET /repositories`, `POST /repositories`, `GET /repositories/{id}`, `POST /repositories/{id}/retry`, `POST /repositories/{id}/sync`, `DELETE /repositories/{id}` |
| Threads | `GET /threads`, `POST /threads`, `GET /threads/{id}/messages`, `PATCH /threads/{id}`, `DELETE /threads/{id}`, `POST /threads/{id}/share`, `DELETE /threads/{id}/share`, `PUT /threads/{id}/messages/{messageId}/feedback` |
| Chat | `POST /chat`, `POST /chat/stream` (SSE) |
| Shared threads | `GET /shared/{token}` |

## Running locally

### Prerequisites
- Java 21, Maven (or use the bundled `mvnw`)
- Node 20+
- Docker (for the Postgres/pgvector database)
- A [Gemini API key](https://ai.google.dev/)

### Backend

```bash
cd backend
cp .env.example .env   # fill in DB_PASSWORD and GEMINI_API_KEY
docker compose up -d   # starts pgvector-enabled Postgres on :5433
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Flyway applies migrations automatically on boot.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173` and talks to the backend on `:8080`.

## Environment variables

Set in `backend/.env` (see `backend/.env.example`):

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | Postgres password (matches `docker-compose.yml`) |
| `GEMINI_API_KEY` | Google Gemini API key, used for embeddings and chat |
| `CORS_ORIGINS` | Allowed origins for the frontend (defaults to `http://localhost:5173`) |
