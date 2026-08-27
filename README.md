# Small Business Platform (SBP)

SBP is a reusable foundation for assembling customer-specific small-business applications. It is a modular-monolith framework, not a finished generic SaaS product.

The current reference implementation proves that an independent customer application can consume reusable backend JARs, add customer-owned behavior and persistence, and run with PostgreSQL through a portable Compose environment.

## Current implementation

Implemented:

- `sbp-framework`: ordinary reusable Java JAR;
- `sbp-module-customer`: reusable Customer JAR with create, retrieve, and list operations;
- `sbp-module-contact`: reusable Contact JAR for people associated with Customers;
- `sbp-module-user-maintenance`: reusable User JAR with create, search, detail, edit, and activate/deactivate lifecycle administration, plus Role and Group maintenance, their User assignments, group-level role inheritance (UM-FR-11) with effective-role resolution, an audit log, encryption-at-rest/masking of personal data with keyed blind-index search, and a read-only permission catalog;
- `sbp-module-job`: reusable Job / Work Order JAR with create, paginated search/filter, detail, edit, a small OPEN/IN_PROGRESS/COMPLETED/CANCELLED lifecycle with explicit transition actions, Customer ownership validated through the public Customer contract, a small priority triage, a single planned-date touchpoint, and framework-hook audit recording (JOB_CREATED / JOB_UPDATED / JOB_STATUS_CHANGED);
- `sbp-module-document`: reusable Document JAR with upload, paginated search/filter by owner, detail, download, content replacement, rename, and explicit audited delete; exactly-one-owner association (Customer or Job) validated through the public contracts; content stored only through the framework FR-11 storage abstraction with a reference database-backed implementation; framework-hook audit recording (DOCUMENT_CREATED / DOCUMENT_METADATA_UPDATED / DOCUMENT_CONTENT_REPLACED / DOCUMENT_DELETED) with a per-Document read view that survives deletion;
- Acme reference backend: executable Spring Boot application;
- Acme Customer profile extension with customer-owned persistence;
- reusable SBP frontend baseline with Customer list, create, and detail flows;
- independent Acme frontend with customer-owned branding and service profile editing;
- PostgreSQL migrations for Customer, Acme profile, and Contact data;
- Docker-compatible and Podman-oriented full-stack local deployment.
- customer-owned development/demo authentication with an Acme sign-in and sign-out experience;
- customer-owned self-service "My Profile" mapped from the authenticated identity to an SBP User.

Not implemented yet:

- Job scheduling, assignment, and notes (deferred by design per ADR-JOB-002);
- production security policy or cloud deployment.

## Repository structure

```text
backend/sbp/
├── sbp-framework/
├── sbp-module-customer/
├── sbp-module-contact/
└── sbp-module-user-maintenance/

frontend/sbp-ui/              reusable Next.js baseline/template

reference/acme/
├── backend/                  executable backend and Acme extension
├── frontend/                 independent customer-owned Next.js application
└── deployment/               Compose environment and run guide

doc/
├── architecture/
└── IMPLEMENTATION-PLAN-v0.1.md
```

The Acme application is the deployable composition root. Reusable SBP JARs are not separate services.

## Prerequisites

- Java 21 for local backend builds;
- Podman with a Compose provider or Docker with Docker Compose;
- Node.js 20.9 or later for either frontend project.

## Run with containers

From the repository root, create the untracked environment file:

```bash
cp reference/acme/deployment/.env.example reference/acme/deployment/.env
```

Review the development-only values in `.env`, then start with either runtime:

```bash
podman compose \
  -f reference/acme/deployment/compose.yaml \
  up --build -d
```

or:

```bash
docker compose \
  -f reference/acme/deployment/compose.yaml \
  up --build -d
```

You can alternatively run Compose from its own directory:

```bash
cd reference/acme/deployment
cp .env.example .env
docker compose up --build -d
```

From the repository root, inspect the running services and backend logs with:

```bash
docker compose -f reference/acme/deployment/compose.yaml ps
docker compose -f reference/acme/deployment/compose.yaml logs -f backend
```

The frontend is available at `http://localhost:3000`, the backend at `http://localhost:8080`, and PostgreSQL remains internal to the Compose network. Flyway applies:

