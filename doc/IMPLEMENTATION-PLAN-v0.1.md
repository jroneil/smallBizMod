# Small Business Platform (SBP) — Implementation Plan v0.1

**Status:** Draft for review  
**Authority:** SBP PRD v0.4 baseline + merged Technical Architecture v0.1  
**Primary objective:** Produce a visible, usable proof of the SBP framework quickly enough to validate the architecture through real implementation.

---

## 1. Implementation philosophy

SBP will be implemented as a sequence of small, demonstrable vertical slices.

The purpose of the first implementation is **not** to maximize framework completeness or test coverage. It is to prove that the framework can produce a useful customer-style application without unnecessary architectural friction.

The implementation shall favor:

- visible working behavior;
- simple architecture;
- clear module boundaries;
- realistic customer extension;
- fast feedback from the reference application;
- tests where they protect meaningful behavior or architecture;
- changing the framework when implementation proves an assumption wrong.

The implementation shall avoid turning test infrastructure, abstraction, or governance into the product.

A passing test suite is a quality signal, not proof that the framework design is useful.

---

## 2. V0.1 proof target

The first proof shall demonstrate this minimum system:

```text
SBP framework JAR
        +
Customer proof-module JAR
        +
Reference customer backend
        +
Reference customer frontend
        +
PostgreSQL
```

The reference application shall provide a small but visible Customer flow:

```text
Open application
      ↓
View customers
      ↓
Create customer
      ↓
View customer detail
      ↓
Add customer-specific reference-app data
      ↓
Persist and reload
```

The reference application is the primary architecture test.

If the framework makes this flow harder than a normal Spring Boot + Next.js application should be, the framework shall be simplified.

This target describes the first Customer proof, not the full V1 baseline.
After that proof is accepted, the completed V1 baseline also requires the
reusable Contact capability identified by the PRD. Contact represents a person
associated with a Customer. It is distinct from the customer-owned Acme Service
Profile.

---

## 3. Implementation rules

1. Work one authorized slice at a time.
2. Each slice must produce either visible functionality or remove a concrete blocker to visible functionality.
3. Do not implement additional modules until the first proof is complete.
   After that proof, implement the PRD-required Contact baseline before calling
   V1 complete; other candidate modules still require separate authorization.
4. Do not add extension points without a demonstrated consumer.
5. Do not add infrastructure merely because it may be useful later.
6. Do not add tests only to increase coverage metrics.
7. Add tests primarily where they protect:
   - public module contracts;
   - persistence/migrations;
   - module-removal behavior;
   - customer-extension boundaries;
   - realistic user-visible behavior.
8. Generated tests shall be reviewed against real behavior.
9. The reference application may invalidate architecture decisions.
10. When architecture and implementation conflict, prefer the simplest design that still satisfies the PRD.

---

## 4. Architecture gates

Before implementation modifies the generated backend structure, resolve the minimum decisions needed to begin.

### Gate A — ADR-001: Backend artifact/repository topology

Decide:

- Maven parent/aggregator structure;
- `sbp-framework` artifact;
- `sbp-module-customer` artifact;
- executable reference application location;
- dependency ownership;
- Spring Boot plugin placement.

**Exit condition:** an agreed project tree that can be implemented without guessing.

### Gate B — ADR-002: Spring module assembly

Decide only enough to prove:

- framework JAR can be consumed;
- Customer JAR can be consumed;
- Customer can be omitted;
- customer application owns the executable Spring Boot entry point.

Do not build a generic plugin or module-discovery system.

### Gate C — ADR-003: Persistence/Flyway proof strategy

Choose the smallest persistence model worth testing first.

The ADR must explicitly compare:

- schema-per-module;
- shared schema with module-owned tables/migrations.

The decision may be provisional and subject to the reference implementation.

### Gate D — ADR-004: Reference scenario

Ratify:

- Customer as first proof module;
- fictional reference customer;
- one customer-specific extension;
- minimum visible flow.

**Gate rule:** ADRs should be concise. They exist to unblock implementation, not to become a second architecture project.

---

# 5. Implementation slices

## Slice 0 — Preserve generated baselines

### Objective

