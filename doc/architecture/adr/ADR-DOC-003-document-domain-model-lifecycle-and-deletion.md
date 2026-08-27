# ADR-DOC-003: Document domain model, lifecycle, replacement, and deletion

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

The Document module must define the smallest genuinely reusable baseline model,
per framework FR-14 ("reusable modules SHALL define only the common data and
behavior justified for their baseline capability") and P-02 (real requirements
over speculation). The candidate field list (id, owner, displayName, contentType,
size, timestamps, plus a possible status) and the operation set (upload,
download, rename, replace, delete) must each be tested for universal usefulness,
and every deferred concept must leave a documented future seam.

Framework FR-21 requires explicit lifecycle states "where appropriate" and
forbids destructive cascading deletion as the default for durable business
history. The open questions are whether a Document needs a lifecycle status at
all, whether replacement should keep version history, and what deletion means
for a leaf content record.

## Decision

### Baseline domain model (v0.1)

| Field | Rule | Rationale |
|---|---|---|
| `id` | SHALL, UUID | Stable identity across all modules; the storage key and the audit subject. |
| `customerId` / `jobId` | SHALL, exactly one of the two, required | The owner (ADR-DOC-001); validated through the public Customer/Job contracts; no cross-module FK. |
| `displayName` | SHALL, required, bounded text | The human-meaningful file name (original name, cleaned of path components). |
| `contentType` | SHALL, advisory string | Stored for display and download headers; not an authorization decision (NFR-DOC-07). |
| `sizeBytes` | SHALL, long | Recorded from the upload; drives the size-limit rejection. |
| `createdAt` / `updatedAt` | SHALL, timestamps | Needed for deterministic list ordering (createdAt DESC) and general utility; matches Job/User Maintenance conventions. |
| `status` | SHALL NOT exist in v0.1 | No lifecycle status; see below. |

### No lifecycle status in v0.1

- A Document has no lifecycle state in v0.1. It exists while attached to its
  owner and is removed only by an explicit audited delete. Operations are:
  upload (create), rename (metadata edit), replace (content overwrite), and
  delete.
- FR-21 is a SHOULD qualified by "where appropriate". A Document is a leaf
  content record, not a durable business record: the durable business history
  lives on the owner (Customer/Job), which keeps its lifecycle and is never
  cascaded into by Document deletion. The document's own history is preserved
  in the value-free audit record (who uploaded/replaced/renamed/deleted what,
  and when).
- A soft-delete/archive status, configurable retention, and records-management
  behavior remain deferred until a demonstrated requirement justifies them
  (P-02; the Document supplement's explicit exclusions). Adding a status
  column or retention rules later is purely additive.

### Deletion: explicit, audited, non-cascading

- Delete is an explicit audited action that removes the metadata record and its
  content (through `BlobStorage.delete`). It never cascades to the owner
  Customer or Job record, which have their own lifecycle semantics
  (FR-21).
- Deleted Documents are no longer listable; the deletion event remains in the
  per-Document audit view, preserving the business history without retaining
  orphaned content indefinitely.

### Replacement, not versioning

- `replaceContent` overwrites the content in place: same Document identity and
  metadata row, updated `sizeBytes`, `contentType`, and `updatedAt`, audited as
  `DOCUMENT_CONTENT_REPLACED`.
- No version history in v0.1: only the latest content is retrievable. Version
  chains, per-version audit, and restore are records-management features no
  governing requirement has demanded (P-02, NFR-07). A future
  `sbp_document_version` table keyed by document id is purely additive and does
  not change the baseline contract.

### Metadata edit

- The display name is editable (rename) and audited as
  `DOCUMENT_METADATA_UPDATED`. Renaming is the natural correction for a
  mistyped or misleading file name and avoids delete-and-re-upload, which would
  break identity and audit continuity.

## Consequences

- The baseline Document is a file record with clean create/read/replace/rename/
  delete semantics, not a document-management system: no version chains, no
  folders, no retention engine, no approval routing.
- Every deferred concept (soft-delete status, versioning, retention,
  standalone or multi-owner documents) remains achievable by additive customer
  composition or a future additive migration; nothing in the v0.1 design
  precludes it.
- FR-21 is satisfied in its qualified form: destructive cascading deletion is
  never the default, the owner records keep their lifecycle, and Document
  history is preserved in audit even though the content record itself is
  removable.
- Customers needing version history or retention before the baseline provides
  them compose customer-owned data around the stable Document id (the proven
  extension model); repetition across customers is the promotion evidence for a
  future baseline addition (P-11).