```text
V1__create_sbp_customer.sql
V2__create_acme_customer_profile.sql
V3__create_sbp_contact.sql
V4__create_sbp_user.sql
V5__create_sbp_role_and_user_role.sql
V6__create_sbp_group_and_user_group.sql
V7__create_sbp_user_audit_event.sql
V8__widen_sbp_user_pii_columns.sql
V9__add_sbp_user_search_tokens.sql
V10__create_sbp_permission_and_role_permission.sql
V11__create_acme_user_credential.sql
V12__add_acme_password_change_required.sql
V13__create_sbp_group_role.sql
V14__create_sbp_job.sql
V15__create_sbp_job_audit_event.sql
V16__create_sbp_document.sql
V17__create_sbp_document_content.sql
V18__create_sbp_document_audit_event.sql
```

Open `http://localhost:3000/signin` and use the development/demo credentials
configured by `ACME_SECURITY_USERNAME` and `ACME_SECURITY_PASSWORD` in the
untracked `.env` file. If those variables are not overridden, the Compose
defaults are:

```text
Username: acme
Password: acme-local-development
```

Values in `reference/acme/deployment/.env` take precedence. These credentials
are for local development only and are not a production identity design. The
reference frontend keeps the resulting HTTP Basic authorization value in the
current browser tab's `sessionStorage`; signing out or closing the tab removes
that tab-scoped state. This deliberately simple mechanism is not production-safe.

Detailed API, restart, persistence, shutdown, and reset instructions are in the [Acme deployment guide](reference/acme/deployment/README.md).

For faster frontend development, the Acme frontend can also run separately while the backend environment is running:

```bash
cd reference/acme/frontend
npm install
npm run dev
```

Open `http://localhost:3000`. Next.js proxies same-origin `/api/...` browser calls to `SBP_API_URL`, which defaults to `http://localhost:8080`. See the [Acme frontend guide](reference/acme/frontend/README.md) for the browser proof and configuration details.

## Build and test locally

Backend artifact and package-boundary details are documented in the [SBP backend guide](backend/sbp/README.md).

Build and install the reusable artifacts:

```bash
./backend/sbp/mvnw -f backend/sbp/pom.xml clean install
```

Build and test the Acme reference backend:

```bash
./backend/sbp/mvnw -f reference/acme/backend/pom.xml clean verify
```

The PostgreSQL integration tests use Testcontainers and therefore require an available Docker-compatible container runtime.

Verify the reusable frontend baseline:

```bash
cd frontend/sbp-ui
npm install
npm run typecheck
npm run lint
npm run build
```

The same verification commands apply in `reference/acme/frontend`.

## API overview

Generic Customer capability:

```text
POST /api/customers
GET  /api/customers
GET  /api/customers/{customerId}
```

Generic Contact capability:

```text
POST /api/contacts
GET  /api/contacts/{contactId}
GET  /api/customers/{customerId}/contacts
```

Generic User Maintenance capability:

```text
POST   /api/users
POST   /api/users/bulk
GET    /api/users?query=&status=&roleId=&groupId=
GET    /api/users/export
GET    /api/users/{userId}
PUT    /api/users/{userId}
POST   /api/users/{userId}/activate
POST   /api/users/{userId}/deactivate

POST   /api/roles
GET    /api/roles
GET    /api/roles/{roleId}
PUT    /api/roles/{roleId}
POST   /api/roles/{roleId}/retire

GET    /api/users/{userId}/roles
POST   /api/users/{userId}/roles/{roleId}
DELETE /api/users/{userId}/roles/{roleId}
GET    /api/users/{userId}/effective-roles

POST   /api/groups
GET    /api/groups
GET    /api/groups/{groupId}
PUT    /api/groups/{groupId}
POST   /api/groups/{groupId}/retire
GET    /api/groups/{groupId}/members

GET    /api/groups/{groupId}/roles
POST   /api/groups/{groupId}/roles/{roleId}
DELETE /api/groups/{groupId}/roles/{roleId}

GET    /api/users/{userId}/groups
POST   /api/users/{userId}/groups/{groupId}
DELETE /api/users/{userId}/groups/{groupId}

GET    /api/audit?actor=&subjectType=&subjectId=&action=&from=&to=&limit=

GET    /api/permissions
GET    /api/roles/{roleId}/permissions
GET    /api/permissions/{permissionKey}/roles
```

Generic Job / Work Order capability:

```text
POST   /api/jobs
GET    /api/jobs?query=&customerId=&status=&priority=&page=&size=
GET    /api/jobs/{jobId}
PUT    /api/jobs/{jobId}
POST   /api/jobs/{jobId}/start
POST   /api/jobs/{jobId}/complete
POST   /api/jobs/{jobId}/cancel
GET    /api/jobs/{jobId}/audit-events?action=
GET    /api/customers/{customerId}/jobs
```

Generic Document capability:

