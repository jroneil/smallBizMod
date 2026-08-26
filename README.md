# Small Business Platform (SBP)

SBP is a reusable foundation for assembling customer-specific small-business applications. It is a modular-monolith framework, not a finished generic SaaS product.

The current reference implementation proves that an independent customer application can consume reusable backend JARs, add customer-owned behavior and persistence, and run with PostgreSQL through a portable Compose environment.

## Current implementation

Implemented:

- `sbp-framework`: ordinary reusable Java JAR;
- `sbp-module-customer`: reusable Customer JAR with create, retrieve, and list operations;
- Acme reference backend: executable Spring Boot application;
- Acme Customer profile extension with customer-owned persistence;
- reusable SBP frontend baseline with Customer list, create, and detail flows;
- independent Acme frontend with customer-owned branding and service profile editing;
- PostgreSQL migrations for both generic Customer and Acme profile data;
- Docker-compatible and Podman-oriented local deployment.

Not implemented yet:

- Contact, Job, or Document modules;
- production security policy or cloud deployment.

## Repository structure

```text
backend/sbp/
├── sbp-framework/
└── sbp-module-customer/

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

The backend is available at `http://localhost:8080`. PostgreSQL remains internal to the Compose network, and Flyway applies:

```text
V1__create_sbp_customer.sql
V2__create_acme_customer_profile.sql
```

Detailed API, restart, persistence, shutdown, and reset instructions are in the [Acme deployment guide](reference/acme/deployment/README.md).

The Acme frontend is deliberately not containerized in this slice. With the backend environment running, start it separately:

```bash
cd reference/acme/frontend
npm install
npm run dev
```

Open `http://localhost:3000`. Next.js proxies same-origin `/api/...` browser calls to `SBP_API_URL`, which defaults to `http://localhost:8080`. See the [Acme frontend guide](reference/acme/frontend/README.md) for the browser proof and configuration details.

## Build and test locally

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

Customer-owned Acme extension:

```text
PUT  /api/acme/customers/{customerId}/profile
GET  /api/acme/customers/{customerId}
```

The Acme combined view is composed through the public Customer API and stable Customer identifier. Acme does not access Customer persistence internals or add columns to `sbp_customer`.

## Architecture boundaries

- Backend reuse uses independently versioned Maven artifacts.
- Customer applications own the executable Spring Boot entry point.
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
