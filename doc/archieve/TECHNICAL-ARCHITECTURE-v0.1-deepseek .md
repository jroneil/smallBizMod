# SBP Technical Architecture — v0.1 (Draft for Review)

| | |
|---|---|
| Status | Proposed — draft for technical architecture review |
| Version | 0.1 |
| Author | Technical Architecture (SBP) |
| Source of authority | PRD v0.4 — `doc/SBP_PRD_Template_v0_4_Baseline.docx` (requirement IDs FR-xx, NFR-xx, P-xx cited inline) |
| Scope | Backend artifact model, module contracts, persistence/Flyway ownership, cross-module interaction, versioning/BOM, frontend template strategy, configuration/secrets, local dev model, testing boundaries, reference implementation, ADR backlog, open decisions |
| Out of scope | Implementation, redesign of the generated `backend/sbp` or `frontend/sbp-ui` baselines, new dependencies, infrastructure beyond what the PRD requires |

This document proposes the architecture that the reference implementation will validate. Per **P-10 / FR-24**, the reference implementation is permitted to prove parts of this architecture wrong; this document is not a protective contract around an initial design. Nothing in this document authorizes production code. All SHALL/SHOULD/MAY wording mirrors PRD v0.4 usage.

---

## 1. Baseline facts (what exists today)

The repository contains generated, unmodified baselines:

| Area | Location | Facts |
|---|---|---|
| Backend | `backend/sbp` | Maven project, `com.oneilsoftware:sbp:0.0.1-SNAPSHOT`, parent `spring-boot-starter-parent:4.1.1`, Java 21. Single `@SpringBootApplication` (`com.oneilsoftware.sbp.SbpApplication`). Lombok annotation-processor configured. |
| Backend starters | `backend/sbp/pom.xml` | actuator, batch, data-jpa, flyway, mail, security, validation, webmvc; `flyway-database-postgresql`; `postgresql` (runtime). Test: `*-test` starter variants, `spring-boot-testcontainers`, testcontainers jupiter + postgresql. |
| Backend tests | `backend/sbp/src/test` | `@SpringBootTest` context load with Testcontainers PostgreSQL (`TestcontainersConfiguration`). |
| Backend config | `application.properties` | Only `spring.application.name=sbp`. |
| Frontend | `frontend/sbp-ui` | Next.js 16.3.3 / React 19.2.8 / TypeScript 5 / Tailwind 4 / ESLint 9, create-next-app boilerplate, `sbp-ui` v0.1.0. |
| Docs | `doc` | PRD v0.4 docx only. No architecture docs exist yet. |

Observations recorded for later ADRs (not actions):

- The generated backend is a **single application project** containing all starters. The PRD requires a framework + independently packageable modules (FR-02, FR-25); splitting the current project into artifacts is an ADR-driven restructuring, not a redesign of the baseline.
- The baseline pom includes starters not required by the PRD v1 scope (batch, mail, actuator is borderline). Which starters belong to which artifact is decided by **ADR-01** during the split, evidence-driven.
- The frontend baseline is a generic create-next-app shell; it is the starting point for the SBP UI template (FR-26).
- Next.js 16 has breaking changes vs. older Next.js conventions (see `frontend/sbp-ui/AGENTS.md`). The template pins exact versions; customers copying it inherit those pins.

---

## 2. Architecture at a glance

```
┌──────────────────────────────────────────────────────────────────┐
│ Customer application (own repo, own groupId/package, own secrets) │
│   app code · customer-owned modules · config · branding           │
│   customer extension data (customer-owned schema) · security      │
│   policy · customer-specific APIs (FR-22)                         │
└───────────────────────────────┬──────────────────────────────────┘
                 consumes versioned artifacts, pinned via sbp-bom (FR-25, §14)
┌───────────────────────────────▼──────────────────────────────────┐
│ SBP platform (single reactor repo — proposal, see ADR-01)         │
│   sbp-module-customer  ·  sbp-module-contact  ·  (future modules) │
│            │  depend only on each other's api contracts (FR-03)   │
│   ┌────────▼─────────┐                                            │
│   │   sbp-framework  │  contracts + shared infrastructure (FR-01) │
│   └────────┬─────────┘                                            │
│            │                                                      │
│   Spring Boot 4.1.1 · PostgreSQL · Flyway (schema per module)     │
└───────────────────────────────────────────────────────────────────┘
        modular monolith: one process, one database instance,
        explicit module/ownership boundaries (P-03, NFR-07)
```

Six load-bearing decisions (each traced to the PRD in the sections below):

