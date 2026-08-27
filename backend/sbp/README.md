# SBP reusable backend artifacts

The backend reactor produces ordinary Maven JARs. It does not produce a standalone SBP service.

## Artifacts

- `com.oneilsoftware:sbp-framework:0.1.0-SNAPSHOT` provides the minimal framework audit contract under `com.oneilsoftware.sbp.framework.audit` (`AuditEvent`, `AuditRecorder`, `AuditActorResolver`) for framework FR-10. It carries no Spring or business-module dependencies.
- `com.oneilsoftware:sbp-module-customer:0.1.0-SNAPSHOT` provides the generic Customer capability and depends only on the framework artifact and its declared Spring/runtime dependencies.
- `com.oneilsoftware:sbp-module-contact:0.1.0-SNAPSHOT` provides people associated with Customers and depends on the Customer module's public contract.
- `com.oneilsoftware:sbp-module-user-maintenance:0.1.0-SNAPSHOT` provides reusable application/business identity records with create, search, detail, edit, and activate/deactivate lifecycle administration, plus Role and Group maintenance, their User assignments, and an audit log. It depends only on the framework artifact; it does not depend on Customer or Contact.
- `com.oneilsoftware:sbp-module-job:0.1.0-SNAPSHOT` provides the reusable Job / Work Order capability: create, paginated search/filter, detail, edit, and an OPEN/IN_PROGRESS/COMPLETED/CANCELLED lifecycle with explicit start/complete/cancel actions. It depends on the framework artifact (audit hook) and on the Customer module's public contract; it does not depend on Contact or User Maintenance. A Job belongs to exactly one Customer, validated through `CustomerOperations` with no cross-module foreign key. Every create, edit, and status change is audited through the framework `AuditRecorder` hook into the Job-owned audit table with field-name markers and status-transition detail only. The per-Job audit history is readable through `JobOperations.listAuditEvents(jobId, action)` and `GET /api/jobs/{jobId}/audit-events` (JOB-FR-09), newest first, with an optional action filter; events never contain field values.
- `com.oneilsoftware:sbp-module-document:0.1.0-SNAPSHOT` provides the reusable Document capability: upload, paginated search/filter by owner, detail, download, content replacement, rename, and explicit audited delete. It depends on the framework artifact (audit hook and the FR-11 storage abstraction) and on the public Customer and Job contracts; a Document is attached to exactly one owner — a Customer or a Job — validated through `CustomerOperations`/`JobOperations` with no cross-module foreign key. Content is stored only through the framework `BlobStorage` abstraction (ADR-DOC-002): the module ships a reference PostgreSQL `bytea` implementation (`sbp_document_content`), and a customer deployment can supply its own approved implementation as a single bean. Every upload, rename, content replacement, and deletion is audited through the framework `AuditRecorder` hook into the Document-owned audit table with field-name markers only. The per-Document audit history is readable through `DocumentOperations.listAuditEvents(documentId, action)` and `GET /api/documents/{documentId}/audit-events` (DOC-FR-10), newest first, and remains readable after deletion (DOC-FR-06); events never contain field values or content. Uploads over `sbp.document.max-size-bytes` (default 10 MB) are rejected before anything is stored, and the module's web-layer multipart configuration accepts files up to that same limit.

The Acme executable application pins and consumes these artifacts from its own Maven project. A local install is sufficient for this baseline:

```bash
./backend/sbp/mvnw -f backend/sbp/pom.xml clean install
```

No BOM or artifact repository is required yet.

## API boundary

Reusable Customer contracts are under:

```text
com.oneilsoftware.sbp.module.customer.api
```

Reusable Contact contracts are under:

```text
com.oneilsoftware.sbp.module.contact.api
```

Reusable User Maintenance contracts are under:

```text
com.oneilsoftware.sbp.module.usermaintenance.api
```

Reusable Job contracts are under:

```text
com.oneilsoftware.sbp.module.job.api
```

Reusable Document contracts are under:

```text
com.oneilsoftware.sbp.module.document.api
```

Consumers may use those types. Packages under:

```text
com.oneilsoftware.sbp.module.customer.internal
```

are implementation details and are not compatibility promises. The module exposes `CustomerOperations` for in-process composition and owns its repository, entity, table, and Flyway migration. Customer applications must not access those internals.

Spring Boot auto-configuration activates the module when its JAR is present. There is no custom module registry, reflection-based discovery, or separately deployed module process.

Contact uses `CustomerOperations` to validate its Customer identifier. It owns its entity, repository, table, and Flyway migration, does not read Customer persistence, and does not create a cross-module foreign key.

