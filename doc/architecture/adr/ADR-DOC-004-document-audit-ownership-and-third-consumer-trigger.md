# ADR-DOC-004: Document audit ownership and the third-consumer trigger

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

Framework FR-10 requires reusable audit mechanisms or hooks so business modules
and customer extensions record relevant actions consistently. ADR-UM-003
established the current contract: `sbp-framework` exposes `AuditEvent`
(immutable record with actor, subjectType, subjectId, action, changedFields,
detail, occurredAt — `changedFields` holds field *names* only, never old/new
values), `AuditRecorder`, and `AuditActorResolver`. ADR-JOB-003 then decided
that Job — the second audit consumer — records through the framework hook but
owns its audit persistence (`sbp_job_audit_event`, V15), and recorded a
**promotion trigger** for generic audit persistence/viewer in the framework:

- a third persistent audit consumer appears, or
- a real cross-module audit-querying requirement emerges, or
- the duplication of the persistence pattern becomes a measurable maintenance
  cost rather than a small, stable convention.

Document is the third persistent audit consumer (after User Maintenance and
Job), so the trigger has now fired and the recorded re-evaluation is required.
Every Document mutation is exactly the kind of operational history the audit
contract exists to record: uploads, renames, content replacements, and
deletions, with no hard-delete of the audit history itself (FR-21).

## Decision

- Document records audit events through the framework `AuditRecorder` /
  `AuditActorResolver` contract — the cross-cutting infrastructure — and owns
  its audit persistence: the `sbp_document_audit_event` table (`V18`), its
  repository, a Document-owned recorder implementation, and a minimal read
  surface for a single Document's audit events (DOC-FR-09/10).
- Document's audit actions in v0.1: `DOCUMENT_CREATED`,
  `DOCUMENT_METADATA_UPDATED`, `DOCUMENT_CONTENT_REPLACED`,
  `DOCUMENT_DELETED`. `changedFields` carries field names only; `detail` may
  carry a small marker but never old/new values, never file content, and never
  content hashes.
- The actor is supplied through the framework `AuditActorResolver`. The module
  auto-configuration registers a default resolver returning `"system"` (guarded
  by `@ConditionalOnMissingBean`); the Acme reference application overrides it
  with a resolver returning the authenticated username, exactly as User
  Maintenance and Job do.
- Every Document upload, rename, content replacement, and deletion SHALL be
  recorded in the same transaction as the business change.

### Trigger re-evaluation result: keep module-owned persistence

The third-consumer trigger has fired, and the re-evaluation concludes that
promotion is **declined for v0.1**:

1. **No cross-module audit-querying requirement exists.** The demonstrated read
   surfaces are per-subject (a Job's history, a Document's history, the User
   Maintenance viewer). No screen must search across several modules' audit
   records.
2. **The duplication is still a small, stable convention, not a measurable
   cost.** Three near-identical tables (`sbp_user_audit_event`,
   `sbp_job_audit_event`, `sbp_document_audit_event`) and three small recorders
   follow one pattern; the third copy adds no new mechanism.
3. **Promotion would enlarge the framework surface without a consumer.** A
   framework-owned audit table would require Spring/JPA/Flyway dependencies in
   an artifact that deliberately carries none, and would force User Maintenance
   and Job persistence changes (their tables are released module-owned schema)
   before any consumer demonstrates the need.

The decision remains open and must be revisited when a fourth persistent audit
consumer appears, or a real cross-module audit-querying requirement emerges, or
the duplication measurably grows.

## Consequences

- The audit contract remains cross-cutting infrastructure; the Document domain
  model does not absorb audit concerns (audit is not a document history
  viewer beyond the per-Document read view).
- Modules stay independently removable: Document's audit data is
  Document-owned, and removing Document removes its audit table from a clean
  assembly without touching User Maintenance's or Job's audit data (FR-02).
- The cost is a third near-identical audit persistence pattern. That cost is
  accepted as the price of module ownership, consistent with ADR-UM-003 and
  ADR-JOB-003, and is bounded by the recorded promotion trigger.
- Document's audit events are value-free (field names and small markers only),
  so no plaintext business content or file data leaks into audit, consistent
  with the contract's intent.
