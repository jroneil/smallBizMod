# ADR-004: First proof module and reference scenario

- Status: Accepted
- Date: 2026-08-26

## Context

The architecture needed one simple reusable business module and one realistic consumer to validate packaging, removal, UI composition, persistence ownership, and customer extension before expanding the platform.

## Decision

- **Customer** is the first reusable proof module.
- The reference customer is **Acme Service & Repair**, a fictional small service/repair company, using the non-SBP package namespace `com.acmeco.reference`.
- Acme extends Customer by composition: a customer-owned `acme_customer_profile` table and API compose around the stable Customer identifier without modifying or subclassing the reusable Customer entity.
- The minimum vertical slice is: framework + Customer module + Acme backend + copied/customized frontend + PostgreSQL, exercised by the create → view → enrich → reload demo flow.
- **Contact** is added later (ADR-006) as a second reusable module once the first proof succeeded.

## Consequences

- The reference application is the primary architecture test and may force simplification of the framework.
- Customer remains independently removable at the module level; its standalone integration test proves it starts with only its declared dependencies.
- Acme's own Customer-specific extension code legitimately depends on the Customer public API, so Customer omission is demonstrated at the module level rather than by stripping it from the fully extended reference application.
