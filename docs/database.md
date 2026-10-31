# Local database (Docker Compose)

- Start: `docker compose up -d`
- Stop: `docker compose down`
- Wipe data: `docker compose down -v`

The database listens on `127.0.0.1:5432` only — never exposed beyond localhost.
Credentials come from `.env` (copy `.env.example` first); the database is plain
PostgreSQL only (ADR-009).

## Epic 1 schema verification

Boot the app once with the dev profile — Flyway applies `V3`–`V9` and
`ddl-auto=validate` must come up clean:

```
JWT_SECRET=<dev secret> mvn -pl typeshift-backend spring-boot:run \
    -Dspring-boot.run.profiles=dev
```

To check the migrations from scratch without wiping the dev data, verify against
a throwaway database instead of `docker compose down -v`:

```
docker compose exec db psql -U typeshift -d typeshift -c "CREATE DATABASE epic1_verify"
JWT_SECRET=<dev secret> mvn -pl typeshift-backend spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/epic1_verify"
docker compose exec db psql -U typeshift -d typeshift -c "DROP DATABASE epic1_verify"
```
