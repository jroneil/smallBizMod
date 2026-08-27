# ADR-005: Reference security boundary

- Status: Accepted
- Date: 2026-08-26

## Context

The reference application must prove that customer applications own security policy while reusable SBP modules remain free of company roles and identity-provider assumptions. No reusable module currently needs an authenticated-actor contract, so introducing an SBP security facade would be an unused abstraction.

## Decision

- The Acme application owns a standard Spring Security `SecurityFilterChain`.
- The reference policy uses development/demo HTTP Basic authentication with credentials supplied by the deployment environment.
- Every current HTTP endpoint requires authentication. CSRF is disabled because this reference policy is stateless HTTP Basic authentication.
- SBP framework and module artifacts contain no Acme roles, users, credentials, or authorization rules.
- Spring Security's supported configuration surface is the v0.1 integration boundary. SBP will add a narrower public contract only when a reusable capability has a concrete need for authenticated actor information.

## Consequences

- A customer can replace the reference policy at the application composition root without changing reusable modules.
- This is not a production identity design and makes no choice about SSO, OAuth/OIDC providers, MFA, session policy, or universal RBAC/ABAC.
- HTTP Basic remains the backend authentication mechanism, but the Acme frontend now owns a branded sign-in and sign-out experience instead of relying on the browser challenge.
- For this development/reference application only, the frontend keeps the Basic authorization value in tab-scoped `sessionStorage` and applies it through its centralized API helper. This is explicitly not a production-safe session design.
- Acme suppresses the `WWW-Authenticate` challenge header on 401 responses so invalid form submissions and expired demo state produce application-owned UI rather than a native browser prompt.

## Implementation refinement (2026-08-27)

The custom Acme login slice advances the previously deferred browser experience without changing the decision boundary: Spring Security and credentials remain customer-owned, all API endpoints still require authentication, and reusable SBP artifacts remain security-policy-free. Production identity, credential transport, session, and authorization design remain open for each customer application.
