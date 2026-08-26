# ADR-006: Contact module and migration coordination

- Status: Accepted
- Date: 2026-08-26

## Context

The PRD lists Contact as a core baseline module. Once the Customer proof succeeded, the second persistent reusable module exercised the migration coordination that ADR-003 explicitly deferred.

## Decision

- **Contact** is the second reusable module (`sbp-module-contact`), owned independently of Customer.
- A contact references its owning account by a stable `customer_id` UUID. The Contact module does **not** depend on the Customer module and does **not** add a foreign key to `sbp_customer`, so Contact can be installed or omitted without requiring the Customer schema or migration.
- Contact is validated and composed with Customer at the application layer (the Acme reference app), not through shared persistence.
- Migration versions continue the single shared ordered history for v0.1: `V1` (Customer), `V2` (Acme-owned extension), `V3` (Contact). The Contact module's own migration applies cleanly on an empty schema, proving module-level omission.
- No reserved version ranges or custom Flyway orchestration are introduced; coordination is revisited only when a real collision or upgrade problem appears.

## Consequences

- A second reusable module now demonstrates composition-by-stable-identifier and module-owned persistence at the same scale as the first.
- Contact is removable from the reference application without changing Acme source (the `without-contact` build profile).
- Cross-module foreign keys remain avoided in favor of stable identifiers plus application-layer composition.
