# Local database (Docker Compose)

- Start: `docker compose up -d`
- Stop: `docker compose down`
- Wipe data: `docker compose down -v`

The database listens on `127.0.0.1:5432` only — never exposed beyond localhost.
Credentials come from `.env` (copy `.env.example` first); the database is plain
PostgreSQL only (ADR-009).
