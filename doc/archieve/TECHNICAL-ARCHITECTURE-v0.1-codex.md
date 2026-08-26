# Small Business Platform Technical Architecture v0.1

Status: Proposed for review  
Scope: Architecture only; no implementation is authorized by this document  
Product source: `doc/SBP_PRD_Template_v0_4_Baseline.docx`

## 1. Purpose and authority

This document proposes the first technical architecture for the Small Business Platform (SBP). The PRD remains authoritative for product intent. Where the PRD or repository does not provide enough evidence for a decision, this document records an open decision instead of manufacturing a requirement.

SBP is a framework for assembling independently delivered, customer-specific small-business applications. It is not a generic SaaS product. The backend is a modular monolith assembled from versioned Java/Spring artifacts. The frontend is a copied and customized baseline. Customer-specific behavior is expected to live in the customer project first.

This is a boundary proposal, not a commitment to every name or mechanism shown below. The reference implementation is expected to test—and may invalidate—these proposals.

## 2. Current repository baseline

The repository contains three top-level areas:

```text
backend/sbp/       generated Spring Boot application
frontend/sbp-ui/   generated Next.js application
doc/               product documentation
```

Observed facts:

- `backend/sbp` is currently one executable Maven artifact, `com.oneilsoftware:sbp:0.0.1-SNAPSHOT`, using Spring Boot 4.1.1 and Java 21.
- Its generated dependencies include MVC, validation, security, JPA, Flyway, PostgreSQL, Actuator, Batch, Mail, and Testcontainers. Presence in the generated POM is not evidence that every dependency belongs in the future framework or every module.
- The backend has only its application bootstrap, one property, and generated context/Testcontainers tests. It has no business modules, contracts, persistence model, migrations, or module-discovery mechanism.
- `frontend/sbp-ui` is a stock Next.js 16.3.3 / React 19.2.8 / TypeScript / Tailwind 4 application. It has no SBP shell, feature modules, API contracts, or branding mechanism yet.
- No container orchestration, artifact publication, BOM, reference customer application, or architecture enforcement exists.

The generated projects are inputs to future implementation, not architecture precedents that must be preserved internally. They must not be regenerated or redesigned during this architecture task.

## 3. Architecture drivers and constraints

The following are non-negotiable because they come directly from the PRD:

1. One customer application is one independently built and deployed modular monolith by default.
2. Backend reuse occurs through explicitly versioned Java/Spring artifacts.
3. Optional modules are physically selected as build dependencies, not unlocked through runtime commercial switches.
4. Baseline business modules remain deliberately generic.
5. Customer code owns customer policy, integrations, security policy, additional data, APIs, and workflows unless reuse evidence earns upstream promotion.
6. Composition and narrow contracts are preferred to domain-entity inheritance.
7. Each module owns its persistence structures and migrations.
8. Cross-module access uses contracts and identities, not another module's repositories or tables.
9. Distributed infrastructure is not needed to create internal boundaries.
10. Customer applications pin approved versions and upgrade deliberately.
11. Frontend reuse is template-copy reuse and need not mirror backend packaging.
12. The reference application may prove this proposal wrong.

## 4. Proposed system context

For one customer deployment, the intended runtime shape is:

```text
Browser
   |
   v
Customer-owned Next.js application (copied SBP UI baseline)
   |
   | REST/JSON
   v
Customer Spring Boot application (composition root and single process)
   |-- SBP framework JAR
   |-- selected SBP module JARs
   |-- customer-owned extensions/modules
   |
   +--> customer-dedicated PostgreSQL database
   +--> selected object storage implementation, only when required
   +--> customer-selected identity/integration systems
```

The deployable unit is the customer application, not an SBP framework service. Each customer has a separate deployment, configuration, secrets, and database by default.

## 5. Backend project and artifact boundaries

### 5.1 Proposed repository shape