1. **D1 — Modular monolith.** One process, one shared database instance, explicit module boundaries (P-03, NFR-07). No microservices, no messaging broker, no distributed infrastructure (Non-Goals §5).
2. **D2 — Backend reuse = versioned artifacts; frontend reuse = template copy.** Backend modules are consumed as versioned Maven artifacts (FR-25); the frontend is copied into a customer project and customized (FR-26). Two different reuse models, deliberately.
3. **D3 — A module is a Spring Boot auto-configuration artifact.** Including a module in a build = declaring the dependency; its beans and schema wiring activate via its own auto-configuration (FR-02, FR-04, P-04). No runtime switches.
4. **D4 — Extension by composition at the customer layer.** Customer behavior composes around stable module contracts/identifiers; extension data lives in customer-owned persistence (FR-15, FR-18, P-05, §7.4).
5. **D5 — Cross-module collaboration via contracts, identifiers, and simple in-process events.** No module reaches another module's persistence internals (FR-20).
6. **D6 — Independent SemVer per artifact + `sbp-bom` release baseline.** Customer applications explicitly pin versions; upgrades are deliberate (FR-25, §10, §14).

---

## 3. Proposed backend project/artifact boundaries

### 3.1 Target Maven topology (proposal)

A single Maven multi-module reactor for the SBP platform, plus a **separate** reference-application project that is deliberately **not** a reactor module (it must consume *released* artifacts to prove FR-25 and the V1 acceptance criteria).

| Artifact | Packaging | Role | Depends on |
|---|---|---|---|
| `com.oneilsoftware:sbp-parent` | pom | Platform reactor parent; owns module list and shared build config (reuses `spring-boot-starter-parent` for dependency management, consistent with the current baseline) | — |
| `com.oneilsoftware:sbp-framework` | jar | Framework: stable contracts + shared infrastructure for the approved cross-cutting capabilities (FR-01, §7.1) | Spring Boot/Spring core only — **no SBP artifact, no business code** |
| `com.oneilsoftware:sbp-module-customer` | jar | Proof module (PRD Appendix B: "Customer — Core baseline — Yes (proof-module candidate)") | `sbp-framework` |
| `com.oneilsoftware:sbp-module-contact` | jar | Core baseline module (Appendix B: "Contact — Core baseline — Yes") | `sbp-framework`; `sbp-module-customer` **api** only if a declared contract requires it (FR-03) |
| `com.oneilsoftware:sbp-bom` | pom | Bill of materials: pins framework + module versions known to work together; the release baseline (§14) | — |
| Future modules (Job, Document, …) | jar | Only when selected for implementation (FR-14, P-02, P-11) | `sbp-framework` (+ declared api deps) |
| `com.oneilsoftware:sbp-reference-app` (or customer-style coordinates, §14) | jar (executable) | Reference implementation — **outside the reactor**, consumes released SBP artifacts like a customer project | Published framework + selected modules via `sbp-bom` |

### 3.2 Rules that follow

- **Framework has zero knowledge of modules.** Modules depend on the framework; the framework never depends on a module (inversion keeps FR-01 and FR-02 honest).
- **Module removal is dependency removal.** Omitting a module's artifact from a customer build must leave the remaining application buildable, startable, and functional (FR-02). This is the modular-monolith "module = artifact = auto-configuration" contract (D3).
- **Executable packaging lives at the customer/application layer.** The current single `backend/sbp` project carries `spring-boot-maven-plugin`; after the split (ADR-01) the boot plugin belongs to application projects (reference app, customer apps), not to framework/module jars.
- **No assembly/starter aggregator artifact** (e.g., an "sbp-all" bundle) is proposed. If a second consumer needs one, that is a promotion decision (P-11), not a v0.1 design.
- **Artifact coordinates.** Platform artifacts use `groupId com.oneilsoftware` (consistent with the baseline) and artifactIds `sbp-framework`, `sbp-module-<name>`, `sbp-bom`. Customer applications use their own groupId.

### 3.3 Repository topology

Proposal for v0.1: the platform reactor, the reference application, and the frontend template all live in one repository (`backend/`, `frontend/`, `reference/`, `doc/`), matching the current layout. The reference app is a separate Maven *project* (own `pom.xml`, own package namespace) even though it shares the repository. **OPEN:** PRD §18 explicitly leaves "one platform repository or multiple repositories" open; revisit when the first independent consumer or external collaborator appears (trigger documented in §17).

---

## 4. Java package conventions

### 4.1 One root package per artifact (1:1 mapping)

A 1:1 artifact → root-package mapping is the enforcement enabler: boundary tests and review can check "no class outside artifact X imports a class under X's internal packages."

| Artifact | Root package |
|---|---|
| `sbp-framework` | `com.oneilsoftware.sbp.framework` |
| `sbp-module-customer` | `com.oneilsoftware.sbp.module.customer` |
| `sbp-module-contact` | `com.oneilsoftware.sbp.module.contact` |
| customer project | `com.<customer>.<application>` (customer-owned) |
| reference app | fictional customer namespace, e.g. `com.acmeco.reference` — deliberately **not** under `com.oneilsoftware.sbp.*`, to prove no package coupling between platform and application layer |

