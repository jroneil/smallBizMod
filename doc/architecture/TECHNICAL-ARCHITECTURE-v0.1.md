# Small Business Platform (SBP) — Technical Architecture v0.1

**Status:** Merged draft for approval  
**Scope:** Architecture only; no production implementation authorized  
**Product authority:** SBP PRD v0.4 baseline  
**Source proposals merged:** Codex Technical Architecture v0.1 and DeepSeek Technical Architecture v0.1

---

## 1. Purpose

This document reconciles the two independent SBP technical-architecture proposals into one working architecture baseline.

The PRD remains authoritative for product intent. This document resolves only those technical choices that are sufficiently supported by the PRD and by agreement between the two proposals. Where the proposals differ or implementation evidence is still missing, the decision remains explicitly open or deferred.

The reference implementation is expected to test this architecture and is allowed to prove it wrong. Architecture exists to make the reference application and future customer applications easier to build, not to protect an initial abstraction.

## 2. Architecture principles carried forward

The following are treated as non-negotiable unless the PRD changes:

1. SBP is a framework for assembling customer-specific small-business applications, not a generic SaaS product.
2. Customer applications are independently built and deployed.
3. The backend is a modular monolith: one customer application process and normally one customer database.
4. Reusable backend capabilities are delivered as independently versioned Java/Spring artifacts.
5. Optional business capabilities are selected by build composition, not unlocked through runtime commercial switches.
6. Baseline business modules remain deliberately generic.
7. Useful customer-specific policy and behavior belongs in the customer project first.
8. Repetition or clear general applicability earns promotion into SBP.
9. Composition and explicit contracts are preferred over entity inheritance.
10. A module owns its persistence structures and migrations.
11. Cross-module interaction must not reach directly into another module's persistence internals.
12. Customer projects pin approved framework/module versions and upgrade deliberately.
13. Frontend reuse is copy-and-customize template reuse, not forced backend-style packaging.
14. AI agents may implement bounded work, but architecture, acceptance, review, and release remain human-controlled.
15. The reference application may invalidate framework assumptions.

## 3. Runtime system context

For one customer deployment:

```text
Browser
   |
   v
Customer-owned Next.js application
(copied from the approved SBP UI baseline)
   |
   | REST/JSON
   v
Customer Spring Boot application
(single executable composition root)
   |
   |-- SBP framework artifact(s)
   |-- selected SBP business-module artifacts
   |-- customer-owned extensions/modules
   |
   +--> customer-dedicated PostgreSQL database
   +--> selected external integrations
   +--> selected object storage only when required
   +--> customer-selected identity/security provider
```

The customer application is the deployable product. SBP framework and module artifacts are reusable building blocks, not independently deployed services.

## 4. Backend artifact model

### 4.1 Approved direction

The reusable backend shall be split into versioned Maven artifacts.

Initial artifact roles:

| Artifact | Packaging | Responsibility |
|---|---|---|
| `sbp-framework` | JAR | Minimal cross-cutting framework contracts and approved shared infrastructure |
| `sbp-module-customer` | JAR | First proof business module |
| `sbp-module-contact` | JAR | Reusable Contact capability required in the completed V1 baseline after the Customer proof |
| future `sbp-module-*` | JAR | Additional approved reusable business capabilities |
| customer/reference application | executable Spring Boot JAR / OCI image | Composition root, selected modules, customer policy, customer extensions, customer APIs |

The reusable framework and business modules do **not** own the final Spring Boot application entry point. The customer application does.

### 4.2 Repository topology

The exact long-term repository topology remains open.

For v0.1, a single repository may contain:

```text
backend/
frontend/
reference/
doc/
```

but the reference application must be an independent Maven project with its own coordinates and package namespace.

Whether reusable artifacts ultimately remain in one repository or move to separate repositories is deferred until an actual distribution/collaboration need exists.

### 4.3 Maven reactor

A Maven multi-module reactor is an acceptable initial implementation mechanism for building reusable SBP artifacts together.

It is **not** treated as a permanent product requirement.

The first ADR shall determine:

- aggregator/parent POM structure;
- artifact names;
- which dependencies belong in the framework versus modules;
- where the Spring Boot executable plugin belongs;
- whether the first reusable artifacts remain in one reactor or are split sooner.

## 5. Java package conventions

Use one clear root namespace per reusable artifact.

