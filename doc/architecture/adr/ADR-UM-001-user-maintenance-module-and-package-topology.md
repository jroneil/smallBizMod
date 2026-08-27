# ADR-UM-001: User Maintenance module and package topology

- Status: Accepted
- Date: 2026-08-27

## Context

The User, Role & Group Maintenance PRD supplement makes User/Role/Group maintenance a candidate reusable SBP baseline module. The User Maintenance implementation plan sequences the first deliverable as Slice 0 (module skeleton) plus Slice 1 (User identity and lifecycle). The module must follow the established Customer/Contact module pattern: an independently versioned JAR, a public API/internal package split, standard Spring Boot auto-configuration, and module-owned persistence with a Flyway migration.

The User domain must stay independent from customer authentication: an SBP User is a reusable application/business identity and maintenance record, while authentication mechanism, identity provider, MFA, and authorization policy remain customer-project decisions (framework FR-09, ADR-005).

## Decision

- The reusable artifact is `com.oneilsoftware:sbp-module-user-maintenance:0.1.0-SNAPSHOT`, an ordinary JAR.
- The package root is `com.oneilsoftware.sbp.module.usermaintenance`, with the public contract under `api` and implementation under `internal.application`, `internal.persistence`, `internal.web`, and `internal`.
- The module depends only on `sbp-framework`. It does not depend on `sbp-module-customer`; User records are application identities, not Customer records.
- Standard Spring Boot auto-configuration activates the module when its JAR is present, mirroring ADR-002. No registry, discovery, or scanning of implementation packages is introduced.
- The module owns the `sbp_user` table, its repository, entity, and migration. It ships `V4__create_sbp_user.sql`, the next version in the shared Flyway history after V1 (Customer), V2 (Acme profile), and V3 (Contact). No cross-module foreign key is created.

## Slice 1 User model and lifecycle

- Public contract: `User`, `UserStatus` (ACTIVE/INACTIVE), `CreateUser`, `UpdateUser`, `UserFilter`, and the `UserOperations` service interface for create, search, find, update, activate, and deactivate.
- Fields: `id`, `username` (unique, immutable in Slice 1), `email`, `givenName`, `familyName`, `phone`, `status`, `createdAt`, `updatedAt`. `lastLoginAt` is deferred to the credential/session slices where it has a real producer.
- Lifecycle uses status transitions (`activate`/`deactivate`) rather than hard deletion, per UM-FR-04 and framework FR-21.
- Search/filter supports a text query (username, email, given name, family name) and a status filter. Role/group filters are deferred until those assignments exist.

## Personal data and encryption status

- `email`, `givenName`, `familyName`, and `phone` are personal data fields subject to the PRD's encryption-at-rest requirement (NFR-UM-01).
- This slice stores those fields in plaintext only because it is a development proof on the non-release `usermod` branch. The User Maintenance implementation plan permits temporary development storage before the data-protection slice.
- The module is not release-complete or PRD-compliant for data protection until Slice 6 implements encryption at rest, key management, and masking. The domain is structured so encryption can be applied to the mapped columns without redesigning the public contract.

## Authentication boundary

- The module contains no authentication, identity-provider, MFA, password-policy, or authorization code.
- The authenticated-identity-to-SBP-User mapping (ADR-UM-002 / Slice 4) is not implemented in Slice 1. The Acme reference application administers Users through its existing customer-owned HTTP Basic security boundary without the User module knowing how authentication happens.

## Slice 2 Role management and assignment

- Public contract: `Role`, `RoleStatus` (ACTIVE/RETIRED), `CreateRole`, `UpdateRole`, `RoleOperations` (create, list, find, update, retire), plus `listRoles`/`assignRole`/`removeRole` on `UserOperations`.
- Role retirement is a lifecycle transition (UM-FR-08): retiring a role sets `RETIRED` and preserves existing `sbp_user_role` rows rather than deleting assignments. Retired roles cannot be newly assigned (`RoleRetiredException` → 409).
- The `sbp_user_role` join has a composite key `(user_id, role_id)` and `assigned_at`, matching the PRD data model. It uses intra-module foreign keys to `sbp_user` and `sbp_role` (same owning module, so relational integrity is retained without any cross-module FK). `assigned_by` is deferred until the authenticated-identity mapping (Slice 4) and audit (Slice 5) supply a real actor value.
- The role capability ships `V5__create_sbp_role_and_user_role.sql` in the same shared Flyway history.

## Slice 3 Group management and membership

- Public contract: `Group`, `GroupStatus` (ACTIVE/RETIRED), `CreateGroup`, `UpdateGroup`, `GroupOperations` (create, list, find, update, retire, listMembers), plus `listGroups`/`assignGroup`/`removeGroup` on `UserOperations`.
- Group retirement mirrors Role retirement (UM-FR-08): retiring sets `RETIRED` and preserves existing `sbp_user_group` rows. Retired groups cannot be newly assigned (`GroupRetiredException` → 409).
- The `sbp_user_group` join has a composite key `(user_id, group_id)` and `assigned_at`, with intra-module foreign keys to `sbp_user` and `sbp_group`. Group-level role inheritance (UM-FR-11) remains deferred pending ADR-UM-005.
- `GroupOperations.listMembers(groupId)` returns the group's member Users for the group detail screen; membership assignment remains user-centric on `UserOperations` to match the PRD's "User–Group Assignment" framing.
- The group capability ships `V6__create_sbp_group_and_user_group.sql` in the same shared Flyway history.

## Consequences

- User Maintenance can be included or omitted from a customer application by Maven composition, matching Contact.
- Personal data remains plaintext in this development proof until the encryption slice; that gap must be tracked and closed before any release claim.
- Shared migration numbering now has three reusable persistent modules (Customer V1, Contact V3, User V4/V5/V6) plus the Acme extension (V2). ADR-003/ADR-006 remain satisfied; a separate migration coordinator is still not justified.