### 4.2 Module internal structure: `api` vs `internal`

Each module splits into two sub-packages:

- `…module.<name>.api` — the **public contract**: DTOs, service interfaces, typed identifiers, domain events. This is the only package other modules and customer code may import.
- `…module.<name>.internal` — **implementation**: JPA entities, repositories, services, controllers, migrations. No other artifact may import from here.

Framework packages follow the same idea: public contract packages (`…framework.contracts.*` or equivalent) vs. implementation (`…framework.internal`). Exact framework package layout is decided by **ADR-02/ADR-08** when the framework skeleton is built.

**Rule:** cross-module and customer-to-module code imports only `.api` packages of other artifacts; anything else is a boundary violation (see §5 enforcement).

---

## 5. Dependency direction and rules

### 5.1 Direction

```
customer app  →  modules (api only)  →  framework (contracts)  →  Spring/third-party
```

### 5.2 Rules

- **R1** — Direction is acyclic as above. The framework depends on no SBP artifact.
- **R2** — A module declares exactly the dependencies it needs; nothing relied on implicitly through another module's transitive dependencies (FR-03).
- **R3** — Optional module-to-module integration uses the target module's **api** contract and an explicit declared dependency, or a framework contract — never undeclared coupling to internals (FR-03).
- **R4** — No module or framework artifact may depend on a customer application.
- **R5** — No imports from another artifact's non-api (internal/implementation) packages, and no reach into another module's persistence (FR-20).
- **R6** — Removing a module's dependency must not break compilation of the remaining build (FR-02). This is exercised as an automated removal test (§13).

### 5.3 Enforcement (v0.1 and beyond)

- v0.1: conventions above + human review + targeted **boundary tests** (a small set of tests that assert the package-import rules by scanning the classpath or by construction of the build matrix). No new dependencies are required for v0.1.
- Later, if violations recur or review cost demonstrates a need: evaluate Spring Modulith (PRD §18 explicitly says *evaluate without assuming adoption*) and/or ArchUnit as test-scope tooling. Adoption is an evidence-driven decision (**ADR-08**), not a v0.1 commitment.

---

## 6. Module contract approach

### 6.1 What a module contract consists of

Each module's `api` package exposes, at minimum:

- **DTOs** — immutable records for create/read/update operations.
- **Service interfaces** — the module's application-level API (the "module API").
- **Typed identifiers** — lightweight value records, e.g. `record CustomerId(UUID value)`. PRD FR-20 names identifiers as a cross-module mechanism; typed IDs give the compiler a role in boundary enforcement (a `CustomerId` cannot be passed where a `ContactId` is expected).
- **Domain events** — immutable records describing facts the module publishes (see §9). Events are optional per module; only what the reference implementation demonstrates is needed (P-11).

### 6.2 Contract semantics

- Contracts are **part of the module artifact and its version**. A breaking contract change is a major-version change (§11).
- Contracts are **interfaces, not base classes**. Composition and explicit contracts are preferred over deep inheritance (P-05, §7.4, FR-06).
- **Extension points are not invented speculatively.** The framework defines extension *boundaries* where the PRD mandates them (security integration FR-09, audit hooks FR-10, storage abstraction FR-11) and nothing else. Module-level extension SPIs (hooks, registries, plugin-style mechanisms) are **not designed in v0.1** — the reference application must demonstrate the need first (P-02, P-10, P-11, and the task directive to avoid speculative framework capabilities).

### 6.3 Framework contract areas (PRD-mandated, minimal shape)

| Area | PRD | v0.1 stance |
|---|---|---|
| Security integration boundary | FR-09 | Define the boundary contract (where customer security policy plugs in); shape validated by the reference app. No universal IdP/RBAC/ABAC model (Non-Goals §5, §16). |
| Audit hooks | FR-10 | Provide a convention/hook for consistent action recording; mechanism (annotation vs. event vs. callback) decided in ADR-06 once a first consumer exists. |
| Storage abstraction | FR-11 | Contract *area* recognized; concrete SPI shape deferred until the Document module or a demonstrated need (OPEN, §17). |
| Configuration conventions | FR-12 | `sbp.*` property namespaces per module; defaults in artifacts, overrides in customer config (§12). |
| Validation / error handling | §7.1 | Use the baseline's Bean Validation; a uniform API error envelope is proposed only when the reference app demonstrates the need (P-11). |
| Module lifecycle/discovery | §7.1 | D3: inclusion-by-dependency + per-module auto-configuration. No separate registry/plugin engine. |

---

## 7. Customer extension and composition approach

### 7.1 The three customization levels (FR-23)