```text
POST   /api/documents                        multipart: file, ownerType (CUSTOMER|JOB), ownerId, displayName?
GET    /api/documents?query=&ownerType=&ownerId=&page=&size=
GET    /api/documents/{documentId}
PUT    /api/documents/{documentId}           rename: {"displayName": "..."}
PUT    /api/documents/{documentId}/content   multipart: file (replace content in place)
DELETE /api/documents/{documentId}
GET    /api/documents/{documentId}/content   attachment download
GET    /api/documents/{documentId}/audit-events?action=
```

Customer-owned Acme extension:

```text
PUT  /api/acme/customers/{customerId}/profile
GET  /api/acme/customers/{customerId}
```

Acme development/demo authentication check:

```text
GET  /api/auth/session
```

Acme self-service profile (mapped from the authenticated identity):

```text
GET  /api/me
PUT  /api/me
POST /api/me/password
```

Acme-owned local credential administration:

```text
GET   /api/acme/users/{userId}/credential
PUT   /api/acme/users/{userId}/password
POST  /api/acme/users/{userId}/unlock
```

The Acme combined view is composed through the public Customer API and stable Customer identifier. Acme does not access Customer persistence internals or add columns to `sbp_customer`.

## Architecture boundaries

- Backend reuse uses independently versioned Maven artifacts.
- Customer applications own the executable Spring Boot entry point.
- Customer applications own their Spring Security configuration and authorization policy.
- Customer-specific data remains in customer-owned tables and packages.
- Modules collaborate synchronously through explicit public contracts and identifiers.
- The deployment remains one application process and one PostgreSQL database.
- Frontend reuse follows a copy-and-customize template model rather than Java-style artifact reuse.

## Authoritative documentation

- [Product requirements](doc/SBP_PRD_Template_v0_4_Baseline.docx)
- [Technical Architecture v0.1](doc/architecture/TECHNICAL-ARCHITECTURE-v0.1.md)
- [Implementation Plan v0.1](doc/IMPLEMENTATION-PLAN-v0.1.md)
- [ADR-002: Spring module assembly](doc/architecture/adr/ADR-002-spring-module-assembly.md)
- [ADR-003: Persistence and Flyway ownership](doc/architecture/adr/ADR-003-persistence-and-flyway.md)
- [ADR-004: First proof module and reference scenario](doc/architecture/adr/ADR-004-reference-scenario.md)
- [ADR-005: Reference security boundary](doc/architecture/adr/ADR-005-reference-security-boundary.md)
- [ADR-006: Contact module and migration ordering](doc/architecture/adr/ADR-006-contact-module-and-migration-ordering.md)
- [ADR-UM-001: User Maintenance module and package topology](doc/architecture/adr/ADR-UM-001-user-maintenance-module-and-package-topology.md)
- [ADR-UM-002: Identity mapping and self-service](doc/architecture/adr/ADR-UM-002-identity-mapping-and-self-service.md)
- [ADR-UM-003: Audit hook contract](doc/architecture/adr/ADR-UM-003-audit-hook-contract.md)
- [ADR-UM-004: PII encryption at rest and key management](doc/architecture/adr/ADR-UM-004-pii-encryption-and-key-management.md)
- [ADR-UM-005: Group role inheritance (implemented)](doc/architecture/adr/ADR-UM-005-group-role-inheritance.md)
- [ADR-UM-006: Local credential management and session boundary](doc/architecture/adr/ADR-UM-006-local-credential-management-and-session-boundary.md)
- [ADR-JOB-001: Job module boundary, terminology, and dependencies](doc/architecture/adr/ADR-JOB-001-job-module-boundary-and-dependencies.md)
- [ADR-JOB-002: Job domain model and lifecycle boundary](doc/architecture/adr/ADR-JOB-002-job-domain-model-and-lifecycle.md)
- [ADR-JOB-003: Job audit ownership and framework boundary](doc/architecture/adr/ADR-JOB-003-job-audit-ownership-and-framework-boundary.md)
- [ADR-DOC-001: Document module boundary, terminology, and dependencies](doc/architecture/adr/ADR-DOC-001-document-module-boundary-and-dependencies.md)
- [ADR-DOC-002: Storage abstraction (FR-11) and reference implementation](doc/architecture/adr/ADR-DOC-002-document-storage-abstraction-and-reference-implementation.md)
- [ADR-DOC-003: Document domain model, lifecycle, replacement, and deletion](doc/architecture/adr/ADR-DOC-003-document-domain-model-lifecycle-and-deletion.md)
- [ADR-DOC-004: Document audit ownership and the third-consumer trigger](doc/architecture/adr/ADR-DOC-004-document-audit-ownership-and-third-consumer-trigger.md)