The first implementation should evolve the backend into a Maven multi-module build without replacing the generated Spring Boot project wholesale. The following is a proposed target shape; exact directory and artifact names require ADR approval:

```text
backend/sbp/
  pom.xml                         Maven aggregator/build definition
  sbp-framework-core/             reusable framework JAR
  sbp-module-<capability>/         one reusable JAR per approved module

reference/
  <reference-customer>/
    backend/                      independent executable customer-style build
    frontend/                     copied sbp-ui baseline, then customized
    deployment/                   customer-style local/deployment configuration

frontend/sbp-ui/                  approved copyable UI baseline source
```

Proposed initial artifact roles:

| Artifact | Packaging | Responsibility | Must not own |
|---|---|---|---|
| `sbp-framework-core` | ordinary JAR | Approved cross-cutting contracts and minimal shared infrastructure | Customer policy or baseline business entities |
| `sbp-module-<capability>` | ordinary JAR | One generic business capability, its public Java contract, implementation, persistence, migrations, and REST implementation where approved | Other modules' repositories or customer-specific behavior |
| `<customer>-application` | executable Spring Boot JAR/OCI image | Composition root, selected dependencies, customer configuration, security policy, extensions, and customer APIs | Reusable SBP source copied into the project |
| `sbp-bom` (future) | Maven BOM POM | A tested set of compatible SBP artifact versions | Automatic customer upgrades or replacement of independent module versions |

The existing `backend/sbp` executable is only a baseline scaffold. The reusable artifacts should not contain an application `main` method. The customer/reference application owns the sole Spring Boot entry point and final dependency graph.

An SBP module initially exposes its Java contract and implementation from one JAR, separated by packages. A separate `*-contract` JAR should be introduced only if a real consumer needs the contract without the implementation or if dependency cycles cannot otherwise be avoided. Creating two artifacts per module preemptively would add release and compatibility overhead without evidence.

No bridge/integration artifacts are proposed yet. If two modules later require optional integration, the dependency and ownership must be made explicit and may earn a small integration artifact after the use case exists.

### 5.2 Application assembly

The customer application's Maven dependencies are the authoritative module selection. Adding a module requires adding its pinned artifact and its explicit Spring configuration; removing both must remove the capability while leaving supported remaining capabilities buildable and startable.

The initial preference is explicit application wiring over broad component scanning or opaque classpath discovery. The exact Spring mechanism—configuration imports, auto-configuration metadata, or a narrowly defined alternative—remains open pending an ADR and a reference spike. Spring Modulith may be evaluated as a boundary/testing aid, but adoption is not assumed and must not change the single-process deployment model.

## 6. Java package conventions

Proposed package roots:

```text
com.oneilsoftware.sbp.framework.contract...
com.oneilsoftware.sbp.framework.internal...

com.oneilsoftware.sbp.modules.<module>.contract...
com.oneilsoftware.sbp.modules.<module>.internal.application...
com.oneilsoftware.sbp.modules.<module>.internal.domain...
com.oneilsoftware.sbp.modules.<module>.internal.persistence...
com.oneilsoftware.sbp.modules.<module>.internal.web...

com.oneilsoftware.customers.<customer>.application...
com.oneilsoftware.customers.<customer>.<capability>...
```

Rules:

- Only `contract` packages are supported compile-time integration surfaces for another artifact.
- JPA entities, Spring Data repositories, migration details, controllers, internal services, and implementation-specific exceptions remain under `internal`.
- A public contract should use purpose-specific commands, results, queries, and identity types. It must not expose JPA entities, repositories, persistence query types, or mutable internal aggregates.
- HTTP APIs are external contracts but their Spring controller implementation is not a Java module contract. HTTP request/response schemas should be treated separately from in-process Java contracts.
- Customer code composes contract operations and stable identities. It does not subclass SBP entities or internal services.
- Package names describe the owning capability. A generic `common`, `shared`, or `util` dumping ground is not an approved module.

