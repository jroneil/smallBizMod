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

`SBP_USERMAINTENANCE_ENCRYPTION_KEY` and `SBP_USERMAINTENANCE_SEARCH_KEY` are required and must be **different** Base64-encoded 256-bit (32-byte) keys. Generate them with:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

and set them in the untracked `.env`. They are never committed to source, committed configuration, or stored in the database. The encryption key protects the recoverable personal data; the separate search key produces the non-reversible blind-index search tokens.

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

The frontend is available at `http://localhost:${ACME_FRONTEND_PORT:-3000}` and the backend at `http://localhost:${ACME_BACKEND_PORT:-8080}`. The frontend proxies `/api/...` requests to the Compose service name `backend`; Spring Boot connects to the service name `postgres`. Flyway applies the SBP Customer V1, Acme profile V2, SBP Contact V3, SBP User Maintenance V4–V10, Acme local credential V11–V12, group-role V13, SBP Job V14–V15, and SBP Document V16–V18 migrations during application startup.

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

Assign a Role to a Group (members inherit it) and read effective roles:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/groups/<group-id>/roles/<role-id>"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/groups/<group-id>/roles"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/users/<user-id>/effective-roles"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X DELETE "http://localhost:${ACME_BACKEND_PORT:-8080}/api/groups/<group-id>/roles/<role-id>"
```

`GET /api/users/<user-id>/roles` remains the directly assigned roles; `GET /api/users/<user-id>/effective-roles` is the deduplicated union of direct roles and roles inherited through group membership. Group→Role assignments are audited as `GROUP_ROLE_ASSIGNED` / `GROUP_ROLE_REMOVED`.

Upload a Document to a Customer (multipart) and exercise the reusable Document workflow:

```bash
# Upload a file attached to the Customer (exactly one owner: CUSTOMER or JOB)
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X POST "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents" \
  -F "file=@signed-contract.pdf" \
  -F "ownerType=CUSTOMER" \
  -F "ownerId=$customer_id"

# List the Customer's Documents (paginated baseline contract)
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents?ownerType=CUSTOMER&ownerId=$customer_id&page=0&size=20"

# Download the content (saved under its display name, attachment disposition)
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents/<document-id>/content" \
  -o downloaded-contract.pdf

# Rename, replace content in place, and delete (all audited)
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents/<document-id>" \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"final-contract.pdf"}'

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents/<document-id>/content" \
  -F "file=@final-contract.pdf"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X DELETE "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents/<document-id>"

# The audit history remains readable after deletion (actor is the authenticated user)
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/documents/<document-id>/audit-events"
```

View and edit the authenticated user's own profile (self-service, mapped from the sign-in identity):

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/me"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/me" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@acme.example","givenName":"Acme","familyName":"Administrator","phone":"555-0100"}'
```

Reset a User's local credential (administrator), optionally requiring a change at the next sign-in:

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  -X PUT "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/users/<user-id>/password" \
  -H 'Content-Type: application/json' \
  -d '{"newPassword":"temporary-pass-123","requireChange":true}'
```

Read the credential state (`hasCredential`, `locked`, `failedAttempts`, `passwordChangeRequired`):

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/acme/users/<user-id>/credential"
```

When `requireChange` is true, that identity's ordinary APIs return 403 until the user changes their own password through `POST /api/me/password`; the reference frontend redirects the flagged session to a forced change-password screen. This is enforced server-side regardless of the client, and audit records never contain the password or its hash.

Review the audit log (read-only, with filters):

```bash
curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/audit?action=USER_CREATED"

curl -fsS \
  -u "$ACME_SECURITY_USERNAME:$ACME_SECURITY_PASSWORD" \
  "http://localhost:${ACME_BACKEND_PORT:-8080}/api/audit?actor=$ACME_SECURITY_USERNAME"
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