Establish a clean checkpoint before framework restructuring.

### Work

- confirm Spring Boot baseline builds;
- confirm Next.js baseline builds;
- remove Lombok if still present;
- commit current generated projects;
- keep PRD and architecture docs in source control.

### Acceptance

- backend builds;
- frontend production build succeeds;
- clean baseline commit exists.

### Not authorized

- framework implementation;
- Customer domain implementation;
- frontend redesign;
- new infrastructure.

---

## Slice 1 — Create backend artifact structure

### Objective

Turn the single generated backend into the minimum reusable artifact structure.

### Target

```text
backend/
  sbp/
    pom.xml
    sbp-framework/
    sbp-module-customer/

reference/
  acme/
    backend/
```

Exact names follow ADR-001.

### Work

- create Maven aggregator/parent as approved;
- create ordinary framework JAR;
- create ordinary Customer module JAR;
- create independent executable reference backend;
- wire reference backend to consume framework + Customer.

### Visible proof

Reference backend starts successfully with the reusable artifacts present.

### Essential tests only

- Maven build;
- one Spring context/startup test if useful;
- one build/start variant proving Customer can be omitted.

### Stop condition

If module wiring requires a plugin framework, dynamic registry, extensive reflection, or substantial Spring magic, stop and simplify.

---

## Slice 2 — Minimal Customer backend

### Objective

Build the smallest useful reusable Customer capability.

### Initial Customer scope

Keep the baseline intentionally small.

Likely fields:

```text
Customer
- id
- name
- status
- createdAt / updatedAt only if justified by the chosen audit/lifecycle conventions
```

Exact domain shape should be decided during the slice, not overdesigned beforehand.

### Work

- Customer public API/contract;
- Customer internal application/domain implementation;
- JPA persistence;
- Flyway migration;
- REST endpoints needed by the reference UI.

### Minimum behavior

- create Customer;
- list Customers;
- get Customer;
- update basic Customer information if needed for the reference flow.

### Essential tests

- Customer persistence against PostgreSQL/Testcontainers;
- migration creates a clean database;
- one or two contract/application behavior tests.

### Avoid

- generic contact model in the Customer proof slice (Contact is implemented as
  its own reusable module after the proof);
- address framework;
- custom-field framework;
- event publishing;
- search DSL;
- pagination abstraction unless the UI needs it;
- generic lifecycle engine.

---

## Slice 3 — Reference customer extension

### Objective

Prove that a real customer can add useful behavior around Customer without modifying SBP Customer source.

### Reference concept

Use a fictional small service/repair company such as Acme Service & Repair.

Example extension:

```text
AcmeCustomerProfile
- customerId
- serviceRegion
- preferredTechnician
- accountNotes
```

The exact fields are illustrative. Use only enough to prove the architecture.

### Work

- customer-owned persistence/table;
- customer-owned migration;
- customer-owned service composing Customer + Acme profile;
- one customer-specific REST operation or enriched view.

### Visible proof

The reference application can create/view a Customer plus Acme-specific information.

### Essential tests

- Acme extension persists independently;
- no Acme column is added to the SBP Customer table;
- Acme does not access the Customer repository directly.

### Architecture check

If extending Customer requires awkward adapters or too many interfaces, revise the framework.

---

## Slice 4 — Basic SBP frontend baseline

### Objective

Turn the generated Next.js application into a basic reusable frontend baseline without building a design system.

### Work

Create only what the first Customer screen requires:

- application shell;
- navigation;
- basic page layout;
- branding seam;
- API access convention;
- basic table/list;
- basic form;
- basic detail view;
- error/loading states.

### Visual goal

It should look clean and professional enough to demonstrate, but not customer-polished.

### Avoid

- component library project;
- shared npm packages;
- schema-driven forms;
- generic page generator;
- theme engine beyond simple branding variables;
- elaborate dashboard.

---

## Slice 5 — Reference Customer UI

### Objective

Produce the first end-to-end visible application.

### Work

Copy the approved `sbp-ui` baseline into the reference customer frontend and customize it.

Implement:

- Customer list;
- Create Customer;
- Customer detail;
- Acme-specific profile section;
- simple branding difference proving the copy/customize model.

