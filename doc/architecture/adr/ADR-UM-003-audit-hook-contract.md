# ADR-UM-003: Audit hook contract

- Status: Accepted
- Date: 2026-08-27

## Context

Framework FR-10 requires reusable audit mechanisms or hooks so modules and customer extensions record relevant actions consistently. The User Maintenance PRD (UM-FR-16/17/18) makes audit a hard requirement: create, update, deactivate, role/group assignment, and permission-relevant changes must produce auditable records with actor, timestamp, event type, and field-change metadata, and encrypted personal-data values must never appear in plaintext in audit records.

The reusable User module must not own authentication, so it cannot resolve the actor itself. The framework is currently empty; the audit hook is the first real cross-cutting framework contract.

## Decision

- `sbp-framework` exposes a minimal audit hook contract under `com.oneilsoftware.sbp.framework.audit`:
  - `AuditEvent` — an immutable record (`actor`, `subjectType`, `subjectId`, `action`, `changedFields`, `detail`, `occurredAt`). `changedFields` holds field *names* only, never old/new values.
  - `AuditRecorder` — `void record(AuditEvent)`, the hook modules and customers call after a relevant action.
  - `AuditActorResolver` — `String currentActor()`, the contract for resolving the authenticated actor.
- User Maintenance owns its audit persistence and viewer: the `sbp_user_audit_event` table (`V7`), `UserAuditEventRepository`, `UserMaintenanceAuditRecorder` (implements `AuditRecorder`), and `AuditOperations.search(AuditFilter)`.
- The module records audit events from its application services for: user create/update/deactivate/activate, role create/update/retire, group create/update/retire, and role/group assign/remove. Each event is recorded in the same transaction as the business change.
- The actor is supplied through the framework `AuditActorResolver`. The module auto-configuration registers a default resolver returning `"system"` (guarded by `@ConditionalOnMissingBean`); the Acme reference application overrides it with a resolver that returns the authenticated username from the Spring Security context.

## Consequences

- The reusable module stays authentication-free: it depends only on the neutral `AuditActorResolver` contract.
- No plaintext personal-data values are written to the audit table; only changed field names are stored, satisfying UM-FR-18 without requiring an encryption scheme in this slice.
- Other modules and customer extensions can reuse the same framework hook; the audit viewer remains User-Maintenance-specific for now.
- The framework artifact is no longer empty — it now carries a real cross-cutting contract, which is the intended FR-10 outcome.