| Level | Mechanism | Where it lives | PRD |
|---|---|---|---|
| **1. Configuration** | Properties for branding (name, logo, theme), labels, simple approved options | Customer project config only | FR-08, FR-12 |
| **2. Composition/extension** | Wrapper/composition services around module contracts; customer-owned entities/tables keyed to baseline identities; customer controllers/APIs; implementations of framework security/audit/storage boundaries | Customer project code | FR-06, FR-15, FR-22, FR-23 |
| **3. Customer-owned modules** | Entirely new business capabilities as customer-owned packages/artifacts | Customer project | FR-23, §7.5 |

### 7.2 Composition patterns (defaults, not exhaustive)

- **Extend data by composition, not columns.** A baseline `Customer` remains the module's entity. Customer-specific profile data is a customer-owned table (e.g. `acme_customer_profile`) referencing the baseline identity by `CustomerId` (FR-15, FR-18). See §8 for persistence mapping.
- **Extend behavior by wrapping.** Customer services call module api services and add policy, workflow, or integration around them (P-05). The PRD's own example: a baseline Customer "need not become AcmeCustomer through class inheritance; Acme-specific profile data and behavior may compose around the stable Customer identity and contract" (§7.4).
- **Extend the API surface at the customer layer.** Customer-specific operations and endpoints stay in the customer project (FR-22); they do not expand generic baseline APIs with customer policy.
- **Extend security at the customer layer.** Identity provider, authentication, MFA, roles, permissions, session policy are customer decisions wired into the framework's security boundary (FR-09, §10 customer model).

### 7.3 Explicit anti-patterns (never a normal extension mechanism)

- Class inheritance from module entities or contract implementations.
- Adding customer-specific columns to framework/module-owned tables (FR-18).
- Editing released module/framework source in a customer project (P-05 "extend rather than fork").
- Copying customer code between projects as a promotion mechanism (FR-13: promotion happens through the SBP baseline/versioning process).

### 7.4 Promotion path

Customer-first implementation (P-09): functionality is customer-owned by default; it may be promoted into SBP only when repetition or clear generality is demonstrated **and** it can be generalized without embedding the originating customer's policy (FR-16, FR-17, P-06, P-11, §9 promotion rule).

---

## 8. Persistence and Flyway ownership

### 8.1 Ownership rules (from the PRD)

- Each module **owns its tables and migrations** (FR-19). Customer projects and unrelated modules must not alter a module's schema as a normal extension mechanism.
- Customer extension data lives in **customer-owned persistence structures** (FR-18), not as columns on module tables.
- Business records support explicit lifecycle states (active, inactive, archived, closed) where appropriate; **destructive cascading deletion is not the default** for durable business history (FR-21).
- Shared database instance, explicit boundaries (P-03). Single PostgreSQL database per customer application (§10, NFR-05).

### 8.2 Candidate models (PRD §18 lists migration ownership/ordering as open)

**Model A — Schema per module (v0.1 default proposal).**
Each module owns a dedicated PostgreSQL schema (`sbp_customer`, `sbp_contact`, …) with its own Flyway migration history inside that schema. Customer extension data uses a customer-owned schema (e.g. `app` or `acme`). The framework defines the convention for wiring one Flyway instance per module schema; exact wiring details are implementation under ADR-03.

- *Why this default:* it makes FR-19 literal and mechanical — a module's migrations travel with its artifact, histories are independent, module versions don't collide in a shared history table, and removal of a module leaves a cleanly removable schema (FR-02).
- *Consequence:* **no cross-schema foreign keys.** Cross-module references are expressed as contract identifiers (typed IDs) without DB-level FK constraints, or with FKs only *within* a module's own schema (FR-20). Integrity across module boundaries is enforced at the application contract layer.

**Model B — Single schema with per-module migration locations/version prefixes.**
All modules share one schema and one history table; migrations are grouped by location and versioned with module prefixes. Simpler initially, but it couples module histories and makes independent packaging/ordering fragile.

**Decision rule:** v0.1 adopts Model A as the proposal; the proof module and the reference application are the evidence gate (P-10/FR-24). If Model A proves unnecessarily heavy in the reference implementation, **ADR-03** documents the pivot to Model B — the architecture must not be protected through avoidable complexity.

### 8.3 Migration conventions (once a model is chosen)

- Migrations live inside the owning artifact (module jar / customer project).
- Version numbering is per-owner (per schema), so module versions move independently.
- Baseline migrations for the current single-project skeleton are re-homed into the framework/module artifacts during ADR-01; no Flyway migration exists in the baseline today (`application.properties` has only the app name), so there is nothing to migrate yet.

---

## 9. Cross-module interaction approach

Default order of preference (all in-process; no distributed infrastructure — Non-Goals §5, NFR-07):

