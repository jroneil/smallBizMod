# ADR-UM-005: Group role inheritance (implemented)

- Status: Accepted (decision supersedes the earlier deferral)
- Date: 2026-08-27

## Context

UM-FR-11 makes group-level role inheritance optional ("SHOULD"): group members inherit the roles
assigned at the group level, where a customer project opts into the model. The original decision
deferred this capability pending a concrete consumer. The Acme reference application now exercises
the model: users are organized into groups, and the reference permissions catalog is role-based, so
role-via-group inheritance has a real consumer and is implemented to close the last deferred
functional capability of User Maintenance v0.1.

## Decision

Group-level role inheritance **is implemented** as role-resolution data in the reusable User
Maintenance module:

- **Relationship**: a `sbp_group_role` join (`V13`) records which Roles are granted by which
  Groups, using the same intra-module FK and composite-key conventions as `sbp_user_role` and
  `sbp_user_group`. No role information is duplicated into Group records.
- **Direct versus inherited**: `listRoles(userId)` continues to mean *directly assigned* roles
  only; its semantics are unchanged. New `listEffectiveRoles(userId)` returns the union of direct
  ACTIVE roles and ACTIVE roles inherited through the user's ACTIVE group memberships.
- **Effective-role calculation** is a deterministic union: direct role assignments plus the roles
  granted by each group the user belongs to, deduplicated by role id and ordered by role name. It
  is plain application logic over the join tables — no authorization graph engine.
- **Retirement semantics**: RETIRED roles are excluded from the effective view, and grants from
  RETIRED groups are excluded as well. Retiring a Role or Group never deletes `sbp_group_role`,
  `sbp_user_role`, or `sbp_user_group` rows, so historical assignments remain visible for audit;
  `listRoles(groupId)` and `listRoles(userId)` still show retired roles/groups with their status.
- **Assignment rules** mirror User↔Role and User↔Group: assigning a RETIRED Role or assigning to a
  RETIRED Group is rejected (409); assignments are idempotent; removing an assignment deletes only
  that join row.
- **Audit**: group→role mutations record `GROUP_ROLE_ASSIGNED` / `GROUP_ROLE_REMOVED` against the
  GROUP subject through the framework audit hook. Effective-role reads generate no audit events.
- **Permissions**: the existing read-only Role→Permission catalog is unchanged. Effective roles are
  role-resolution data that a customer may map to permissions in its own authorization policy; the
  module does not expose or evaluate "effective permissions" and adds no authorization policy.

## Consequences

- Administrators can assign roles at the group level; group members inherit them. Direct and
  inherited roles remain distinguishable in every contract and in the UI.
- Existing direct-role callers are unaffected: `GET /api/users/{userId}/roles` still returns only
  directly assigned roles; effective roles are a new read endpoint.
- The module continues to store and calculate maintenance/reference data only. Acme/customer
  security remains responsible for authentication, credential management, and authorization
  enforcement.

## Status

The deferral is resolved: UM-FR-11 is implemented in the User Maintenance v0.1 release. Nested or
Group→Group inheritance, Role→Role inheritance, deny roles, permission precedence, and any
authorization-policy engine remain deliberately unsupported (see the implementation report).