### Demo flow

A reviewer should be able to:

1. open the reference application;
2. create a customer;
3. see the customer in the list;
4. open the detail page;
5. add/edit Acme-specific information;
6. reload and see persisted data.

### Acceptance priority

The demo flow working correctly matters more than adding broad UI test coverage.

### Tests

Use only the minimum needed at this point:

- frontend type check;
- lint;
- production build;
- add focused component/browser tests only for behavior that proves difficult or regression-prone.

---

## Slice 6 — Local Podman/Docker environment

### Objective

Make the working proof easy to run locally.

### Work

Add portable Compose configuration for the minimum required services.

Initial target:

```text
PostgreSQL container

developer processes:
- Spring Boot reference backend
- Next.js reference frontend
```

Optionally add full frontend/backend containers only when integration/deployment testing needs them.

### Acceptance

Documented commands can bring up the development database using:

```text
podman compose up
```

and remain Docker-compatible.

### Avoid

- Kubernetes;
- proxy layers unless needed;
- local identity infrastructure unless required by the reference security slice;
- monitoring stack.

---

## Slice 7 — Minimal reference security

### Objective

Prove the security boundary without designing customer security policy.

### Work

Implement only enough development/demo security to show:

- the customer application owns security configuration;
- SBP modules do not hard-code company roles/policy;
- the Customer flow remains usable.

### Avoid

Unless required by the reference scenario:

- SSO;
- OAuth-provider-specific integration;
- MFA;
- universal RBAC/ABAC;
- permission administration UI.

---

## Slice 8 — Architecture proof and cleanup

### Objective

Review what the working reference app taught us.

### Review questions

- Was `sbp-framework` actually useful, or did it accumulate unnecessary abstractions?
- Is Customer independently removable?
- Is the public API/internal split practical?
- Is customer extension easy?
- Did persistence ownership help or hinder normal relational modeling?
- Is the chosen Flyway approach simple enough?
- Did the frontend copy model work naturally?
- Did any module abstraction exist without a real consumer?
- Which generated dependencies are unused?
- What should be removed before calling this a baseline?

### Required outcome

Simplify the implementation before adding more modules.

Do not preserve an abstraction merely because tests exist for it.

---

## Slice 9 — First baseline release

### Objective

Create the first internal SBP baseline that can serve as the starting point for later modules/customer projects.

### Work

- remove unused dependencies;
- document framework/module API boundaries;
- document how to run the reference app;
- tag/version reusable artifacts;
- record reference frontend baseline version;
- ensure the reference application consumes reusable artifacts rather than source internals.

### Not required yet

- private artifact repository if local Maven install is sufficient;
- BOM;
- generator;
- automated frontend updater;
- extensive compatibility matrix.

---

## Slice 10 — Required V1 Contact baseline

### Objective

Complete the PRD-required V1 business baseline after the Customer architecture
proof by adding a small reusable Contact capability.

### Domain boundary

A Contact is a person associated with a Customer. It contains only generally
useful person/contact information and a lifecycle status. The Acme Service
Profile remains separate customer-owned service/account information.

### Work

- independently versioned `sbp-module-contact` JAR;
- explicit dependency on the Customer public contract;
- Contact-owned persistence and Flyway migration;
- create, retrieve, and list-by-Customer public/REST operations;
- basic reusable Contact UI on Customer detail;
- copied Contact UI in Acme alongside the Service Profile;
- focused module, omission, integration, and realistic browser verification.

### Avoid

- generic CRM, search, address, workflow, or custom-field frameworks;
- Customer persistence internals or direct Customer-table access;
- Acme policy in the reusable Contact module;
- interpreting the Acme Service Profile as Contact data.

---

# 6. Testing strategy

Testing follows implementation rather than driving the product.

## Required early tests

Use automated tests where failure would undermine the architecture:

- module builds with declared dependencies;
- Customer persistence works against PostgreSQL;
- Flyway builds a clean database;
- customer extension persists separately;
- optional Customer module can be omitted;
- Contact persists, lists by Customer, and validates Customer existence through
  the public Customer contract;
- optional Contact can be omitted without breaking the remaining valid Acme
  composition;
