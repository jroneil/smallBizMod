# ADR-JOB-003: Job audit ownership and framework boundary

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

Framework FR-10 requires reusable audit mechanisms or hooks so business modules
and customer extensions record relevant actions consistently. ADR-UM-003
established the current contract: `sbp-framework` exposes `AuditEvent`
(immutable record with actor, subjectType, subjectId, action, changedFields,
detail, occurredAt — `changedFields` holds field *names* only, never old/new
values), `AuditRecorder`, and `AuditActorResolver`. ADR-UM-003 also decided that
audit *persistence and viewer* are User-Maintenance-owned ("the audit viewer
remains User-Maintenance-specific for now"), explicitly leaving open what
happens when another module becomes an audit consumer.

The Job module is the second audit consumer: its lifecycle (OPEN → IN_PROGRESS
→ COMPLETED / CANCELLED) is operational history that must not be hard-deleted
(FR-21), and its status transitions are exactly the kind of change the audit
contract exists to record. Job's arrival is therefore the test of ADR-UM-003's
"for now".

Two options were considered:

1. **Job owns its audit persistence** — a Job-owned `sbp_job_audit_event` table
   and read surface, recording through the framework hook, mirroring the User
   Maintenance pattern.
2. **Promote generic audit persistence into `sbp-framework`** — move the audit
   table/viewer concept to the framework so every module shares one persistence
   and one viewer.

## Decision

- Job records audit events through the framework `AuditRecorder` /
  `AuditActorResolver` contract — the cross-cutting infrastructure — and owns
  its own audit persistence: the `sbp_job_audit_event` table (`V15`), its
  repository, a Job-owned recorder implementation, and a minimal read surface
  for a Job's audit events.
- The framework does **not** gain generic audit persistence or a generic audit
  viewer at this stage. Option 2 is rejected because it would contradict
  ADR-UM-003's explicit ownership decision, move User Maintenance's
  persistence ownership out from under its module without a demonstrated
  failure, and enlarge the framework surface before a real cross-module
  querying requirement exists.
- Job's audit actions in v0.1: `JOB_CREATED`, `JOB_UPDATED`,
  `JOB_STATUS_CHANGED`. Status transitions may carry a marker in `detail`
  (e.g., "OPEN→IN_PROGRESS"); `changedFields` carries field names only, never
  values. `JOB_ASSIGNED` is reserved as a future action when assignment is
  added (see ADR-JOB-002).
- The actor is supplied through the framework `AuditActorResolver`. The module
  auto-configuration registers a default resolver returning `"system"` (guarded
  by `@ConditionalOnMissingBean`); the Acme reference application overrides it
  with a resolver returning the authenticated username, exactly as User
  Maintenance does.
- Every Job create, edit, and status change SHALL be recorded in the same
  transaction as the business change.

### Promotion trigger (recorded for the future)

Generic audit persistence/viewer in the framework, or a shared audit viewer,
should be re-evaluated only when one of these is demonstrated:

- a third persistent audit consumer appears, or
- a real cross-module audit-querying requirement emerges (e.g., one screen must
  search audit events across several modules' records), or
- the duplication of the persistence pattern becomes a measurable maintenance
  cost rather than a small, stable convention.

## Consequences

- The audit contract remains cross-cutting infrastructure; the Job domain model
  does not absorb audit concerns (audit is not the Job's notes system).
- Modules stay independently removable: Job's audit data is Job-owned, and
  removing Job removes its audit table from a clean assembly without touching
  User Maintenance's audit data (FR-02).
- The cost is a second, near-identical audit persistence pattern. That cost is
  accepted as the price of module ownership, consistent with ADR-UM-003, and is
  bounded by the recorded promotion trigger.
- Job's audit events are value-free (field names and transition markers only),
  so no plaintext personal or business values leak into audit, consistent with
  the contract's intent even though Job fields are not personal data.