1. **Direct contract calls.** Module A calls module B's `api` service interface (Spring beans). This is the default for synchronous, coupled-enough behavior.
2. **Identifiers as links.** References between modules are typed IDs, not objects from another module's persistence layer (FR-20).
3. **In-process events** via Spring's `ApplicationEventPublisher` (already part of the Spring baseline — no new dependency) for decoupled reactions where a direct call would create unwanted coupling. **Adoption is evidence-driven:** v0.1 defines the *convention* (immutable event records in the publisher's `api` package; synchronous delivery by default; async only with demonstrated need). No broker, no queues, no service bus.

**Forbidden:** direct repository access to another module, shared JPA entities across modules, cross-schema FKs (§8.2), shared mutable services outside contracts, module A reading module B's tables.

PRD §18 lists "cross-module contract and in-process event conventions" as an open decision; this section is the v0.1 proposal to be confirmed by **ADR-04** against reference-implementation evidence.

---

## 10. Versioning and future BOM strategy

### 10.1 Versioning policy (proposal)

- **SemVer** for every SBP artifact. Breaking contract changes → major bump. Compatible additions → minor. Fixes → patch.
- **Independent versions** per artifact (FR-25, §14: "Reusable artifacts SHOULD be independently versioned").
- **`sbp-bom`** is the release baseline: it pins framework + module versions tested to work together (§14: BOM "MAY define combinations known to work together" — v0.1 proposes making it the *mechanism* for the release baseline, since §14 defers the policy to "after the initial packaging model is selected", which this document selects in §3).
- **Customer pinning:** a customer application imports `sbp-bom` and thereby pins the approved framework/module versions; it may override deliberately and records that choice. No automatic flow of platform upgrades into customer deployments (§10, §14).
- **Reference app as compatibility gate:** each approved SBP release is validated by rebuilding and running the reference application against the released artifacts before the BOM is published (V1 scope: "reference application used as a compatibility and realistic-behavior regression test for approved SBP releases").

### 10.2 Upgrade/compatibility policy

Partially defined by v0.1: within a BOM, artifacts are tested together; a customer moves between BOMs deliberately (with the reference app as the migration test). The full compatibility/upgrade matrix (which artifact versions may coexist, migration ordering across module versions) remains **OPEN** until the first real version bump provides evidence (§17).

### 10.3 Artifact repository

v0.1: `mvn install` to the local Maven repository is sufficient for the reference implementation and any single customer build. A private artifact repository strategy (Nexus/Artifactory/GitHub Packages/…; PRD §18) is triggered by the first need to distribute artifacts beyond one team — evidence-gated (**ADR-11**, §17).

### 10.4 Frontend versioning

The frontend template is versioned **independently** of backend artifacts (currently 0.1.0) and does not participate in the backend BOM (FR-26). A release note mapping "frontend template version ↔ sbp-bom version known to work together" is proposed as a lightweight convention, not a package-manager mechanism (§11).

---

## 11. Frontend template strategy

### 11.1 Reuse model: copy, don't package

- `sbp-ui` is an **approved baseline/template** that customer projects copy and customize (FR-26). It is **not required** to follow backend JAR/package reuse; no npm-package distribution, no shared-component registry, no frontend monorepo tooling is proposed for v0.1 (P-02, P-11).
- The template pins exact versions (Next.js 16.3.3, React 19.2.8, Tailwind 4, TS 5). Customers inherit the pins by copying; template upgrades are deliberate and propagated by re-copying or applying documented deltas — consistent with "customer project may remain on an approved SBP version" (§10).
- **Branding (FR-08)** is configurable in the customer copy via theme tokens (Tailwind CSS variables) and layout/app metadata — no reusable source edits.

### 11.2 Module UI delivery (proposal — needs confirmation, see §18)

- Module UI ships **inside the template copy** as feature folders (e.g. `app/modules/<module>/…` with its data-access layer), matching the module's backend api. "Installing" a module in the frontend = keeping its folder in the customer copy; removing the module = deleting the folder such that the remaining app still builds and starts (mirrors backend FR-02 removal behavior).
- v0.1 proposes no cross-module UI composition framework; basic module UI is basic by design (FR-07, P-07) and customer projects replace/wrap/enhance it freely.
- Whether module UI should instead be published as separately versioned copyable packages is **OPEN** until a second module UI exists (evidence gate, §17).

---

## 12. Configuration and secrets boundaries

### 12.1 Configuration ownership (FR-12)

| Owner | What it configures | Where it lives |
|---|---|---|
| Framework | Framework-level behavior (e.g. audit on/off, storage backend selection) | Defaults inside `sbp-framework`; overridable |
| Module | Module behavior and approved presentation options | Defaults inside the module artifact under `sbp.<module>.*` namespaces |
| Customer | Branding, labels, simple options; overrides of framework/module properties; customer extension behavior | Customer project `application.yml` / environment |

Rules:

