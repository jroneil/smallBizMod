# ADR-DOC-001: Document module boundary, terminology, and dependencies

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

The SBP PRD (v0.4 baseline) lists "Document" as a core-baseline candidate
module in Appendix B and names Customer, Contact, Job, and Document as the
generic baseline capabilities (FR-14 and the Section 2 problem statement). The
PRD defers a detailed module PRD until the module is selected (Section 9), and
the Technical Architecture and Implementation Plan v0.1 both keep Document out
of the first proof and the V1 baseline. Document is now being selected for
requirements and architecture definition, which also activates the deferred
FR-11 storage-abstraction decision (see ADR-DOC-002).

The existing reusable modules establish the pattern this decision must follow:
independently versioned JARs, a public `api`/`internal` package split, standard
Spring Boot auto-configuration (ADR-002), module-owned persistence and Flyway
migrations in one shared history (ADR-003, ADR-006), validation of cross-module
references only through the owning module's public contract with no cross-module
database foreign key (ADR-006, ADR-JOB-001), and customer-owned security policy
(ADR-005).

Two questions must be settled before the domain model can be defined: what the
reusable "Document" concept is, and which business records it attaches to and
with what dependency graph. On attachment, ADR-JOB-002 recorded that "a future
Document capability attaches by jobId (owner-style references or a generic
owner model are the Document module's choice) without any Job redesign", and
the Job PRD supplement (Section 10.4) recorded that Documents attach to Jobs by
jobId and to Customers by customerId. The mechanism is therefore this module's
decision; both owner types are pre-recorded intent.

## Decision

### Concept: one entity, metadata plus content

- There is **one** reusable concept and **one** entity. The canonical entity and
  capability name is **Document** (matching the PRD's Appendix B row and FR-14).
- A Document is a stored file (**content**) plus the descriptive **metadata**
  needed to find, display, download, replace, and remove it (display name,
  advisory content type, byte size, owner reference, timestamps). Metadata and
  content are separate concerns: metadata is module-owned relational data;
  content is stored only through the framework storage abstraction
  (ADR-DOC-002, framework FR-11).
- "Attachment" and "File" are customer-facing synonyms for the same concept.
  Customer projects MAY relabel the concept in their own copied frontend
  through normal copy-and-customize and branding seams (Level 1
  configuration). No reusable-module change is required for a customer to use
  different terminology.

### Module artifact and package topology

- The reusable artifact is `com.oneilsoftware:sbp-module-document:0.1.0-SNAPSHOT`,
  an ordinary JAR inside the existing Maven reactor.
- The package root is `com.oneilsoftware.sbp.module.document`, with the public
  contract under `api` and implementation under `internal.application`,
  `internal.persistence`, `internal.storage`, `internal.web`, and `internal`,
  mirroring Customer, Contact, User Maintenance, and Job.
- Standard Spring Boot auto-configuration activates the module when its JAR is
  present (ADR-002). No registry, discovery, or implementation-package scanning
  is introduced.

### Association model: explicit owner references, exactly one owner

- A Document SHALL reference exactly one owner: either a Customer or a Job, in
  v0.1. The schema uses two explicit nullable UUID columns, `customer_id` and
  `job_id`, with a CHECK constraint requiring exactly one of them. A Document
  MAY NOT exist without an owner in v0.1.
- A **generic owner model** (`owner_type` + `owner_id` with no module
  dependency) is rejected: it has no demonstrated consumer beyond the two
  recorded owner types, it would either leave owner existence unvalidated or
  require a speculative owner-validation extension point, and it weakens
  relational integrity. Explicit columns keep validation in the module and
  follow the established "owner-style references" option named in ADR-JOB-002.
- One Document content SHALL NOT be shared across multiple owners in v0.1
  (no multi-owner model). Re-uploading a file where a second association is
  needed is the v0.1 behavior; a document-link join is a purely additive future
  seam.

### Dependencies

```text
sbp-framework (audit hook + storage abstraction)
      ↑
sbp-module-customer (public contract only)
      ↑
sbp-module-job (public contract only)
      ↑
sbp-module-document
```

- `sbp-module-document` depends on `sbp-framework` for the audit hook contract
  (FR-10) and the storage abstraction contract (FR-11), and on the public
  `CustomerOperations` and `JobOperations` contracts.
- The Customer and Job dependencies exist because both are recorded owner types
  of a Document and every create must validate its owner. The dependencies are
  on the public contracts only: Document validates owner identifiers through
  `CustomerOperations.findById` and `JobOperations.findById` and never accesses
  Customer or Job repositories, entities, tables, or `internal` packages.
- Document stores the stable owner UUIDs in its own table and creates **no**
  cross-module database foreign keys, exactly as Contact and Job do (ADR-006).
- Document deliberately does **not** depend on `sbp-module-contact` or
  `sbp-module-user-maintenance`. Neither association is part of the v0.1
  baseline (see ADR-DOC-003), and adding either dependency would reduce
  Document's independent removability without a demonstrated requirement.

### Owner validation

- Creating or editing a Document with an unknown owner identifier is rejected
  through a Document-module-owned `UnknownDocumentOwnerException` mapped to 404,
  mirroring the Contact/Job unknown-reference pattern.

### Removability

- Removing the Document Maven dependency removes the Document capability without
  changing application source and without breaking Customer, Contact, Job, or
  User Maintenance, consistent with FR-02 and the Contact/User Maintenance/Job
  removal proofs.
- Selecting Document requires selecting Customer and Job, because Document
  validates through both public contracts (the same product-contract
  consequence ADR-006 records for Contact). This is a product-contract
  consequence, not an infrastructure constraint.

### Acme extension proof

- Acme will not build a second customer-owned persistence-extension proof. The
  Acme Customer Service Profile already proved the composition-around-a-stable-
  identity pattern, and Job already demonstrated that later modules need no
  data-extension proof (ADR-JOB-001).
- Acme instead demonstrates the customer-owned surface that Document does
  create: copied Documents panels with Acme branding, optional customer-owned
  terminology ("Files" or "Attachments"), and customer-owned permission keys
  (`acme.document.read`, `acme.document.manage`) in its permission catalog.

## Consequences

- The PRD's "Document" module is realized as one Document module; Attachment and
  File remain labels, not design elements.
- Document follows the exact module-ownership pattern proven by Contact and Job,
  so the established architecture (ADR-002/003/006) is reused rather than
  extended.
- Document's dependency graph is the minimum that expresses its product
  contract: framework + the two public owner contracts. It stays independent of
  Contact and User Maintenance, preserving its removability.
- The explicit exactly-one-owner rule keeps deletion and lifecycle semantics
  unambiguous: deleting a Document never affects another record and never has to
  resolve shared ownership.
- A customer needing a document on an owner type the baseline does not cover
  (e.g., a Contact) composes customer-owned data around the stable Document id
  (the proven extension model) or waits for promotion evidence.
