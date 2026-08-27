# ADR-JOB-001: Job module boundary, terminology, and dependencies

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

The SBP PRD (v0.4 baseline) lists "Job / Work Order" as a core-baseline candidate
module in Appendix B, and names Customer, Contact, Job, and Document as the
generic baseline capabilities (FR-14). The PRD defers a detailed module PRD until
the module is selected for implementation (Section 9), and the Technical
Architecture and Implementation Plan v0.1 both keep Job explicitly out of the
first proof and the V1 baseline (Contact is the only PRD-required module added
after the Customer proof). Job is now being selected for requirements and
architecture definition.

The existing reusable modules establish the pattern this decision must follow:
independently versioned JARs, a public `api`/`internal` package split, standard
Spring Boot auto-configuration (ADR-002), module-owned persistence and Flyway
migrations in one shared history (ADR-003, ADR-006), validation of cross-module
references only through the owning module's public contract with no cross-module
database foreign key (ADR-006), and customer-owned security policy (ADR-005).

Two naming/identity questions must be settled before the domain model can be
defined: whether "Job" and "Work Order" are one concept or two, and what the
reusable module's dependency graph must be.

## Decision

### Terminology: one concept, one entity

- There is **one** reusable concept and **one** entity. The canonical entity and
  capability name is **Job**.
- "Work Order" is customer-facing industry terminology for the same concept
  (common in repair, field-service, and contractor businesses). It is **not** a
  second entity and does not introduce a second module, table, or API surface.
- Customer projects MAY relabel the concept in their own copied frontend (e.g.,
  Acme displaying "Work Order") through normal copy-and-customize and branding
  seams (Level 1 configuration). No reusable-module change is required for a
  customer to use different terminology.
- This matches the PRD, which lists a single "Job / Work Order" module row in
  Appendix B and uses "Job" as the canonical term in FR-14 and the problem
  statement, and it avoids the anti-pattern of creating two entities merely
  because two terms exist.

### Module artifact and package topology

- The reusable artifact is `com.oneilsoftware:sbp-module-job:0.1.0-SNAPSHOT`, an
  ordinary JAR inside the existing Maven reactor.
- The package root is `com.oneilsoftware.sbp.module.job`, with the public
  contract under `api` and implementation under `internal.application`,
  `internal.persistence`, `internal.web`, and `internal`, mirroring Customer,
  Contact, and User Maintenance.
- Standard Spring Boot auto-configuration activates the module when its JAR is
  present (ADR-002). No registry, discovery, or implementation-package scanning
  is introduced.

### Dependencies

```text
sbp-framework (audit contract)
      ↑
sbp-module-customer (public contract only)
      ↑
sbp-module-job
```

- `sbp-module-job` depends on `sbp-framework` for the audit hook contract
  (FR-10) and on `sbp-module-customer` for the public `CustomerOperations`
  contract.
- The Customer dependency exists because the Job concept is defined as work
  performed for or on behalf of a Customer (PRD problem statement; module
  mission). The dependency is on the public contract only: Job validates
  Customer identifiers through `CustomerOperations` and never accesses Customer
  repositories, entities, tables, or `internal` packages.
- Job stores the stable Customer UUID in its own table and creates **no**
  cross-module database foreign key, exactly as Contact does (ADR-006).
- Job deliberately does **not** depend on `sbp-module-contact` or
  `sbp-module-user-maintenance`. Neither relationship is part of the v0.1
  baseline (see ADR-JOB-002), and adding either dependency would reduce Job's
  independent removability without a demonstrated requirement.

### Customer ownership

- A Job SHALL reference exactly one Customer in v0.1. A Job may not exist
  without its Customer reference; nullable-customer jobs are deferred until a
  demonstrated requirement (e.g., internal work orders) justifies them.
- Creating or editing a Job with an unknown Customer id is rejected through a
  Job-module-owned `UnknownCustomerException` mapped to 404, mirroring the
  Contact pattern.

### Removability

- Removing the Job Maven dependency removes the Job capability without changing
  application source and without breaking Customer, Contact, or User
  Maintenance, consistent with FR-02 and the Contact/User Maintenance removal
  proofs.
- Selecting Job requires selecting Customer, because Job validates through the
  Customer public contract (the same constraint ADR-006 records for Contact).
  This is a product-contract consequence, not an infrastructure constraint.

### Acme extension proof

- Acme will not build a second customer-owned persistence-extension proof. The
  Acme Customer Service Profile already proved the composition-around-a-stable-
  identity pattern (customer-owned table, migration, service, and API), so a Job
  data extension would add no new architectural evidence.
- Acme instead demonstrates the customer-owned surface that Job does create:
  copied Job UI with Acme branding, customer-owned terminology ("Work Order"),
  and customer-owned permission keys (`acme.job.read`, `acme.job.manage`). A
  data extension (service type, equipment, access instructions, work
  classification) remains available if a future slice needs to prove something
  new; it is not built for symmetry.

## Consequences

- The PRD's "Job / Work Order" module is realized as one Job module; Work Order
  remains a label, not a design element.
- Job follows the exact module-ownership pattern proven by Contact, so the
  established architecture (ADR-002/003/006) is reused rather than extended.
- Job's dependency graph is the minimum that expresses its product contract:
  framework + Customer public contract. It stays independent of Contact and User
  Maintenance, preserving its removability.
- A customer needing assignment, contact-person, document, or note association
  before the baseline provides it composes customer-owned data around the stable
  Job id (the proven extension model) or waits for promotion evidence.
