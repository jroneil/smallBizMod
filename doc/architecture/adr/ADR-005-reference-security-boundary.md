# ADR-005: Reference security boundary

- Status: Accepted
- Date: 2026-08-26

## Context

The PRD requires a security integration boundary (FR-09) while leaving identity provider, authentication method, roles, and permissions to the customer project. The reference application needed to prove the boundary without prescribing a universal security policy.

## Decision

- Spring Security is the customer-owned security foundation. The Acme reference application owns its own `SecurityConfig` and enables HTTP Basic authentication for all `/api/**` endpoints.
- A single in-memory demo user is configured through `acme.demo.user` / `acme.demo.password` (overridable by environment), using a documented development-only default. Real deployments override these values.
- The Acme frontend adds a minimal sign-in page and attaches HTTP Basic credentials to API calls; it is customer-owned presentation of the customer's security policy.
- Reusable SBP modules (Customer, Contact) do **not** depend on or configure Spring Security and encode no roles or authorization policy. The customer application applies one security policy uniformly across installed module endpoints.
- No universal RBAC/ABAC, SSO, or MFA model is introduced.

## Consequences

- The customer application is proven to own authentication and authorization policy; SBP modules remain policy-free.
- Removing or replacing the Acme demo security would not require changing any SBP module.
- The demo credential is a development convenience, not a production security mechanism; NFR-04 is satisfied because real credentials are environment-injected, not shipped as the only option.