Java packages alone do not make a secure boundary. Build structure, code review, and boundary tests must enforce the convention. Whether JPMS, Spring Modulith verification, ArchUnit, or another mechanism is appropriate is an implementation decision; no enforcement dependency is selected here.

## 7. Dependency direction and rules

Allowed compile-time direction:

```text
customer application / customer modules
          |                |
          v                v
selected module contracts and implementations
          |
          v
SBP framework contracts/minimal infrastructure
          |
          v
Spring/Java and approved third-party libraries
```

Rules:

- `sbp-framework-core` depends on no business module.
- A business module depends on the framework only for approved framework contracts it actually uses.
- A module may depend on another module only through the owning module's `contract` package and only when the dependency is declared in Maven.
- Circular module dependencies are prohibited. A cycle indicates a misplaced contract, an application-level composition responsibility, or a capability boundary that must be reconsidered.
- The customer application is the composition root and may depend on all selected module contracts and implementations.
- Reusable modules never depend on a customer project.
- Customer-owned modules may depend on SBP public contracts; SBP modules do not depend back on them.
- Optional collaboration cannot be implemented by reaching into another module's repositories, entities, Spring bean names, or database tables.
- Framework abstractions are not created merely to invert a dependency. A contract belongs with the capability that owns the behavior or at the application composition layer.

## 8. Module contract approach

Use the smallest explicit synchronous contract that satisfies a demonstrated collaboration. A typical owning-module contract may expose:

- an opaque/stable identity type;
- commands or operations that preserve the module's invariants;
- queries returning immutable contract projections;
- documented errors and transaction expectations;
- lifecycle facts only where another real consumer requires them.

Contract evolution must be deliberate because customer applications compile against released artifacts. Internal domain models and persistence models may evolve without becoming supported integration surfaces.

The default collaboration is a direct, in-process method call through an owner-defined interface. In-process events are allowed only for a demonstrated one-to-many or decoupling need. They are not a substitute for clear command ownership and do not imply Kafka, a message broker, durable delivery, event sourcing, or eventual consistency.

The exact identifier representation, error model, event publication semantics, and transaction boundary are open until the proof module supplies concrete use cases.

## 9. Customer extension and composition

Customer-specific behavior is implemented in the customer project first in one of three forms defined by the PRD:

1. **Configuration:** branding, labels, endpoints, and narrowly approved behavior choices.
2. **Composition/extension:** customer services, APIs, views, integrations, and workflows that call SBP contracts and associate customer data with SBP identities.
3. **Customer-owned module:** a new capability with its own code, schema, migrations, and contracts.

For example, an Acme-specific customer profile should store the baseline Customer identifier plus Acme-owned fields in an Acme-owned table. An Acme service composes that profile with data obtained through the Customer module contract. It should not add Acme columns to the Customer table, subclass a Customer JPA entity, or read the Customer repository.

No generic custom-field engine, workflow engine, rules engine, plugin SPI, entity-inheritance hierarchy, or dynamic module loader is proposed. Repeated implementations must provide evidence before any such abstraction is considered for promotion.

## 10. Persistence and Flyway ownership

### 10.1 Ownership invariants

- A customer deployment normally uses one PostgreSQL database because SBP is a modular monolith.
- Every reusable module owns its tables, indexes, constraints, persistence mappings, and migration files.
- Customer-owned modules/extensions own separate persistence structures for their additional data.
- Only the owner module accesses its repositories and maps its tables.
- Another module or customer extension stores an owning module's stable identifier and uses its contract for behavior.
- An unrelated module must not alter, backfill, or drop another module's tables in its migrations.
- Durable business history should use explicit lifecycle states where appropriate; cross-module cascading deletion is not the default.

Database credentials may technically permit access across all tables in the single process. Ownership is therefore an application architecture rule enforced by code/package/build tests, not a claim of database-level isolation.

### 10.2 Flyway proposal and remaining decision

Each artifact that owns persistence should package migrations in a unique location such as:

