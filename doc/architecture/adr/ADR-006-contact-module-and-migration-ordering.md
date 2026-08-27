# ADR-006: Contact module dependency and migration ordering

- Status: Accepted
- Date: 2026-08-26

## Context

The PRD requires Contact as a reusable V1 capability after the initial Customer proof. Contact represents a person associated with a Customer and must remain distinct from Acme's customer-owned Service Profile. Contact is also the second persistent reusable module, so it supplies the evidence ADR-003 required before revisiting shared Flyway ordering.

Contact needs to confirm that its referenced Customer exists. The Customer module already exposes this information through `CustomerOperations`; direct repository or table access would violate module ownership. The application still uses one process, database schema, and Flyway history.

Acme already owns migration `V2`. The next available migration version is therefore `V3`. A reusable assembly without Acme can legitimately apply `V1` followed by `V3`; Flyway supports this ordered history without custom orchestration.

## Decision

- `sbp-module-contact` is an ordinary reusable JAR with an explicit Maven dependency on `sbp-module-customer`.
- It validates Customer identifiers only through the public `CustomerOperations` contract.
- Contact stores the stable Customer UUID in its own table. It does not read Customer tables and does not create a cross-module database foreign key.
- Contact owns `V3__create_sbp_contact.sql`, packaged under Flyway's standard migration location.
- Migration versions remain globally coordinated across the shared history for v0.1. Gaps in a particular application composition are acceptable.
- A customer application activates Contact by declaring its JAR dependency and can omit it by removing that dependency. No module registry or discovery mechanism is introduced.

## Consequences

- Contact cannot be selected without Customer, which reflects its product contract rather than an infrastructure constraint.
- Acme can include or omit Contact without changing application source or its Service Profile capability.
- An existing database is not downgraded or stripped of Contact data when the dependency is removed.
- Shared migration numbering now has demonstrated coordination cost, but no collision or upgrade failure justifies multiple histories or custom migration plumbing yet.
- The migration strategy must be revisited if independently released modules create a real version collision or prevent a supported application upgrade.
