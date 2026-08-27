# ADR-UM-002: Authenticated identity to SBP User mapping

- Status: Accepted
- Date: 2026-08-27

## Context

The User Maintenance implementation plan's Slice 4 requires proving the distinction between customer authentication and the reusable User domain. An authenticated customer identity must map to one stable SBP User without moving authentication mechanism, identity provider, or authorization policy into the reusable module (framework FR-09, ADR-005). The PRD's UM-FR-05 also requires a self-service profile where an authenticated end user edits their own permitted fields.

## Decision

- The reusable User module exposes only a neutral lookup: `UserOperations.findByUsername(String)`. It has no concept of authentication or of "the current actor".
- The authenticated-identity-to-User mapping is customer-owned. Acme implements `AcmeIdentityService`, which maps `Principal.getName()` to `UserOperations.findByUsername(username)` — i.e., the reference policy aligns the authenticated username with the SBP User username.
- Self-service is customer-owned. Acme exposes `GET /api/me` and `PUT /api/me`, resolving the current User through `AcmeIdentityService` and editing only the non-privileged fields (email, given name, family name, phone) through the reusable `UserOperations.update`. Username, lifecycle status, roles, and groups are not editable through self-service.
- Acme adds a customer-owned `ApplicationRunner` (`AcmeDemoUserBootstrap`) that creates an SBP User for the configured development/demo security username when one does not exist, so the reference flow is demonstrable out of the box.

## Consequences

- The reusable module remains authentication-free and identity-provider-agnostic; the Acme Basic-auth boundary is unchanged.
- If a customer adopts an external IdP whose subject differs from the SBP username, they replace `AcmeIdentityService` with a customer-owned identity-mapping table. No SBP change is required.
- Self-service cannot escalate privilege: it cannot modify username, status, roles, or groups, and it always resolves the authenticated principal rather than an arbitrary user id.
- The bootstrap is development/reference convenience only and does not introduce identity policy into SBP.
