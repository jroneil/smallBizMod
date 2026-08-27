# Acme reference deployment

This local/demo deployment runs the complete Acme reference application: Next.js frontend, Spring Boot backend, and PostgreSQL.

## Prerequisites

- Podman with a Compose provider, or Docker with Docker Compose;
- ports configured by `.env` available locally.

## Configure

From this directory, create the untracked local environment file:

```bash
cp .env.example .env
```

The checked-in values are development examples only. Replace the passwords in `.env` as appropriate; do not commit real credentials. The `ACME_SECURITY_*` values configure Acme's development/demo HTTP Basic user.

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

The frontend is available at `http://localhost:${ACME_FRONTEND_PORT:-3000}` and the backend at `http://localhost:${ACME_BACKEND_PORT:-8080}`. The frontend proxies `/api/...` requests to the Compose service name `backend`; Spring Boot connects to the service name `postgres`. Flyway applies the SBP Customer V1, Acme profile V2, SBP Contact V3, and SBP User Maintenance V4/V5/V6 migrations during application startup.

Open the frontend and sign in through the Acme form with the HTTP Basic credentials from the untracked `.env` file. The frontend retains the authorization value only in the current tab's `sessionStorage`, adds it to same-origin API requests, and clears it on sign-out or a backend 401. This reference policy is intentionally minimal and must be replaced for a real customer deployment.

The frontend backend target is supplied while its image is built. Rebuild the frontend image if that internal target changes.

## Exercise the API

Load the untracked development values into the current shell before using the
commands below:

```bash
set -a
. ./.env
set +a
```

Create a generic Customer:

```bash
customer=$(curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST http://localhost:${ACME_BACKEND_PORT:-8080}/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme Demo Customer"}')

customer_id=$(printf '%s' "$customer" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
printf '%s\n' "$customer"
```

Add Acme-specific profile data:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id/profile" \
  -H 'Content-Type: application/json' \
  -d '{"serviceRegion":"Coastal","accountNotes":"Gate code is on file"}'
```

Add a reusable Contact associated with the Customer:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/contacts" \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$customer_id\",\"fullName\":\"Jordan Lee\",\"email\":\"jordan@example.com\",\"phone\":\"555-0104\",\"roleTitle\":\"Operations Manager\"}"
```

Create a reusable User and exercise its lifecycle:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users" \
  -H 'Content-Type: application/json' \
  -d '{"username":"acme.admin","email":"admin@acme.example","givenName":"Acme","familyName":"Admin","phone":"555-0100"}'

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users?query=admin"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users/<user-id>/deactivate"
```

Create a Role and assign it to a User:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/roles" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Manager","description":"Oversees daily work"}'

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users/<user-id>/roles/<role-id>"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users/<user-id>/roles"
```

Create a Group and assign a User to it:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/groups" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Field Crew","description":"Field service team"}'

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users/<user-id>/groups/<group-id>"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/groups/<group-id>/members"
```

Retrieve the combined view:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id"
```

## Verify persistence across a backend restart

Restart only the backend, leaving PostgreSQL and its named volume in place:

```bash
podman compose restart backend
# or: docker compose restart backend
```

After the backend is ready, repeat the combined-view GET using the same `customer_id`. The data remains in the `acme-postgres-data` named volume.

## Existing-volume migration warning

If Flyway reports that the database version is newer than the latest migration
in the checked-out source, the named volume was previously used by a different
code revision. This is local state, not a reason to recreate or renumber current
migrations. Preserve the volume if its data matters, or use the explicit reset
command below only when discarding that local data is acceptable.

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