Proposed roots:

```text
com.oneilsoftware.sbp.framework
com.oneilsoftware.sbp.module.customer
com.oneilsoftware.sbp.module.<capability>
```

Customer applications use their own namespace rather than living under `com.oneilsoftware.sbp.*`.

A reference application should deliberately use a fictional customer namespace such as:

```text
com.acmeco.reference
```

to prove that customer code does not depend on SBP package internals.

### 5.1 Public versus internal packages

Each reusable artifact shall expose a supported public contract surface and keep implementation details internal.

Recommended pattern:

```text
...api
...internal.application
...internal.domain
...internal.persistence
...internal.web
```

or an equivalent `contract` / `internal` split.

Only the public API/contract package is a supported compile-time integration surface.

The following remain internal:

- JPA entities;
- Spring Data repositories;
- persistence query types;
- internal services;
- controllers as Java implementation types;
- migration details;
- implementation-specific exceptions.

Public contracts should not expose JPA entities or repositories.

## 6. Dependency direction

Preferred direction:

```text
customer application / customer-owned modules
                 |
                 v
        selected SBP modules
                 |
                 v
           SBP framework
                 |
                 v
      Spring / Java / approved libraries
```

Rules:

1. The framework depends on no business module.
2. Reusable modules never depend on a customer project.
3. Circular module dependencies are prohibited.
4. Customer applications may depend on all selected module APIs and implementations.
5. Customer-owned modules may depend on SBP public contracts.
6. Cross-module persistence access is prohibited.
7. Another module's `internal` package is not a supported dependency surface.

### 6.1 Direct module-to-module dependencies

Direct reusable-module dependencies are allowed only when a real use case justifies them.

The default preference is:

```text
Module A      Module B
    \          /
     \        /
      Customer/application composition
```

rather than building a large reusable-module dependency tree.

When a direct module dependency is justified, it must:

- be explicitly declared in Maven;
- depend only on the owning module's public API/contract;
- remain acyclic;
- not access the other module's persistence internals.

Application-layer composition is preferred for cross-module use cases when it keeps modules more independently removable.

## 7. Module contract approach

A module contract should be the smallest explicit synchronous surface required by demonstrated consumers.

A contract may contain:

- immutable DTOs/records;
- service/application interfaces;
- stable or typed identifiers;
- documented query/command/result types;
- documented errors and transaction expectations where required.

Composition and interfaces are preferred over base-class inheritance.

A typical customer extension should compose around a stable baseline identity rather than subclass a reusable entity.

Example:

```text
SBP Customer
    id
    name
    status

AcmeCustomerProfile
    customerId
    serviceRegion
    preferredTechnician
    contractType
```

### 7.1 Events

No generic event contract is required for the first proof module.

In-process events are permitted only after a real one-to-many or notification-style collaboration demonstrates the need.

If introduced, they remain:

- in-process;
- simple;
- synchronous by default unless evidence requires otherwise.

No broker, queue, Kafka, distributed eventing, CQRS, or event sourcing is authorized by this architecture.

## 8. Customer extension model

Customer-specific functionality belongs in one of three levels.

### Level 1 — Configuration

Examples:

- branding;
- labels;
- non-secret endpoints;
- simple approved behavior options.

Configuration must not become a hidden programming language or a substitute for customer-specific domain implementation.

### Level 2 — Composition / extension

Examples:

- customer-owned wrapper/application services;
- customer-specific APIs;
- customer-owned extension persistence;
- customer-specific workflows;
- integrations;
- security-policy implementation.

### Level 3 — Customer-owned module

A genuinely new business capability may be implemented as a customer-owned module with its own:

- code;
- schema;
- migrations;
- APIs/contracts where needed.

### 8.1 Explicit anti-patterns

The following are not normal extension mechanisms:

- subclassing reusable JPA entities;
- adding customer-specific columns to reusable module-owned tables;
- modifying released SBP source directly in a customer project;
- copying customer-specific code into another customer project as a promotion mechanism.

Promotion occurs through the SBP baseline after generalization.

## 9. Persistence ownership

The approved ownership rules are:

1. One PostgreSQL database per customer application by default.
2. Each reusable module owns its own tables, indexes, constraints, persistence mappings, and migrations.
3. Customer extensions own their own persistence structures.
4. Only the owning module accesses its repositories and maps its tables.
5. Customer or module extensions store stable identifiers when referencing another capability.
6. Another module shall not alter another reusable module's schema as a normal extension mechanism.
7. Durable business history should use explicit lifecycle semantics where appropriate rather than destructive cascading deletion by default.

