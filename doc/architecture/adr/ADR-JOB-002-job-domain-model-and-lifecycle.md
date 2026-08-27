# ADR-JOB-002: Job domain model and lifecycle boundary

- Status: Accepted (documentation/architecture decision; implementation not yet authorized)
- Date: 2026-08-27

## Context

The Job module must define the smallest genuinely reusable baseline model,
per framework FR-14 ("reusable modules SHALL define only the common data and
behavior justified for their baseline capability") and P-02 (real requirements
over speculation). The candidate field list (id, customerId, title,
description, status, priority, assignedUserId, requested date, scheduled date,
due date, started date, completed date, createdAt, updatedAt) must each be
tested for universal usefulness, and every deferred concept must leave a
documented future seam rather than a design that precludes it.

Three boundary questions dominate the decision:

1. **Assignment (WHO).** Whether a Job should reference an SBP User, and with
   what semantics. "Assigned user" is genuinely ambiguous across the target
   businesses (owner, worker, technician, coordinator), a single
   `assignedUserId` field would collapse those meanings, and a typed generic
   assignment join would require a customer-specific assignment-type catalog.
   The User Maintenance module's existence is not itself a justification for
   assignment (no workforce management merely because User Maintenance exists).
   Per the Scheduling boundary in this ADR, WHO-performs-work is Scheduling
   territory, and no governing requirement places assignment on the Job.
   FR-14 explicitly forbids encoding customer-specific interpretations of Job.

2. **Scheduling (WHEN/WHERE/WHO).** A Job represents WORK; Scheduling
   represents WHEN/WHERE/WHO performs work. Calendar infrastructure, recurring
   schedules, availability engines, dispatch, route optimization, resource
   capacity, and time tracking are all out of scope for the Job capability. The
   only open question is whether a single planned date belongs on the Job
   record itself.

3. **Dates.** requested/scheduled/due/started/completed dates are individually
   common in enterprise work-order systems, which is not sufficient reason to
   include them (the PRD and this exercise both warn against that).

## Decision

### Baseline domain model (v0.1)

| Field | Rule | Rationale |
|---|---|---|
| `id` | SHALL, UUID | Stable identity across all modules; the future attachment anchor for Documents, Scheduling, notes, and customer extensions. |
| `customerId` | SHALL, required UUID | A Job is work for/on behalf of a Customer (see ADR-JOB-001); validated through the public `CustomerOperations` contract; no cross-module FK. |
| `title` | SHALL, required, short text | The human-meaningful name of the work; universal. |
| `description` | MAY, optional, longer text | Free-form work description; universal but not always present. |
| `status` | SHALL, enum | Lifecycle below; the core operational attribute. |
| `priority` | SHALL, small closed enum `LOW`/`NORMAL`/`HIGH`, default `NORMAL` | Urgency triage is genuinely repeated across service, repair, contractor, field-service, and small manufacturing businesses. Deliberately small: no configurable priority sets, no SLA calculation, no escalation rules, no queues. Mutable; display/filter semantics only. |
| `scheduledDate` | MAY, optional single date | The **only** scheduling information on Job v0.1: the date the work is currently planned to start. One nullable column with no recurrence, time-slot, resource, or availability semantics. Chosen because the target businesses treat "when is this job happening" as intrinsic to the job record, and it is the minimal scheduling touchpoint that keeps the Scheduling boundary honest. |
| `createdAt` / `updatedAt` | SHALL, timestamps | Needed for deterministic list ordering (createdAt DESC) and general utility; matches User Maintenance conventions. |

### Deferred fields and concepts (with future seams)

| Deferred | Reason | Future seam |
|---|---|---|
| Assignment (`assignedUserId` or assignment join) | Ambiguous semantics (owner/worker/technician/coordinator); WHO belongs to Scheduling; FR-14; no requirement authorizes a richer model. | A future `sbp_job_assignment` join (jobId, userId, type, assignedAt) or a single `assigned_user_id` column is purely additive. Customers needing assignment today compose customer-owned joins around the stable Job and User identities. When assignment is added, the audit action `JOB_ASSIGNED` becomes available. |
| Contact association (primary contact, requested-by, onsite contact) | Multiple plausible meanings, none settled by a requirement; Contact module existing is not a justification. | A future `requestedByContactId` field or job-contact join, validated through the public `ContactOperations` contract when needed. |
| `requestedDate`, `dueDate`, `startedDate`, `completedDate` | started/completed are derivable from status transitions and audit timestamps; requested/due imply deadline tracking not proven required; minimalism (FR-14). | Additive nullable columns in a future slice when a real consumer needs queryable dates (e.g., Reporting). |
| Notes / comments | Audit history and business notes are different concepts; audit must not become a customer-visible notes system; no concrete requirement justifies a notes subsystem. | A future `job_note` capability (jobId, author, body, createdAt) composed around the stable Job id. |
| Documents | Documents are a separate candidate capability. | The stable Job id is the attachment anchor; a future Document capability attaches by jobId (owner-style references or a generic owner model are the Document module's choice) without any Job redesign. FR-11 storage abstraction remains deferred until Document selection. |
| Bulk creation | No requirement; User Maintenance's bulk import (UM-FR-19) does not transfer by analogy. | Future bulk/high-volume ingestion as an in-process capability (validating batch with per-row error reporting, mirroring the proven User Maintenance bulk pattern) or a same-deployment worker, only when a requirement justifies it. No queues, brokers, or microservice now (NFR-07). |

### Lifecycle

- Statuses: `OPEN` (initial), `IN_PROGRESS`, `COMPLETED` (terminal),
  `CANCELLED` (terminal).
- Allowed transitions (deliberately small, no workflow framework):
  - `OPEN` → `IN_PROGRESS`
  - `OPEN` → `CANCELLED`
  - `IN_PROGRESS` → `COMPLETED`
  - `IN_PROGRESS` → `CANCELLED`
- Invalid transitions are rejected (409 Conflict, matching the User Maintenance
  state-conflict convention). Reopening a terminal Job (COMPLETED/CANCELLED →
  any other state) is **not** supported in v0.1; a reopened engagement is a new
  Job or a customer extension.
- No hard delete of operational history (FR-21). There is no DELETE endpoint in
  v0.1; lifecycle transitions replace deletion.
- Non-status fields (title, description, priority, scheduledDate) remain
  editable in any state, including terminal states, in v0.1. All edits are
  audited (see ADR-JOB-003). Customers may restrict editing further through
  their own authorization policy.

### Scheduling boundary

- Job v0.1 carries exactly one scheduling touchpoint (`scheduledDate`, above).
- Everything else about WHEN/WHERE/WHO — calendars, recurrence, availability,
  dispatch, routing, capacity, time tracking, and assignment — belongs to a
  future Scheduling capability that composes around the stable Job id and does
  not require Job table changes.
- The Job schema must not preclude that future capability; the single nullable
  date column does not.

## Consequences

- The baseline Job is a work record with a small lifecycle, not a work-order
  system: no workforce management, no scheduling engine, no notes subsystem, no
  document storage, no bulk machinery.
- Every deferred concept remains achievable by additive customer composition or
  a future additive migration; nothing in the v0.1 design precludes it.
- Customers with assignment, contact-person, document, or note needs before the
  baseline provides them use the proven composition model around the stable Job
  id; repetition across customers is the promotion evidence for a future
  baseline addition (P-11).
- The pagination lesson from User Maintenance is applied at the design level:
  the list contract (search, filters, pagination, deterministic ordering) is
  part of the baseline requirement set, not a later retrofit.