- Configuration addresses **presentation and approved behavior choices**, not a substitute for customer-specific domain implementation (FR-12).
- **No customer-editable switches that unlock capabilities absent from the assembled project** (FR-05; Non-Goals §5: no switches that unlock unpurchased modules). Configuration only tunes what is already included in the build.
- Removing a module from the build removes its configuration surface entirely (D3).

### 12.2 Secrets (NFR-04)

- Secrets are **never committed** to customer source or distributed artifacts; they are injected at runtime via environment variables / secret managers per deployment.
- Customer projects own their secrets and their secret-handling policy (§10: separate customer configuration and secrets).
- Local development: `.env` files are git-ignored; the local dev model (§13) supplies throwaway credentials.
- The reference implementation uses development-only credentials (clearly marked as such), never real customer secrets.

---

## 13. Local Podman/Docker development model

PRD basis: OCI-compatible images, Podman preferred with Docker compatibility retained (NFR-02, §13). No Kubernetes (Non-Goals §5, §16).

Proposal for v0.1:

- **PostgreSQL for local dev** via a repository-level `compose.yaml` (root of the repo or `backend/` — decided in ADR-01). Run with `podman compose up -d` or `docker compose up -d` interchangeably (NFR-02). Throwaway local credentials only.
- **Backend dev**: `./mvnw spring-boot:run` against the local PostgreSQL; Flyway applies module schemas on startup (ADR-03 wiring).
- **Frontend dev**: `npm run dev` (`next dev`) against the local backend; no container needed for frontend development. Containerizing the frontend is a deployment concern for customer projects, not a v0.1 baseline item.
- **Tests**: the baseline already wires Testcontainers PostgreSQL (`TestcontainersConfiguration`). Testcontainers under Podman typically needs a Docker-compatible socket / `TESTCONTAINERS_*` environment configuration; the exact incantation is verified during reference-implementation work and recorded in dev docs (evidence-gated, not designed now).
- **OCI images**: the Spring Boot Maven plugin can produce OCI images (buildpacks) and works with Podman via the Podman socket; alternatively a plain Dockerfile. The choice (**ADR-13**) is made when the first deployment artifact is actually needed — v0.1 does not commit.

---

## 14. Testing boundaries

PRD basis: framework/modules support automated unit/integration testing appropriate to responsibilities (NFR-03); tests are quality gates, not proof of correctness — generated tests require human review against requirements (NFR-03, §12); V1 acceptance requires automated tests of module boundary, removal behavior, version compatibility, persistence ownership, and reference extension (§17).

| Layer | What is tested | Where |
|---|---|---|
| Unit | Framework contracts; module services/validation in isolation, neighbors mocked via contracts | `src/test` inside each artifact |
| Module integration | Module persistence against real PostgreSQL (Testcontainers, already wired in the baseline) — proves schema ownership + migrations | `src/test` inside each module |
| Boundary | Package-import rules (§5); no cross-module persistence access; api/internal separation | Per-artifact boundary tests |
| Removal | Build + start a customer-style app without a module — must still compile, start, and function (FR-02) | CI matrix on the reference app |
| Compatibility | Reference app runs against released artifacts for each approved SBP release — the regression gate for the BOM | CI pipeline on the platform release |
| Reference extension | Customer-style extension of the proof module without editing released source (§14) | Reference app tests |

Guidance:

- Test pyramid: mostly unit tests, targeted integration tests, and a small number of full-context `@SpringBootTest` tests — kept at the reference-application level rather than duplicated per module.
- Passing generated tests do not by themselves establish correctness; behavior is reviewed against requirements and realistic scenarios (§12).
- No test-scope tooling beyond the existing baseline (Testcontainers, JUnit, Spring test starters) is required for v0.1; enforcement tooling (ArchUnit/Modulith) is a later, evidence-driven decision (**ADR-08**).

---

## 15. Reference implementation structure

### 15.1 Shape

A **customer-style project** (`reference/`, own `pom.xml`, own coordinates and package namespace, e.g. `com.acmeco.reference`) that:

- Consumes **released/versioned SBP artifacts** (framework + selected modules) via `sbp-bom`, pinned — *not* framework source internals (V1 scope, FR-25). In dev, artifacts come from the local Maven repository (`mvn install`); in CI, from the repository strategy in place (ADR-11).
- Selects **Customer** (the proof module, Appendix B) and **Contact** (Appendix B core baseline) as the v1 module set — subject to the scope confirmation in §18.
- Implements a **development/demo security policy** at the application layer through the framework security boundary (FR-09, §16/§18 "reference security implementation" open decision) — clearly not customer policy.
- Adds a **customer-owned extension** proving FR-15/FR-18: e.g. an AcmeCo profile table keyed by `CustomerId`, a wrapper service adding Acme behavior, and a customer-specific API (FR-22) — all without editing released module source.
- Carries a **customized copy of `sbp-ui`** (branding, AcmeCo presentation, module UI folders) proving the frontend template model (FR-26).
- Owns its **secrets and configuration** like a real customer project.