- reference app integrates through public contracts;
- frontend builds and the primary demo flow works.

## Tests that may wait

Do not front-load:

- exhaustive controller permutations;
- broad DTO getter/setter tests;
- tests proving framework implementation details;
- coverage-driven tests;
- exhaustive validation combinations before real usage;
- large mock-heavy suites that duplicate implementation structure.

## Realistic scenario rule

At the end of the first proof, the strongest acceptance test is the working reference flow:

```text
create Customer
→ persist
→ retrieve
→ enrich with Acme data
→ display in copied/customized UI
```

If that works naturally and survives realistic use, the architecture has evidence in its favor.

---

# 7. Demonstration checkpoints

The project should remain demonstrable throughout implementation.

### Demo 1

Reference Spring application boots using reusable JARs.

### Demo 2

Customer can be created and retrieved through the backend.

### Demo 3

Acme extension works without changing Customer module source.

### Demo 4

Customer list/create/detail works in the browser.

### Demo 5

Acme-specific UI and persistence work end-to-end.

### Demo 6

A clean clone can run the environment using documented Podman/Docker steps.

### Demo 7

The completed V1 reference application can add and reload a Contact person on a
Customer while independently creating or editing the Acme Service Profile.

These checkpoints matter more than accumulating infrastructure that cannot yet be shown.

---

# 8. Explicitly not authorized before the first proof

Do not implement the following until the reference application demonstrates a need or the first baseline is accepted:

- Contact as a separate reusable module;
- Job / Work Order;
- Document;
- Scheduling;
- Estimates;
- Invoicing;
- Inventory;
- Reporting;
- workflow engine;
- rules engine;
- generic custom fields;
- plugin architecture;
- dynamic module loader;
- event framework;
- message broker;
- runtime licensing;
- anti-tamper system;
- microservices;
- Kubernetes;
- generic customer-project generator;
- module generator;
- private artifact infrastructure unless distribution requires it;
- BOM unless version coordination becomes real;
- frontend npm-package extraction.

For Contact, this list is sequencing only. Once the Customer proof is accepted,
Contact is authorized and required for completed V1. The remaining items stay
deferred until their own requirements or evidence justify them.

---

# 9. Promotion after proof

After the first reference application works, evaluate the next capability from actual need.

Contact is not an optional promotion decision in this step: the PRD already
identifies it as part of the V1 baseline. This promotion rule governs later
customer-specific behavior and other candidate capabilities.

A feature becomes reusable only when:

1. it appears broadly applicable; or
2. implementation repetition demonstrates reuse; and
3. it can be generalized without carrying customer policy into SBP.

The expected growth loop remains:

```text
SBP baseline
   ↓
reference / real customer implementation
   ↓
customer-specific functionality
   ↓
observe repetition
   ↓
generalize
   ↓
promote to SBP
   ↓
version/release
```

---

# 10. Agent execution model

Each agent prompt should authorize exactly one slice.

Every implementation prompt should include:

- authoritative PRD/architecture/ADR references;
- objective;
- files/areas allowed to change;
- explicit non-goals;
- visible behavior expected;
- minimum required tests;
- stop conditions;
- completion report format.

Agents must not advance automatically to the next slice.

A slice completion report should answer:

1. What visible behavior now works?
2. What files changed?
3. What architecture assumptions were validated?
4. What assumptions were challenged?
5. What tests were added and why?
6. What was deliberately not implemented?
7. Is the next slice still appropriate, or should architecture change first?

---

# 11. Success condition for Implementation Plan v0.1

This plan succeeds when SBP has produced one small, working, customer-style application that demonstrates:

- reusable backend framework/module artifacts;
- customer-specific extension without forking;
- practical persistence ownership;
- a copied/customized frontend;
- local reproducible development;
- enough tests to protect the meaningful boundaries;
- and, most importantly, a visible application that is easier to build because SBP exists.

That condition completes the first architecture proof. Completed V1 additionally
requires the reusable Contact module and basic Contact UI described in Slice 10,
with Contact and the Acme Service Profile visible as separate concepts. Only
after those outcomes should the platform expand to other modules.