### 9.1 Schema-per-module versus shared schema

**Decision deferred.**

The DeepSeek proposal favors schema-per-module with separate Flyway histories because it makes module ownership mechanically obvious.

The Codex proposal keeps the exact Flyway/history strategy open and requires a proof against clean creation, upgrades, omission, and failure recovery.

The merged architecture adopts the Codex caution.

Before persistence implementation, ADR-003 shall compare at least:

**Model A — schema per module**
- strong physical ownership;
- independent history;
- potentially more operational complexity;
- cross-schema integrity may become awkward.

**Model B — shared schema with module-owned tables/migration locations**
- simpler relational integrity;
- easier conventional foreign keys;
- greater need for disciplined migration naming/ordering.

The selected model must be proven with the Customer proof module and reference application.

No architecture rule shall force the database to give up useful relational integrity merely to make code modules look cleaner.

## 10. Flyway ownership

Regardless of schema model:

- migrations live with the artifact/customer extension that owns the persistence change;
- released migrations are immutable;
- another module does not modify or rename an owner's released migrations;
- module omission must not require migrations from the omitted module;
- customer-specific migrations remain customer-owned.

The exact Flyway history-table strategy, ordering model, and startup orchestration are ADR decisions.

The proof must demonstrate:

- clean database creation;
- incremental module upgrade;
- customer-extension migration;
- module omission;
- failure/recovery behavior.

## 11. Cross-module interaction

Preferred order:

1. Customer/application-layer orchestration across multiple module APIs.
2. Direct synchronous call to another module's public API when the dependency is genuinely part of the reusable module's capability.
3. Stable identifiers as cross-module references.
4. Simple in-process events only when demonstrated.

Forbidden:

- direct access to another module's repository;
- shared mutable JPA entities;
- reading another module's tables as an integration API;
- REST calls between modules in the same process merely to create artificial boundaries;
- separate databases per module;
- distributed transactions;
- brokers/service discovery for internal module communication.

Transaction semantics for the first real cross-module use case remain open until that use case exists.

## 12. Versioning

Reusable framework and business-module artifacts are independently versioned.

Customer applications pin exact approved versions.

Rules:

- no version ranges in released customer builds;
- no automatic customer upgrades;
- no released customer build consumes `SNAPSHOT` artifacts;
- public API/contract changes are deliberate;
- internal implementation may evolve without becoming a supported compatibility surface.

Semantic versioning is a likely policy but shall not be treated as a contractual compatibility guarantee until the first real version evolution provides evidence.

## 13. BOM strategy

**Deferred until useful.**

A future `sbp-bom` is likely to be valuable once multiple independently versioned reusable artifacts exist and tested combinations need to be recorded.

Do not introduce a BOM merely because Maven supports one.

Trigger to introduce a BOM:

- at least the framework and multiple reusable module artifacts have real releases; and
- compatibility coordination has become a real maintenance concern.

The BOM will coordinate tested versions. It will not create lockstep releases or automatically upgrade customer applications.

## 14. Frontend strategy

`frontend/sbp-ui` is the approved reusable frontend baseline.

Reuse model:

```text
SBP UI baseline
      |
      | copy
      v
Customer frontend
      |
      +--> customize freely
```

The customer copy becomes customer-owned.

The frontend is not required to mirror backend artifact packaging.

The baseline may contain:

- application shell;
- navigation;
- branding/theme seams;
- basic forms/tables/detail/search patterns justified by real screens;
- basic UI for implemented baseline capabilities;
- an API boundary to the customer backend.

### 14.1 Frontend module folders

A feature-folder convention may be tried with the first Customer UI, but it is not yet a ratified architecture requirement.

No shared npm package registry, UI plugin system, schema-driven form engine, frontend monorepo dependency model, or automatic updater is required for v0.1.

If repeated frontend reuse later justifies extraction into packages, that decision can be made from evidence.

## 15. Configuration and secrets

Ownership is explicit:

| Owner | Responsibility |
|---|---|
| Framework | minimal framework-level technical defaults |
| Reusable module | module-owned technical settings and approved simple options |
| Customer application | branding, customer behavior, selected integrations, security policy |
| Deployment environment | database credentials, tokens, signing material, provider secrets |