### 15.2 What it must prove (V1 acceptance criteria mapping)

| Acceptance criterion (§17) | Proved by |
|---|---|
| Assemble and start app with framework + proof module | Reference app boots against local PostgreSQL |
| Proof module physically removable; remaining baseline builds/starts | Removal build variant (§14) |
| Absent capability cannot be enabled by configuration | Config surface test: no switches for removed modules |
| Proof module exposes functional baseline backend + basic UI | Customer module api + template module UI |
| Extend around the proof module without editing released source | AcmeCo extension (§15.1) |
| Customer security policy via the defined boundary | Reference security impl wired through FR-09 boundary |
| Automated tests of boundary, removal, compatibility, persistence ownership, extension | Test layers in §14 |
| Remains a modular monolith; no distributed service to enforce boundaries | Architecture review of the reference app |

### 15.3 Relationship to the delivery loop

The reference app is the second step of the guiding loop (baseline → reference application → real customer …; §19). It is maintained as a regression/compatibility vehicle for every approved release, and as the living example that customer projects copy the *mechanics* from (not the code — FR-13).

---

## 16. Proposed ADRs needed before implementation

Each ADR below names the decision, its PRD trace, and when it must be written. ADRs 01–04 are prerequisites for the first implementation slice; others are triggered by specific evidence.

| # | ADR | Decision to record | PRD trace | Timing |
|---|---|---|---|---|
| ADR-01 | Maven reactor & artifact split | Exact reactor layout (per §3); which baseline starters belong to which artifact; where the boot plugin lives; dev `compose.yaml` placement | FR-02, FR-25, §13/§18 | Before restructuring `backend/sbp` |
| ADR-02 | Module discovery/registration | Auto-configuration (`AutoConfiguration.imports`) as the module-activation mechanism; framework package layout; Spring Modulith evaluation outcome | FR-02, FR-04, §18 | Before framework skeleton |
| ADR-03 | Persistence & Flyway ownership | Model A vs B (§8); per-schema Flyway wiring; cross-schema FK policy; customer schema naming | FR-18, FR-19, FR-20, §18 | Before proof module persistence |
| ADR-04 | Cross-module contract & event conventions | Contents of `api` packages; typed-ID convention; event convention and sync/async rule (§9) | FR-03, FR-20, §18 | With first two modules |
| ADR-05 | Versioning, BOM, release baseline | SemVer policy; `sbp-bom` mechanics; pinning/override rules; first-release process | §14, FR-25 | Before first release |
| ADR-06 | Security integration boundary | Shape of the FR-09 boundary; reference dev/demo security implementation | FR-09, §18 | Before reference app security |
| ADR-07 | Extension data mapping | Customer-owned extension tables: FK vs ID-only reference to module identities; naming | FR-15, FR-18, §18 | With reference extension |
| ADR-08 | Boundary enforcement tooling | Adoption trigger for ArchUnit/Modulith; review checklist until then | FR-03, NFR-01, §18 | When first violation recurs |
| ADR-09 | Frontend template & module UI delivery | Template versioning; module UI folder convention; branding token mechanism | FR-07, FR-08, FR-26 | Before first module UI |
| ADR-10 | Storage abstraction shape | FR-11 SPI shape and timing (tied to Document module or demonstrated need) | FR-11, §18 | When Document module is selected |
| ADR-11 | Artifact repository strategy | Local repo for v0.1; private repo trigger and candidate products | §14, §18 | First external distribution need |
| ADR-12 | Customer project assembly mechanics | Generator vs documented copy; what the reference app contributes as a template | §10, §18 | Before second customer project |
| ADR-13 | Container build & runtime images | OCI build approach (buildpacks vs Dockerfile); Podman-first verification | NFR-02, §13 | Before first deployment artifact |

---

## 17. Deliberately unresolved decisions (remain open until evidence)

The following are intentionally **not** resolved in v0.1; the trigger column names the evidence that should resolve them. Resolving them now would violate P-02 / P-11 ("repetition earns abstraction", "real requirements over speculation").

| Open item | Why left open | Trigger to resolve |
|---|---|---|
| Spring Modulith adoption | PRD §18 says evaluate, don't assume | A recurring boundary violation or demonstrable review cost |
| Async in-process events | No current consumer; sync default suffices | A reference-app scenario where sync delivery creates real coupling |
| Storage abstraction SPI details (FR-11) | No Document module in v1; no storage consumer exists | Document module selected or a module needs object storage |
| Module UI packaging beyond template folders | Only one module UI exists (none yet) | Second module UI or a customer needing independent UI versioning |
| Customer-project generator/template mechanics | One reference app is not repetition; documented assembly may suffice | Second customer project starts |
| Private artifact repository | Local Maven repo suffices for one team | First distribution beyond the team / second consumer |
| ArchUnit/enforcer tooling | No boundary violation observed yet | First violation recurs despite review |
| Full upgrade/compatibility matrix | No real version bump has happened yet | First framework or module version bump beyond 0.0.1-SNAPSHOT |
| Repository topology (single vs multiple repos) | Single repo works for v0.1; PRD §18 explicitly open | First external collaborator or independent consumer |
| Reference security implementation details | Policy is customer-owned by definition (FR-09); only a dev/demo impl is needed | Reference app security slice |
| IP/source ownership model | Contractual, not technical (§18); out of scope for a technical architecture | Commercial/legal review — explicitly **not** an architecture decision |