```text
db/migration/sbp/<module>/
db/migration/customer/<customer-module>/
```

Migration files ship with the artifact version that requires them and are immutable after release. The application composition root supplies the assembled set of migration locations. A module must not silently scan or execute another module's migration location.

The exact orchestration is deliberately unresolved. Before persistence implementation, an ADR must choose and prove one of these approaches against independent artifact upgrades and removal:

- one Flyway history with a collision-free, deterministic version convention across all selected modules; or
- one controlled Flyway migration context/history per owning module, with explicit startup ordering only where genuinely required.

The choice must demonstrate clean database creation, incremental upgrades, failure recovery, module omission, and customer-extension migrations. Global numeric ranges assigned to modules and ad hoc migration renaming are not proposed.

Cross-module database foreign keys are not the default proposal because they couple migration and record lifecycles. A concrete integrity requirement may justify one through an ADR; until then, cross-module references are identifiers validated through contracts.

## 11. Cross-module interaction

Use these mechanisms in descending order of preference:

1. A synchronous call to the owning module's public contract.
2. Application-layer composition in the customer application when a use case spans modules.
3. A simple in-process event for a demonstrated notification-style collaboration.

REST calls between modules in the same process, separate databases per module, queues, brokers, service discovery, distributed transactions, and network policy are out of scope. External REST/JSON exists at the customer application boundary, not as ceremony between in-process modules.

Transaction ownership should follow the use case. A module protects its own invariants; a cross-module orchestrator coordinates calls without manipulating repositories. Precise rollback and after-commit event rules remain open until a real cross-module use case exists.

## 12. Versioning and future BOM strategy

- Framework and reusable module JARs receive independent, immutable release versions.
- Customer application POMs pin exact approved versions. Version ranges and automatic adoption of newer platform releases are not proposed.
- Released customer builds do not consume `SNAPSHOT` artifacts.
- An upgrade is a customer-project change with compilation, migration, integration, and reference-behavior testing.
- Compatibility promises apply to documented `contract` packages and external APIs, not `internal` packages.
- A module version owns the migrations needed by that version; downgrades are not assumed safe.

The exact semantic-versioning policy and supported compatibility window require evidence from the first released contract. Until then, version numbers communicate release identity, not an invented guarantee.

After at least the framework, proof module, and reference application have compatible releases, publish an optional `sbp-bom` that records a tested version set. Customers may import the BOM and still override/pin deliberate choices. The BOM coordinates versions; it does not force lockstep releases or update customer applications automatically.

Private artifact repository selection, release promotion, signing, retention, and source-repository topology remain open operational decisions.

## 13. Frontend template strategy

`frontend/sbp-ui` should become the approved baseline that is copied into each new customer project and then owned by that project. It is not proposed as a collection of versioned Java-like runtime modules or as a remotely updated UI dependency.

The baseline may eventually contain:

- a minimal application shell and navigation;
- branding/configuration seams;
- common visual primitives justified by actual screens;
- basic feature folders for implemented baseline modules;
- an API boundary suitable for the selected backend contracts.

When creating a customer project, copy the baseline and only the feature UI needed for selected backend modules. The customer may edit, replace, wrap, or remove copied UI. Record the source baseline version in project documentation so future comparison is possible, but upgrades are manual merges reviewed in the customer project.

No shared npm package, monorepo workspace dependency, UI plugin registry, page generator, schema-driven form engine, or automatic template updater is proposed. A repeated pain point may later justify extracting a small package, but template copying remains the approved v0.1 reuse model.

The eventual folder layout, server/client component split, API client generation, and runtime-versus-build-time branding mechanism remain open until the first functional module UI is built. Browser-delivered configuration must never contain secrets.

## 14. Configuration and secrets boundaries

