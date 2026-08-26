# Acme reference deployment

This local/demo deployment currently runs the Acme Spring Boot backend and PostgreSQL. There is no Acme reference frontend yet, so no frontend container is defined.

## Prerequisites

- Podman with a Compose provider, or Docker with Docker Compose;
- ports configured by `.env` available locally.

## Configure

From this directory, create the untracked local environment file:

```bash
cp .env.example .env
```

The checked-in values are development examples only. Replace the password in `.env` as appropriate; do not commit real credentials.

## Start

Podman:

```bash
podman compose up --build -d
podman compose logs -f backend
```

Docker-compatible alternative:

```bash
docker compose up --build -d
docker compose logs -f backend
```

The backend is available at `http://localhost:${ACME_BACKEND_PORT:-8080}`. Spring Boot connects to PostgreSQL through the Compose service name `postgres`, and Flyway applies both the SBP Customer and Acme migrations during application startup.

## Exercise the API

Create a generic Customer:

```bash
customer=$(curl -fsS \
  -X POST http://localhost:${ACME_BACKEND_PORT:-8080}/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme Demo Customer"}')

customer_id=$(printf '%s' "$customer" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
printf '%s\n' "$customer"
```

Add Acme-specific profile data:

```bash
curl -fsS \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id/profile" \
  -H 'Content-Type: application/json' \
  -d '{"serviceRegion":"Coastal","accountNotes":"Gate code is on file"}'
```

Retrieve the combined view:

```bash
curl -fsS \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id"
```

## Verify persistence across a backend restart

Restart only the backend, leaving PostgreSQL and its named volume in place:

```bash
podman compose restart backend
# or: docker compose restart backend
```

After the backend is ready, repeat the combined-view GET using the same `customer_id`. The data remains in the `acme-postgres-data` named volume.

## Stop or reset

Stop containers while preserving data:

```bash
podman compose down
# or: docker compose down
```

Explicitly remove the local database volume and all demo data:

```bash
podman compose down --volumes
# or: docker compose down --volumes
```