---

## 18. Mapping to PRD §18 open decisions

| PRD §18 open decision | v0.1 stance |
|---|---|
| Exact Maven/Gradle multi-module and repository topology | **Proposed** — §3: Maven reactor, `sbp-parent`/`sbp-framework`/`sbp-module-*`/`sbp-bom`; reference app outside the reactor. ADR-01. |
| Whether reusable artifacts live in one platform repository or multiple | **Open** (§17) — single repo for v0.1; revisit on evidence |
| Private artifact repository strategy | **Open** (§17, ADR-11) — local `mvn install` suffices for v0.1 |
| Exact Spring module discovery and extension mechanism; evaluate Spring Modulith | **Proposed** — D3 auto-configuration inclusion; Modulith evaluation gated (§17, ADR-02) |
| Frontend packaging strategy for reusable Next.js/React module UI | **Proposed** — template-copy model (§11); module UI in template folders; packaging only on evidence |
| Database migration ownership and ordering when modules are independently packaged | **Proposed** — Model A schema-per-module with per-schema Flyway histories (§8); evidence-gated pivot to Model B (ADR-03) |
| Reference security implementation used for development and demonstrations | **Proposed** — minimal dev/demo impl at the application layer through the FR-09 boundary (ADR-06, §17) |
| Reference proof module to implement first | **Proposed** — Customer (Appendix B: "proof-module candidate") (§15) |
| Customer-project generator/template mechanics | **Open** (§17, ADR-12) — documented assembly + reference app as example |
| Upgrade and compatibility rules between framework, modules, and customer extensions | **Partially proposed** — pinning via BOM (§10); full matrix open until first bump (§17) |
| Persistence mapping pattern for baseline identities plus customer-owned extension data | **Proposed** — customer-owned tables keyed by typed identifiers; FK vs ID-only decided in ADR-07 against reference evidence (§8, §7) |
| Cross-module contract and in-process event conventions | **Proposed** — §9; confirmed by ADR-04 |
| Platform BOM/release-baseline strategy | **Proposed** — `sbp-bom` as release baseline, reference app as compatibility gate (§10, §15) |
| IP/source ownership model | **Out of scope** — contractual (§17) |

---

## 19. PRD observations, ambiguities, and conflicts

1. **v1 module set (Appendix B vs §15).** Appendix B lists Contact as "Core baseline — Yes (in v1 baseline)" while §15 mandates only "at least one simple reusable business module" with Customer as the proof candidate. It is ambiguous whether **Contact ships in the v1 baseline** or whether only Customer does. The architecture assumes Customer + Contact for the reference app but this needs product confirmation. **OPEN (product decision).**
2. **Generated starter set exceeds PRD v1 scope.** The baseline pom includes batch and mail starters the PRD never requires. This is a generator artifact, not a requirement; ADR-01 decides what each artifact actually needs. No action now.
3. **FR-07 vs FR-26 (module UI in a template-copy world).** "An installed reusable business module SHOULD provide a basic functional UI" is written for a packaging world, but FR-26 mandates copy-based frontend reuse. "Installed" must be reinterpreted as "kept in the customer's template copy." The §11.2 folder convention is a proposal needing confirmation in ADR-09.
4. **§14 sequencing.** "Compatibility, migration, and upgrade policy will be defined in the technical architecture after the initial packaging model is selected." v0.1 selects the packaging model (§3) and proposes the BOM/pinning mechanism (§10); the detailed compatibility matrix remains open (§17) — consistent with §14, not a conflict.
5. **FR-11 storage abstraction vs v1 module set.** The framework SHOULD provide a storage abstraction, but no v1 module (Document is a candidate) consumes storage. The contract area is acknowledged, concrete design deferred until a consumer exists (§6.3, §17) — a deliberate sequencing choice, flagged for reviewer awareness.
6. **FR-10 audit hooks scope.** The PRD requires hooks but not a mechanism; the mechanism choice (annotation, event, callback) is deferred to ADR-06 with the first consumer. Not a conflict, but reviewers should confirm the minimal expectation for v1.

---

*End of Technical Architecture v0.1 — draft for review. Superseded sections are updated via ADR records, not by editing this document in place.*