| Owner | Examples | Boundary |
|---|---|---|
| Framework | infrastructure defaults and framework feature settings | `sbp.framework.*`; no customer policy or secrets in artifacts |
| Reusable module | module-specific technical settings and approved simple behavior choices | `sbp.modules.<module>.*`; documented by the owner module |
| Customer application | branding, selected integrations, identity/security policy, customer behavior, deployment endpoints | customer namespace and deployment configuration |
| Deployment environment | database credentials, signing material, API tokens, provider credentials | injected at runtime; never committed or baked into distributed artifacts |

Reusable artifacts may provide safe defaults but not environment credentials. Customer repositories may contain non-secret example configuration. Real secrets should be injected through environment variables, mounted files, or a customer-selected secret provider. SBP v0.1 does not select a universal secrets manager.

Configuration must not become a hidden programming language or a way to ship all modules behind switches. Build dependencies select capabilities; configuration adjusts only approved behavior of selected capabilities.

The framework may define integration contracts for identity, audit, and storage, but the customer application owns concrete providers and security policy. Exact property names, validation rules, precedence, and refresh behavior should be decided alongside the first implementation rather than predesigned here.

## 15. Local Podman/Docker development model

The proposed minimum local topology is:

- backend and frontend run as normal developer processes for fast feedback;
- PostgreSQL runs in one OCI-compatible development container;
- a customer-style compose file may run the complete frontend, backend, and database when integration testing needs parity;
- optional storage or identity containers are added only when the selected proof use case requires them.

Orchestration should use the portable Compose specification and work with both `podman compose` and `docker compose`. Images and database versions should be pinned in committed configuration rather than use floating `latest` tags. Non-secret defaults may live in an example environment file; actual local secrets stay untracked.

The existing backend Testcontainers PostgreSQL setup remains useful for automated integration tests, but it is separate from the long-running developer database. No Kubernetes, local cluster, message broker, service mesh, or distributed observability stack is proposed.

Exact ports, image build files, health checks, volume/seed policy, and whether the full frontend runs in a container remain implementation-time decisions. Backup/restore considerations must be documented before a production deployment, not simulated through speculative local infrastructure.

## 16. Testing boundaries

### Framework

- Unit tests for approved cross-cutting contracts and behavior.
- Focused integration tests only for infrastructure the framework actually owns.
- No business-module behavior tests in the framework artifact.

### Reusable module

- Domain/application unit tests within the module.
- PostgreSQL/Flyway integration tests for the module's repositories and migrations.
- Contract/API tests that treat internal classes as replaceable.
- A test proving the module starts with only its declared dependencies.
- Migration tests from supported released states once more than one release exists.

### Customer application

- Composition tests for the exact selected artifact set.
- Tests for customer-owned rules, security policy, integrations, APIs, and extension persistence.
- A negative assembly test or separate build profile/project proving an optional module can be physically omitted while the remaining application builds and starts.
- Cross-module scenario tests at the application boundary rather than repository-level coupling tests.

### Frontend and end-to-end

- Type checking, linting, and production build are baseline gates with the current toolchain.
- UI unit/component and browser end-to-end tools should be selected only when the first UI behavior requires them; this document does not add a test framework.
- The reference application's critical realistic scenario should cover frontend, REST API, persistence, and customer extension behavior.

### Release compatibility

The reference customer application is a release gate. It must consume built/published version coordinates rather than import reusable source internals. A proposed release should test clean assembly, module omission, migration, public contract compatibility, and the reference extension. Exact compatibility matrices and artifact-repository staging remain open.

## 17. Reference implementation structure

The reference implementation should be an explicitly isolated, customer-style project, even if initially hosted in the same repository for convenience. It has:

```text
reference/<reference-customer>/
  backend/
    pom.xml                       independent executable build
    src/main/...                  composition root and customer code
    src/main/resources/...        customer configuration/migrations
    src/test/...                  realistic boundary/regression tests
  frontend/                       copied and customized sbp-ui baseline
  deployment/                     customer-style local deployment files
```

It must:

- pin released/approved SBP artifact versions;
- select only the framework and proof module(s) it uses;
- add meaningful customer-owned data and behavior without editing reusable module source;
- own its identity/security policy and customer APIs;
- own its extension tables and migrations;
- prove a selected module can be omitted;
- be allowed to trigger changes to framework contracts when the proposed architecture creates avoidable complexity.

The reference implementation should not be a showcase containing every candidate module. The exact customer scenario and first proof module remain open. The PRD names Customer as a proof-module candidate, also lists Contact as in the v1 baseline, and separately leaves the reference proof-module choice open; architecture should not resolve that product choice without review.

## 18. Proposed architecture decisions

These decisions are proposed by v0.1 and should be accepted or revised through ADR review:

- **D-01 — Modular monolith:** one customer backend process and normally one customer database.
- **D-02 — Artifact composition:** framework and approved business capabilities are independently versioned JARs; the customer application is the executable composition root.
- **D-03 — Explicit selection:** Maven dependencies plus explicit Spring wiring determine included backend capabilities.
- **D-04 — Package boundary:** only owner-defined `contract` packages are supported cross-artifact Java APIs; implementation and persistence remain internal.
- **D-05 — Composition-first extension:** customer data and services compose around stable SBP identities/contracts; entity inheritance and SBP-table customization are rejected.
- **D-06 — Persistence ownership:** each module/customer extension owns its structures and packaged Flyway migrations.
- **D-07 — In-process collaboration:** synchronous contracts are the default; simple in-process events require a demonstrated need; distributed infrastructure is excluded.
- **D-08 — Deliberate upgrades:** customer builds pin exact versions; release adoption is never automatic.
- **D-09 — Deferred BOM:** introduce a BOM only after real compatible artifact releases exist.
- **D-10 — Frontend copy model:** `sbp-ui` is copied and customer-owned after project creation; it is not forced into backend artifact semantics.
- **D-11 — Configuration ownership:** framework, module, customer, and deployment configuration/secrets have separate owners and namespaces.
- **D-12 — Portable local runtime:** use minimal OCI/Compose-compatible development infrastructure that works with Podman and Docker.
- **D-13 — Reference authority:** an isolated reference customer application is a release boundary test and may invalidate these decisions.

## 19. ADRs required before or during initial implementation

The following ADRs should be written in decision order. An ADR may preserve an item as a time-boxed experiment when evidence is not yet available.

1. **ADR-001: Backend repository, Maven reactor, and published artifact topology** — final artifact names, aggregator/parent roles, and whether reusable artifacts remain in one repository.
2. **ADR-002: Module assembly and boundary enforcement** — explicit Spring wiring mechanism, visibility verification, and the result of evaluating Spring Modulith.
3. **ADR-003: Public Java and HTTP contract conventions** — contract types, errors, identity representation, compatibility surface, and API documentation approach.
4. **ADR-004: Module persistence and Flyway orchestration** — locations, history strategy, ordering, omission, upgrade, and failure behavior.
5. **ADR-005: Customer extension persistence mapping** — how extension rows reference baseline identities and what integrity guarantees are required.
6. **ADR-006: Reference security integration** — development/demo identity choice while keeping customer authentication and authorization policy outside SBP.
7. **ADR-007: First proof module and reference scenario** — the smallest realistic capability that proves packaging, persistence, UI, removal, and customer extension.
8. **ADR-008: Frontend baseline lifecycle** — copy procedure, provenance, selected-feature copying, branding boundary, and manual upgrade expectations.
9. **ADR-009: Release versioning and private artifact publication** — version rules, compatibility policy, staging, and promotion.
10. **ADR-010: BOM introduction criteria** — when enough released evidence exists, what a tested combination means, and how overrides work.
11. **ADR-011: Local OCI/Compose topology** — concrete images, ports, data lifecycle, and Podman/Docker compatibility after proof-module needs are known.

An event convention ADR is not required until a real event consumer exists. An object-storage ADR is not required until Document or another selected capability demonstrates the need.

