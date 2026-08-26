# Acme reference deployment

This local/demo deployment runs the full Acme reference stack: PostgreSQL, the Acme Spring Boot backend, and the Acme Next.js frontend.

## Prerequisites

- Podman with a Compose provider, or Docker with Docker Compose;
- ports configured by `.env` available locally.

## Configure

From this directory, create the untracked local environment file:

```bash
cp .env.example .env
```

The checked-in values are development examples only. Replace the passwords in `.env` as appropriate; do not commit real credentials.

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

Open the application at `http://localhost:${ACME_FRONTEND_PORT:-3000}` and sign in with `ACME_DEMO_USER` / `ACME_DEMO_PASSWORD` from `.env` (defaults `demo` / `demo-local-development`).

The backend is available directly at `http://localhost:${ACME_BACKEND_PORT:-8080}`. Spring Boot connects to PostgreSQL through the Compose service name `postgres`, and Flyway applies the SBP Customer, Acme extension, and SBP Contact migrations during startup:

```text
V1__create_sbp_customer.sql
V2__create_acme_customer_profile.sql
V3__create_sbp_contact.sql
```

## Exercise the API

All `/api/**` endpoints require HTTP Basic authentication. Use the demo credentials:

```bash
auth="demo:demo-local-development"
```

Create a generic Customer:

```bash
customer=$(curl -fsS \
  -u "$auth" \
  -X POST http://localhost:${ACME_BACKEND_PORT:-8080}/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme Demo Customer"}')

customer_id=$(printf '%s' "$customer" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
printf '%s\n' "$customer"
```

Add Acme-specific profile data:

```bash
curl -fsS \
  -u "$auth" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id/profile" \
  -H 'Content-Type: application/json' \
  -d '{"serviceRegion":"Coastal","accountNotes":"Gate code is on file"}'
```

Add a generic Contact for the customer:

```bash
curl -fsS \
  -u "$auth" \
  -X POST http://localhost:${ACME_BACKEND_PORT:-8080}/api/contacts \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$customer_id\",\"fullName\":\"Casey Nolan\",\"role\":\"Property Manager\",\"email\":\"casey@example.com\",\"phone\":\"555-0100\"}"
```

List the customer's contacts and retrieve the combined view:

```bash
curl -fsS \
  -u "$auth" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/contacts?customerId=$customer_id"

curl -fsS \
  -u "$auth" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/customers/$customer_id"
```

## Verify persistence across a backend restart

Restart only the backend, leaving PostgreSQL and its named volume in place:

```bash
podman compose restart backend
# or: docker compose restart backend
```

After the backend is ready, repeat the combined-view GET using the same `customer_id`. The data remains in the `acme-postgres-data` named volume.

## End-to-end browser check

With the stack running, a headless-Chrome check verifies the full sign-in → create customer → add contact → save profile → reload → sign-out flow:

```bash
cd reference/acme/e2e
npm install
npm test
```

See [reference/acme/e2e/README.md](../e2e/README.md) for prerequisites and behavior.

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