Reusable artifacts must not contain environment credentials.

Real secrets are injected at runtime through environment variables, mounted files, or a customer-selected secret provider.

No universal secret manager is selected for v0.1.

## 16. Security boundary

SBP provides a security integration boundary and secure foundations.

Customer applications determine:

- identity provider;
- authentication method;
- MFA;
- roles;
- permissions;
- session policy;
- other company-specific authorization rules.

The reference application may use a minimal development/demo security implementation only to prove the boundary.

SBP does not define a universal RBAC/ABAC model in v0.1.

## 17. Local development model

The baseline development environment shall be reproducible and OCI-compatible.

Preferred development shape:

```text
Next.js dev process
Spring Boot dev process
PostgreSQL container
```

Use a portable Compose specification compatible with:

```text
podman compose
docker compose
```

Testcontainers PostgreSQL remains the preferred automated integration-test database.

Exact ports, container build mechanism, health checks, seed/reset policy, and production image strategy are implementation-time decisions.

No Kubernetes or local distributed infrastructure is required.

## 18. Testing boundaries

### Framework

- unit tests for framework-owned behavior;
- focused integration tests only for infrastructure actually owned by the framework.

### Reusable module

- domain/application unit tests;
- PostgreSQL/Flyway integration tests;
- public API/contract tests;
- test proving the module starts with only declared dependencies;
- migration tests once multiple released versions exist.

### Customer/reference application

- exact composition tests for selected artifacts;
- customer-owned rule and extension tests;
- cross-module scenario tests at the application boundary;
- removal test proving an omitted optional module does not break the remaining app.

### Frontend

Initial quality gates:

- type check;
- lint;
- production build.

Component/browser test tooling is selected when actual UI behavior justifies it.

Passing tests do not alone establish product correctness. Realistic behavior remains the acceptance standard.

## 19. Reference implementation

The reference implementation is an independent customer-style project.

Suggested shape:

```text
reference/<fictional-customer>/
  backend/
  frontend/
  deployment/
```

It must:

1. consume versioned SBP artifacts rather than reusable source internals;
2. use the Customer proof module;
3. add meaningful customer-owned data and behavior;
4. keep customer-specific persistence separate from reusable module-owned persistence;
5. use a customer-specific package namespace;
6. customize a copy of the SBP UI baseline;
7. provide a minimal customer-style security implementation;
8. prove the proof module can be omitted;
9. act as a realistic release/compatibility regression application;
10. be allowed to force simplification or redesign of the framework.

### 19.1 First proof module

**Customer is the first proof module.**

Contact is not part of the first Customer proof slice. That is an implementation
sequence decision only: Contact remains a required reusable capability in the
completed V1 baseline.

Contact represents a person associated with a Customer and is implemented as a
separate reusable module after the Customer proof. Customer remains an
organization/account-style baseline and does not absorb Contact fields.

The Acme Service Profile is customer-specific service/account information about
a Customer. It does not represent a person and does not satisfy any part of the
Contact baseline requirement.

## 20. Initial implementation scope

The first implementation slice should prove only:

```text
SBP framework JAR
        +
Customer proof-module JAR
        +
reference customer application
        +
basic Customer UI
        +
PostgreSQL persistence
```

The first slice must prove:

- reusable artifact consumption;
- explicit module selection/removal;
- public API/internal boundary;
- customer-specific composition around Customer;
- customer-owned extension persistence;
- workable Flyway ownership;
- copied/customized frontend baseline;
- basic local Podman/Docker development;
- realistic integration testing.

Not authorized in the first slice:

- Contact as a separate module (sequencing only; Contact is required after the
  first proof for the completed V1 baseline);
- Job;
- Document;
- Inventory;
- Scheduling;
- workflow engine;
- plugin system;
- generic custom-field engine;
- runtime licensing;
- microservices;
- message broker;
- event architecture without a consumer;
- BOM without a compatibility problem;
- customer-project generator.

## 21. ADRs

### ADR-001 — Backend artifact/repository topology

Decide:

- Maven reactor/parent structure;
- artifact coordinates;
- framework versus module dependency placement;
- executable plugin placement;
- initial repository topology.

**Required before restructuring the generated backend.**

### ADR-002 — Spring module assembly and boundary enforcement