## 20. Open decisions

The following must remain open until implementation or product review supplies evidence:

- Exact Maven directory, parent POM, and publication-repository topology.
- Exact Spring module activation/discovery mechanism and whether Spring Modulith adds sufficient value.
- Whether any module needs a separate contract-only JAR.
- The proof module and realistic reference-customer scenario.
- Whether Contact is mandatory in v1 in addition to the one required proof module.
- Identifier representation and public error/result conventions.
- Flyway history and ordering strategy across independently versioned module JARs.
- Cross-module foreign-key policy for any concrete relationship that cannot tolerate application-level reference validation.
- Transaction and after-commit semantics for the first demonstrated in-process event.
- Reference identity provider/mechanism and demo authorization policy.
- Private Maven repository, release promotion, artifact signing, and retention.
- Semantic-versioning guarantees and supported compatibility window.
- Timing and detailed mechanics of an SBP BOM.
- Frontend feature-folder layout, API-client strategy, test framework, and baseline upgrade assistance.
- Runtime versus build-time non-secret frontend branding/configuration.
- Customer-project creation mechanics; no generator is justified yet.
- Concrete Podman/Docker files, ports, images, and local data reset/seed policy.
- Object-storage provider and abstraction shape until a document-storage use case is selected.
- Backup/restore targets and operational runbooks for a real deployment.

## 21. PRD ambiguities and tensions discovered

1. The task calls the v0.4 PRD approved, while the document labels itself “Draft — ready for technical architecture review.” This architecture treats its product statements as authoritative per the task, but the document status should be reconciled administratively.
2. Appendix B marks Customer as a v1 proof-module candidate and Contact as “Yes” for v1, while Section 15 requires at least one proof module and Section 18 leaves the first proof module open. It is unclear whether Contact is a mandatory second v1 module or only a likely core candidate.
3. The PRD requires a functional baseline UI for an installed module while explicitly allowing frontend template-copy reuse. It does not define how selected backend modules map to copied frontend feature source; that is appropriately left to the first module/UI implementation.
4. The PRD requires independently versioned artifacts but intentionally leaves compatibility and BOM rules open. v0.1 therefore proposes exact pinning and deliberate upgrades without claiming a compatibility window.
5. The framework concerns in Section 7.1 are examples (“may include”), not authorization to implement lifecycle discovery, audit, storage, security, or other services before their v1 use cases and contracts are approved.

No irreconcilable product conflict was found.

## 22. Deliberately not designed

Because evidence does not yet exist, v0.1 deliberately does not design:

- detailed Customer, Contact, Job, Document, or other domain models;
- a generic workflow/rules/custom-field/plugin engine;
- a dynamic module marketplace, licensing system, or runtime capability switches;
- microservices, brokers, distributed events, service discovery, CQRS, or event sourcing;
- universal RBAC/ABAC, SSO, MFA, or identity-provider policy;
- detailed audit or object-storage APIs before a proving use case;
- a customer-project generator or automatic frontend updater;
- shared frontend packages merely to imitate backend reuse;
- a complete module catalog, generic reporting, accounting, payroll, or regulated workflows;
- multi-tenancy, Kubernetes, service mesh, or enterprise observability infrastructure;
- schema details, API endpoints, UI screens, configuration keys, ports, or deployment sizing;
- automatic upgrades, downgrade guarantees, or speculative long-term compatibility adapters.

These omissions are intentional applications of “repetition earns abstraction” and “reference implementation may prove the framework wrong.”

## 23. Initial implementation gate

Before production implementation begins, reviewers should approve at minimum ADR-001 through ADR-004 and ADR-007. The first implementation should then build the smallest vertical slice that proves artifact consumption, explicit assembly/removal, owner-controlled persistence, one basic UI, and one meaningful customer extension. Any architecture that makes that slice harder without protecting a stated requirement should be simplified or rejected.
