# ADR-UM-006: Local credential management and session boundary

- Status: Accepted
- Date: 2026-08-27

## Context

UM-FR-13/14 are conditional SHALLs that apply where a customer uses local credential storage/authentication. UM-FR-15 requires active-session visibility and revocation. The Acme reference uses customer-owned local HTTP Basic authentication with a persisted local credential store, while the reusable User Maintenance module remains authentication-free.

## Decision

- The reusable User Maintenance module gains **no** credential contract, password storage, session table, or authentication logic. Its public boundary is unchanged.
- Acme owns a local credential store (`acme_user_credential`, `V11`) keyed by the stable SBP User id. It stores only a one-way BCrypt password hash (Spring Security's delegating encoder) plus a failed-attempt counter — never a reversible password.
- **UM-FR-13** is implemented in Acme:
  - Self-service `POST /api/me/password` requires the current password and changes only the authenticated identity's credential.
  - Administrator `PUT /api/acme/users/{userId}/password` sets a new credential for the selected User (upsert), with no prior credential required.
- **UM-FR-14** is implemented in Acme: a failed login increments the counter; five failed attempts lock the account; a successful authentication resets the counter; the administrator can unlock (`POST /api/acme/users/{userId}/unlock`). Lockout state is customer-owned and surfaced read-only through Acme's own endpoint.
- **Administrator password generation and temporary-password workflow** is implemented in Acme:
  - The admin reset UI offers an optional "Generate password" control that uses the browser's `crypto.getRandomValues`; manual entry remains available. The generated value is never persisted and only ever shown in (and copied from) the form field.
  - The reset request carries a `requireChange` flag (default true in the reference UI). When set, a server-side `password_change_required` column (`V12`) is persisted on the credential record.
  - A security filter enforces the flag at the boundary: a flagged identity receives 403 for every ordinary API and is limited to the session, own-profile, and own-password endpoints until the flag is cleared.
  - A single authoritative `AcmePasswordPolicy` (minimum 8, maximum 72 characters) validates every password — manual, generated, and self-service — through the same server-side path; form-level length checks are presentation-only.
  - The session endpoint surfaces `passwordChangeRequired` so the reference frontend can redirect a freshly authenticated flagged identity to a forced change-password screen, after which the local session is dropped and the user signs in again.
- **UM-FR-15 is not applicable**: HTTP Basic authentication is stateless and creates no server-side session to view or revoke. This is documented rather than faked with a session table.
- Credential actions are audited through the framework `AuditRecorder` with actions `PASSWORD_CHANGED`, `CREDENTIAL_RESET`, and `ACCOUNT_UNLOCKED`. No password, hash, reset secret, or token is ever placed in audit records.
- The demo account is bootstrapped from `acme.security.username`/`acme.security.password` only when no credential exists, after which the persisted credential governs.

## Consequences

- A customer using an external IdP delegates UM-FR-13 reset to that IdP (not applicable locally) and typically has no local failed-login/lockout state to administer.
- The reference password policy (minimum 8, maximum 72 characters) is an Acme default, not an SBP requirement, and is the sole server-side rule — manual and generated passwords are indistinguishable to the backend.
- The generated-password control is deliberately client-side only: the backend accepts the generated value through the same reset endpoint as any manual value, so no new API or secret-transport surface is introduced.
- No reusable credential seam was introduced because credential storage is entirely customer-owned; the reusable module only provides the stable User identity that Acme's credentials reference.

## Status

Recorded to replace the earlier deferral of Slice 8. UM-FR-13 and UM-FR-14 are implemented for the reference; UM-FR-15 remains genuinely inapplicable to stateless Basic authentication.