Decide:

- explicit Spring wiring mechanism;
- auto-configuration versus explicit imports;
- package-boundary enforcement;
- whether Spring Modulith or ArchUnit provides value now or should remain deferred.

**Required before framework/module implementation.**

### ADR-003 — Persistence and Flyway orchestration

Decide:

- schema-per-module versus shared-schema ownership;
- migration discovery/history;
- migration ordering;
- foreign-key policy;
- module omission;
- customer-extension migration approach.

**Required before Customer persistence implementation.**

### ADR-004 — First proof module and reference scenario

Ratify:

- Customer as the first proof module;
- fictional reference-customer scenario;
- minimal vertical slice needed to prove the architecture.

**Required before domain implementation.**

### Later ADRs, evidence-triggered

- public API/identifier/error conventions;
- reference security integration;
- frontend baseline lifecycle;
- artifact repository/publication;
- versioning/BOM policy;
- storage abstraction;
- customer-project generator;
- OCI image build/deployment approach;
- event convention if a real event consumer appears.

## 22. Deliberately open decisions

Remain open until evidence exists:

- long-term single-repo versus multi-repo layout;
- exact Spring auto-configuration/discovery mechanism;
- Spring Modulith adoption;
- ArchUnit or other boundary tooling;
- schema-per-module versus shared-schema persistence;
- cross-module FK policy;
- exact identifier representation;
- exact public error/result convention;
- async in-process events;
- object-storage abstraction details;
- private artifact repository;
- semantic-versioning compatibility guarantees;
- BOM introduction timing;
- frontend feature-folder structure;
- frontend API-client strategy;
- frontend test framework;
- runtime versus build-time branding configuration;
- customer-project generator;
- production container-image strategy;
- backup/restore runbooks for a real deployment.

## 23. Explicitly rejected for v0.1

The merged architecture rejects or defers the following unless a real requirement appears:

- microservices;
- module-to-module REST calls inside the monolith;
- Kafka or other brokers;
- CQRS/event sourcing;
- generic plugin framework;
- workflow/rules engine;
- generic custom-field engine;
- runtime commercial module toggles;
- licensing/anti-tamper infrastructure;
- universal RBAC/ABAC;
- Kubernetes;
- frontend package extraction merely to imitate backend modularity;
- automatic customer upgrades;
- automatic frontend template updater;
- project/module generators before repetition proves their value.

## 24. Implementation gate

Before production implementation begins, approve at minimum:

- ADR-001;
- ADR-002;
- ADR-003;
- ADR-004.

Then implement the smallest vertical slice that proves:

```text
framework artifact
      +
Customer artifact
      +
reference app
      +
Customer UI
      +
PostgreSQL
```

If this slice requires avoidable adapters, excessive framework plumbing, or abstractions that do not protect an explicit requirement, simplify the architecture rather than expanding it.

## 25. Merge rationale

The merged architecture intentionally combines:

### Retained from both proposals

- modular-monolith runtime;
- customer application as composition root;
- versioned backend JARs;
- public API/internal package boundaries;
- composition-first customer extension;
- module-owned persistence;
- synchronous in-process collaboration;
- copied frontend baseline;
- deliberate customer upgrades;
- isolated reference application;
- minimal Podman/Docker development model.

### Retained primarily from the Codex proposal

- caution around unresolved persistence/Flyway strategy;
- application-layer composition as the preferred cross-module orchestration point;
- BOM deferred until actual release compatibility evidence;
- events deferred until a real consumer exists;
- explicit preservation of unresolved decisions.

### Retained primarily from the DeepSeek proposal

- strong 1:1 artifact/package naming discipline;
- reference application using a non-SBP fictional customer namespace;
- executable application outside reusable artifacts;
- explicit API/internal package split;
- concrete customer-extension anti-patterns;
- practical initial Maven reactor as an implementation option.

### Explicitly not adopted from DeepSeek as settled decisions

- schema-per-module as the default;
- no-cross-schema-FK policy;
- mandatory early BOM;
- domain events as a standard initial contract element;
- Contact as part of the first Customer proof slice.

These remain deferred because the Customer proof module can answer them more cheaply and reliably than further architecture speculation. This deferral does not remove the PRD-required Contact module from the completed V1 baseline.

---

**Status:** Merged Technical Architecture v0.1 — ready for ADR review before implementation.