User Maintenance owns the `sbp_user`, `sbp_role`, `sbp_group`, `sbp_user_role`, `sbp_user_group`, `sbp_group_role`, `sbp_user_audit_event`, `sbp_permission`, and `sbp_role_permission` tables, their entities, repositories, and `V4`–`V10` plus `V13` Flyway migrations. It contains no authentication, identity-provider, or authorization code: the SBP User is a maintenance record, and authentication remains customer-owned. Role and Group retirement are lifecycle transitions (`RETIRED`) that preserve assignment history rather than deleting it. Every maintenance mutation is audited through the framework `AuditRecorder` hook with the actor supplied by a customer-owned `AuditActorResolver`; audit stores changed field names only, never personal-data values. Personal fields (email, given name, family name, phone) are encrypted at rest with AES-256-GCM, and name/email search uses keyed HMAC-SHA-256 blind-index tokens so plaintext PII never appears in the database; the encryption and search keys are separate, environment-supplied, and required. The permission catalog is customer-defined: the module stores and exposes it read-only, while the customer seeds its own permission keys and role grants. Bulk provisioning validates rows before commit and reports per-row errors; bulk export returns the same masked data as the list view. Group-level role inheritance (UM-FR-11) is implemented as a `sbp_group_role` join: `GroupOperations.listRoles/assignRole/removeRole` manage group grants, `UserOperations.listRoles` remains directly assigned roles only, and `UserOperations.listEffectiveRoles` returns the deduplicated union of direct ACTIVE roles and ACTIVE roles inherited through ACTIVE group memberships. Effective-role calculation is role-resolution data for customer authorization policy, not enforcement.

## Module removal

User Maintenance is independently removable. It depends only on `sbp-framework`; Customer and Contact do not depend on it, so a customer application can assemble Customer + Contact without User. The module's own integration test starts with only the framework and User Maintenance on the classpath, proving there is no hidden dependency on Customer or Contact. A customer application that chooses to consume User Maintenance must also remove its own User-dependent code when it removes the module (as with any selected module).

Job is independently removable from Customer, Contact, and User Maintenance. It depends on `sbp-framework` and the Customer public contract; its integration test starts with the framework, Customer, and Job on the classpath, proving no hidden dependency on Contact or User Maintenance. Selecting Job requires selecting Customer (the same product-contract consequence ADR-006 records for Contact). Job's migrations are `V14__create_sbp_job.sql` and `V15__create_sbp_job_audit_event.sql` in the shared Flyway history; a composition without Job simply does not apply them.

Document is independently removable from Customer, Contact, and User Maintenance, and from Job's persistence. It depends on `sbp-framework` (audit hook and the FR-11 storage abstraction) and on the public Customer and Job contracts; its integration test starts with the framework, Customer, Job, and Document on the classpath, proving no hidden dependency on Contact or User Maintenance. Selecting Document requires selecting Customer and Job (the same product-contract consequence ADR-006 records for Contact). Document's migrations are `V16__create_sbp_document.sql`, `V17__create_sbp_document_content.sql`, and `V18__create_sbp_document_audit_event.sql` in the shared Flyway history; a composition without Document simply does not apply them. The reference application packages cleanly without the Document module when its `without-document` profile is active together with the other modules; the reference test suite requires the default composition because it exercises the Document workflow.

## Release status

For its current scope, User Maintenance satisfies its SHALL requirements: User/Role/Group maintenance and assignment, audit with no plaintext PII, encryption at rest with separate externally-supplied keys, masked list/export, blind-index name/email search, and the read-only permission catalog. Conditional local-auth credential administration (UM-FR-13/14) is implemented in the Acme reference as customer-owned security (see ADR-UM-006); UM-FR-15 session visibility/revocation is not applicable to stateless HTTP Basic authentication and is documented rather than implemented. Group-role inheritance (UM-FR-11) and retention/erasure (NFR-UM-06/07) remain explicitly deferred; see ADR-UM-005 and the User Maintenance implementation plan.

For its current scope, Document satisfies its SHALL requirements: upload/list/search/filter/paginate/detail/download/replace/rename/delete with a paginated baseline list contract, exactly-one-owner association validated through the public Customer/Job contracts with no cross-module FK, content stored only through the framework storage abstraction with a reference database-backed implementation, size-limit enforcement before storage, and value-free same-transaction audit with a per-Document read view that survives deletion. Versioning, folders, full-text indexing, scanning, retention, and standalone/multi-owner documents remain deferred (ADR-DOC-003 and the Document implementation plan).
