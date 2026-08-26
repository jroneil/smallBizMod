# Small Business Platform (SBP)

SBP is a reusable foundation for assembling customer-specific small-business applications. It is a modular-monolith framework, not a finished generic SaaS product.

The current reference implementation proves that an independent customer application can consume reusable backend JARs, add customer-owned behavior and persistence, and run with PostgreSQL through a portable Compose environment.

## Current implementation

Implemented:

- `sbp-framework`: ordinary reusable Java JAR;
- `sbp-module-customer`: reusable Customer JAR with create, retrieve, and list operations;
- Acme reference backend: executable Spring Boot application;
- Acme Customer profile extension with customer-owned persistence;
- PostgreSQL migrations for both generic Customer and Acme profile data;
- Docker-compatible and Podman-oriented local deployment.

Not implemented yet:

- Acme reference frontend;
- Contact, Job, or Document modules;
- production security policy or cloud deployment.

## Repository structure

```text
backend/sbp/
├── sbp-framework/
└── sbp-module-customer/

frontend/sbp-ui/              Next.js baseline/template

reference/acme/
├── backend/                  executable backend and Acme extension
└── deployment/               Compose environment and run guide

doc/
├── architecture/
└── IMPLEMENTATION-PLAN-v0.1.md
```

The Acme application is the deployable composition root. Reusable SBP JARs are not separate services.

## Prerequisites

- Java 21 for local backend builds;
- Podman with a Compose provider or Docker with Docker Compose;
- Node.js only when working with the frontend baseline.

## Run with containers

From the repository root:

```bash
cd reference/acme/deployment
cp .env.example .env
```

Review the development-only values in `.env`, then start with either runtime:

```bash
podman compose up --build -d
```

or:

```bash
docker compose up --build -d
```

The backend is available at `http://localhost:8080`. PostgreSQL remains internal to the Compose network, and Flyway applies:

```text
V1__create_sbp_customer.sql
V2__create_acme_customer_profile.sql
```

Detailed API, restart, persistence, shutdown, and reset instructions are in the [Acme deployment guide](reference/acme/deployment/README.md).

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
