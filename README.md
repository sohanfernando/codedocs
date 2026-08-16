# codedocs

[![CI](https://github.com/sohanfernando/codedocs/actions/workflows/ci.yml/badge.svg)](https://github.com/sohanfernando/codedocs/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sohanfernando_codedocs&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sohanfernando_codedocs)

Ask questions about any GitHub repository and get answers grounded in its actual code, with citations back to the exact files and lines they came from.

Point it at a repo, it clones and indexes the codebase (chunked + embedded into pgvector), and you can then chat with it — the assistant answers using retrieval over your code rather than general knowledge, and every claim links back to its source chunk.

## Stack

**Backend** — `backend/`
- Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Actuator)
- PostgreSQL + [pgvector](https://github.com/pgvector/pgvector) for embedding storage/similarity search, via Hibernate's vector support
- Flyway for schema migrations
- [JGit](https://www.eclipse.org/jgit/) for cloning/syncing repositories
- Google Gemini for embeddings (`gemini-embedding-001`) and chat completion (`gemini-3.6-flash`)
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

Backend — set in `backend/.env` (see `backend/.env.example`):

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | Postgres password (matches `docker-compose.yml`) |
| `GEMINI_API_KEY` | Google Gemini API key, used for embeddings and chat |
| `CORS_ORIGINS` | Allowed origins for the frontend (defaults to `http://localhost:5173`) |
| `SENTRY_DSN` | Optional. Unset = error tracking disabled (the default locally) |
| `SENTRY_ENVIRONMENT` | Optional, defaults to `local`. Set to `production`/`staging` etc. on deploy |
| `SENTRY_TRACES_SAMPLE_RATE` | Optional, defaults to `0.1` |

Frontend — set in `frontend/.env` (see `frontend/.env.example`):

| Variable | Purpose |
|---|---|
| `VITE_SENTRY_DSN` | Optional. Unset = error tracking disabled (the default locally) |

## Monitoring

- **Error tracking** ([Sentry](https://sentry.io)) — wired into both sides but inert unless `SENTRY_DSN` / `VITE_SENTRY_DSN` are set, so it costs nothing locally. Backend: unexpected exceptions (`GlobalExceptionHandler`'s catch-all and `AiProviderException`) are reported with the same `errorId` returned to the caller, so a support conversation ("reference: xyz") jumps straight to the matching event. Frontend: uncaught errors and a top-level `ErrorBoundary` around the whole app report render crashes instead of leaving a blank tab.

  > The Spring Boot starter (`sentry-spring-boot-starter-jakarta`) isn't used — as of Sentry 8.53.0 it still reflects on a class Spring Boot 4 moved, which throws `NoClassDefFoundError` during context startup. The plain SDK is initialized by hand in `CodedocsApplication` instead.

- **Stuck ingestion detection** (`StuckIngestionMonitor`) — a scheduled check (every 10 minutes) that flags any repo still in a non-terminal `RepoStatus` (`PENDING`/`CLONING`/`CHUNKING`/`EMBEDDING`) longer than `ingestion.stuck-after` (default 30m) — a crashed worker or a hung Gemini call can otherwise leave a repo "Indexing…" forever with nothing surfacing it. Logs a warning and reports to Sentry; it never mutates repo state itself.

- **Health check** — `GET /actuator/health` (unauthenticated) is the endpoint to point an uptime checker at once this is deployed somewhere.

## CI/CD & code quality

Every push to `main` and every PR runs [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

1. **Backend** — `mvn verify`: compiles, runs the Testcontainers-backed test suite (spins up a real pgvector-enabled Postgres), and generates a JaCoCo coverage report.
2. **Frontend** — `npm ci`, `eslint`, `vite build` (which runs the TypeScript compiler as part of the build).
3. **[SonarQube Cloud](https://sonarcloud.io/summary/new_code?id=sohanfernando_codedocs)** scan — one project covering both halves of the monorepo (config in [`sonar-project.properties`](sonar-project.properties)), reporting security/reliability/maintainability issues and the JaCoCo coverage from step 1.

This is CI-based analysis, not SonarQube Cloud's zero-config "Automatic Analysis" mode — that alternative can't run your test suite, so it can never report real coverage, and it gives no visibility into scanner logs if something goes wrong.
