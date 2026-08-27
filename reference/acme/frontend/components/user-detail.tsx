"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { GroupStatusBadge } from "@/components/group-list";
import { RoleStatusBadge } from "@/components/role-list";
import { UserStatusBadge } from "@/components/user-list";
import { credentialApi, type AcmeCredentialStatus } from "@/lib/acme-api";
import { groupApi, roleApi, userApi, type Group, type Role, type User } from "@/lib/api";
import { generatePassword } from "@/lib/password";

export function UserDetail({ userId }: { userId: string }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [email, setEmail] = useState("");
  const [givenName, setGivenName] = useState("");
  const [familyName, setFamilyName] = useState("");
  const [phone, setPhone] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const [credential, setCredential] = useState<AcmeCredentialStatus | null>(null);
  const [resetPassword, setResetPassword] = useState("");
  const [showResetPassword, setShowResetPassword] = useState(false);
  const [requirePasswordChange, setRequirePasswordChange] = useState(true);
  const [credentialNotice, setCredentialNotice] = useState<string | null>(null);
  const [credentialBusy, setCredentialBusy] = useState(false);

  const [assignedRoles, setAssignedRoles] = useState<Role[]>([]);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [roleError, setRoleError] = useState<string | null>(null);
  const [effectiveRoles, setEffectiveRoles] = useState<Role[]>([]);
  const [inheritedRoles, setInheritedRoles] = useState<{ role: Role; group: Group }[]>([]);

  const [assignedGroups, setAssignedGroups] = useState<Group[]>([]);
  const [allGroups, setAllGroups] = useState<Group[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState("");
  const [groupError, setGroupError] = useState<string | null>(null);

  const loadUser = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await userApi.get(userId);
      setUser(loaded);
      setEmail(loaded.email);
      setGivenName(loaded.givenName);
      setFamilyName(loaded.familyName);
      setPhone(loaded.phone ?? "");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The user could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    let cancelled = false;

    void userApi
      .get(userId)
      .then((result) => {
        if (cancelled) return;
        setUser(result);
        setEmail(result.email);
        setGivenName(result.givenName);
        setFamilyName(result.familyName);
        setPhone(result.phone ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : "The user could not be loaded.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    void userApi
      .listRoles(userId)
      .then((result) => {
        if (!cancelled) setAssignedRoles(result);
      })
      .catch(() => {
        // Role listing errors are surfaced when the admin acts on them.
      });

    void roleApi
      .list()
      .then((result) => {
        if (!cancelled) setAllRoles(result);
      })
      .catch(() => {
        // Role catalog errors are surfaced when the admin acts on them.
      });

    void userApi
      .listEffectiveRoles(userId)
      .then((result) => {
        if (!cancelled) setEffectiveRoles(result);
      })
      .catch(() => {
        // Effective-role errors are surfaced when the admin acts on them.
      });

    void userApi
      .listGroups(userId)
      .then(async (groups) => {
        if (cancelled) return;
        setAssignedGroups(groups);
        const rows: { role: Role; group: Group }[] = [];
        for (const group of groups) {
          if (group.status !== "ACTIVE") continue;
          try {
            const roles = await groupApi.listRoles(group.id);
            for (const role of roles) {
              if (role.status === "ACTIVE") rows.push({ role, group });
            }
          } catch {
            // Group-role errors are surfaced when the admin acts on them.
          }
        }
        rows.sort(
          (left, right) =>
            left.role.name.localeCompare(right.role.name) ||
            left.group.name.localeCompare(right.group.name),
        );
        if (!cancelled) setInheritedRoles(rows);
      })
      .catch(() => {
        // Group listing errors are surfaced when the admin acts on them.
      });

    void groupApi
      .list()
      .then((result) => {
        if (!cancelled) setAllGroups(result);
      })
      .catch(() => {
        // Group catalog errors are surfaced when the admin acts on them.
      });

    void credentialApi
      .status(userId)
      .then((result) => {
        if (!cancelled) setCredential(result);
      })
      .catch(() => {
        // Credential status errors are surfaced when the admin acts on them.
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!email.trim() || !givenName.trim() || !familyName.trim()) {
      setNotice("Email, given name, and family name are required.");
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const updated = await userApi.update(userId, {
        email: email.trim(),
        givenName: givenName.trim(),
        familyName: familyName.trim(),
        phone: phone.trim(),
      });
      setUser(updated);
      setEmail(updated.email);
      setGivenName(updated.givenName);
      setFamilyName(updated.familyName);
      setPhone(updated.phone ?? "");
      setNotice("Profile saved.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The user could not be updated.");
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(action: "activate" | "deactivate") {
    setSaving(true);
    setNotice(null);
    try {
      const updated =
        action === "deactivate"
          ? await userApi.deactivate(userId)
          : await userApi.activate(userId);
      setUser(updated);
      setNotice(action === "deactivate" ? "User deactivated." : "User activated.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The user status could not be changed.");
    } finally {
      setSaving(false);
    }
  }

  async function handleAssignRole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedRoleId) return;

    setRoleError(null);
    try {
      const updated = await userApi.assignRole(userId, selectedRoleId);
      setAssignedRoles(updated);
      setSelectedRoleId("");
    } catch (reason) {
      setRoleError(
        reason instanceof Error ? reason.message : "The role could not be assigned.",
      );
    }
  }

  async function removeRole(roleId: string) {
    setRoleError(null);
    try {
      const updated = await userApi.removeRole(userId, roleId);
      setAssignedRoles(updated);
    } catch (reason) {
      setRoleError(
        reason instanceof Error ? reason.message : "The role could not be removed.",
      );
    }
  }

  async function handleAssignGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGroupId) return;

    setGroupError(null);
    try {
      const updated = await userApi.assignGroup(userId, selectedGroupId);
      setAssignedGroups(updated);
      setSelectedGroupId("");
    } catch (reason) {
      setGroupError(
        reason instanceof Error ? reason.message : "The group could not be assigned.",
      );
    }
  }

  async function removeGroup(groupId: string) {
    setGroupError(null);
    try {
      const updated = await userApi.removeGroup(userId, groupId);
      setAssignedGroups(updated);
    } catch (reason) {
      setGroupError(
        reason instanceof Error ? reason.message : "The group could not be removed.",
      );
    }
  }

  async function handleResetPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (resetPassword.length < 8) {
      setCredentialNotice("New password must be at least 8 characters.");
      return;
    }
    setCredentialBusy(true);
    setCredentialNotice(null);
    try {
      const updated = await credentialApi.resetPassword(
        userId,
        resetPassword,
        requirePasswordChange,
      );
      setCredential(updated);
      setResetPassword("");
      setShowResetPassword(false);
      setRequirePasswordChange(true);
      setCredentialNotice("Password reset.");
    } catch (reason) {
      setCredentialNotice(
        reason instanceof Error ? reason.message : "The password could not be reset.",
      );
    } finally {
      setCredentialBusy(false);
    }
  }

  function handleGeneratePassword() {
    setResetPassword(generatePassword());
    setShowResetPassword(true);
    setCredentialNotice(null);
  }

  async function handleCopyPassword() {
    if (!resetPassword) return;
    try {
      await navigator.clipboard.writeText(resetPassword);
      setCredentialNotice("Password copied to clipboard.");
    } catch {
      setCredentialNotice("Could not copy. Select the password and copy it manually.");
    }
  }

  async function handleUnlock() {
    setCredentialBusy(true);
    setCredentialNotice(null);
    try {
      const updated = await credentialApi.unlock(userId);
      setCredential(updated);
      setCredentialNotice("Account unlocked.");
    } catch (reason) {
      setCredentialNotice(
        reason instanceof Error ? reason.message : "The account could not be unlocked.",
      );
    } finally {
      setCredentialBusy(false);
    }
  }

  if (loading) {
    return <DetailSkeleton />;
  }

  if (error || !user) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">User unavailable</h1>
        <p className="mt-2 text-sm text-red-800">{error ?? "No user was returned."}</p>
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={() => void loadUser()}
            className="rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
          <Link href="/users" className="px-3 py-2 text-sm font-semibold text-red-900">
            Back to users
          </Link>
        </div>
      </section>
    );
  }

  const assignedIds = new Set(assignedRoles.map((role) => role.id));
  const assignableRoles = allRoles.filter(
    (role) => role.status === "ACTIVE" && !assignedIds.has(role.id),
  );
  const assignedGroupIds = new Set(assignedGroups.map((group) => group.id));
  const assignableGroups = allGroups.filter(
    (group) => group.status === "ACTIVE" && !assignedGroupIds.has(group.id),
  );

  return (
    <>
      <Link
        href="/users"
        className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand"
      >
        <span aria-hidden="true">←</span> Users
      </Link>
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            User record
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {user.givenName} {user.familyName}
          </h1>
        </div>
        <UserStatusBadge status={user.status} />
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">User information</h2>
          <p className="mt-1 text-sm text-slate-500">
            Application identity and lifecycle state.
          </p>
        </div>
        <dl className="mt-6 grid gap-6 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Username</dt>
            <dd className="mt-2 font-medium text-slate-950">@{user.username}</dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Status</dt>
            <dd className="mt-2 font-medium text-slate-950">{user.status}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">User ID</dt>
            <dd className="mt-2 break-all font-mono text-sm text-slate-600">{user.id}</dd>
          </div>
        </dl>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Profile</h2>
          <p className="mt-1 text-sm text-slate-500">
            Editable personal details. The username is the stable identity and is not editable.
          </p>
        </div>
        <form onSubmit={handleSave} className="mt-6 grid gap-5 sm:grid-cols-2">
          <ProfileField id="edit-email" label="Email" value={email} onChange={setEmail} type="email" maxLength={320} required />
          <ProfileField id="edit-givenName" label="Given name" value={givenName} onChange={setGivenName} maxLength={100} required />
          <ProfileField id="edit-familyName" label="Family name" value={familyName} onChange={setFamilyName} maxLength={100} required />
          <ProfileField id="edit-phone" label="Phone" value={phone} onChange={setPhone} type="tel" maxLength={40} />
          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save profile"}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Roles</h2>
          <p className="mt-1 text-sm text-slate-500">
            Roles assigned directly to this user. Retired roles remain visible for history. Roles
            gained through groups are shown separately below.
          </p>
        </div>

        {assignedRoles.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No roles have been assigned directly.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {assignedRoles.map((role) => (
              <li key={role.id} className="flex items-center justify-between gap-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="font-semibold text-slate-950">{role.name}</span>
                  <RoleStatusBadge status={role.status} />
                  <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Direct
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => void removeRole(role.id)}
                  className="text-sm font-semibold text-slate-500 hover:text-red-700 hover:underline"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={handleAssignRole} className="mt-6 border-t border-slate-100 pt-6">
          <h3 className="font-bold text-slate-950">Assign a role</h3>
          {roleError ? (
            <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
              {roleError}
            </p>
          ) : null}
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
            <select
              id="assign-role"
              name="roleId"
              value={selectedRoleId}
              onChange={(event) => setSelectedRoleId(event.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none focus:border-brand focus:ring-2 focus:ring-brand-muted sm:max-w-xs"
            >
              <option value="">Select a role…</option>
              {assignableRoles.map((role) => (
                <option key={role.id} value={role.id}>
                  {role.name}
                </option>
              ))}
            </select>
            <button
              type="submit"
              disabled={!selectedRoleId}
              className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Assign role
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Inherited roles</h2>
          <p className="mt-1 text-sm text-slate-500">
            Roles granted through this user&apos;s group memberships, with the group that provides
            each one. Retired roles and retired groups no longer contribute.
          </p>
        </div>

        {inheritedRoles.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No roles are inherited from groups.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {inheritedRoles.map(({ role, group }) => (
              <li key={`${role.id}:${group.id}`} className="flex items-center justify-between gap-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="font-semibold text-slate-950">{role.name}</span>
                  <RoleStatusBadge status={role.status} />
                  <span className="text-sm text-slate-500">via {group.name}</span>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="mt-6 border-t border-slate-100 pt-6">
          <h3 className="font-bold text-slate-950">Effective roles</h3>
          <p className="mt-1 text-sm text-slate-500">
            Direct plus inherited roles, deduplicated. A role gained both directly and through groups
            counts once.
          </p>
          {effectiveRoles.length === 0 ? (
            <p className="mt-4 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
              No effective roles.
            </p>
          ) : (
            <ul className="mt-4 flex flex-wrap gap-2">
              {effectiveRoles.map((role) => (
                <li
                  key={role.id}
                  className="rounded-full border border-slate-300 bg-slate-50 px-3 py-1 text-sm font-medium text-slate-800"
                >
                  {role.name}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Groups</h2>
          <p className="mt-1 text-sm text-slate-500">
            Assign or remove group membership for this user. Retired groups remain visible for history.
          </p>
        </div>

        {assignedGroups.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No groups have been assigned.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {assignedGroups.map((group) => (
              <li key={group.id} className="flex items-center justify-between gap-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="font-semibold text-slate-950">{group.name}</span>
                  <GroupStatusBadge status={group.status} />
                </div>
                <button
                  type="button"
                  onClick={() => void removeGroup(group.id)}
                  className="text-sm font-semibold text-slate-500 hover:text-red-700 hover:underline"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={handleAssignGroup} className="mt-6 border-t border-slate-100 pt-6">
          <h3 className="font-bold text-slate-950">Assign a group</h3>
          {groupError ? (
            <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
              {groupError}
            </p>
          ) : null}
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
            <select
              id="assign-group"
              name="groupId"
              value={selectedGroupId}
              onChange={(event) => setSelectedGroupId(event.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none focus:border-brand focus:ring-2 focus:ring-brand-muted sm:max-w-xs"
            >
              <option value="">Select a group…</option>
              {assignableGroups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.name}
                </option>
              ))}
            </select>
            <button
              type="submit"
              disabled={!selectedGroupId}
              className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Assign group
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Account status</h2>
          <p className="mt-1 text-sm text-slate-500">
            Deactivation preserves the record and history rather than deleting it.
          </p>
        </div>
        {notice ? (
          <p className="mt-5 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
            {notice}
          </p>
        ) : null}
        <div className="mt-5 flex flex-wrap gap-3">
          {user.status === "ACTIVE" ? (
            <button
              type="button"
              disabled={saving}
              onClick={() => void changeStatus("deactivate")}
              className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-2.5 text-sm font-semibold text-amber-800 hover:bg-amber-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-700 disabled:opacity-60"
            >
              Deactivate user
            </button>
          ) : (
            <button
              type="button"
              disabled={saving}
              onClick={() => void changeStatus("activate")}
              className="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 disabled:opacity-60"
            >
              Reactivate user
            </button>
          )}
        </div>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Credential administration</h2>
          <p className="mt-1 text-sm text-slate-500">
            Local credential state for this user. A newly generated temporary password is shown only
            until the form is submitted.
          </p>
        </div>

        {credential ? (
          <div className="mt-6">
            {credential.hasCredential ? (
              <p className="text-sm text-slate-700">
                {credential.locked
                  ? "Account is locked."
                  : "Account is active."}{" "}
                Failed sign-in attempts: {credential.failedAttempts}.
                {credential.passwordChangeRequired
                  ? " Password change is required at next sign-in."
                  : ""}
              </p>
            ) : (
              <p className="text-sm text-slate-700">No credential has been set for this user.</p>
            )}
          </div>
        ) : null}

        {credential?.locked ? (
          <div className="mt-5">
            <button
              type="button"
              disabled={credentialBusy}
              onClick={() => void handleUnlock()}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-700 disabled:opacity-60"
            >
              Unlock account
            </button>
          </div>
        ) : null}

        <form onSubmit={handleResetPassword} className="mt-6 border-t border-slate-100 pt-6">
          <h3 className="font-bold text-slate-950">Reset password</h3>
          {credentialNotice ? (
            <p className="mt-4 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
              {credentialNotice}
            </p>
          ) : null}
          <div className="mt-4 flex flex-wrap items-end gap-3">
            <div className="relative w-full sm:max-w-xs">
              <input
                id="reset-password"
                name="newPassword"
                type={showResetPassword ? "text" : "password"}
                value={resetPassword}
                onChange={(event) => setResetPassword(event.target.value)}
                maxLength={72}
                required
                placeholder="New password"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 pr-16 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted"
              />
              <button
                type="button"
                onClick={() => setShowResetPassword((value) => !value)}
                disabled={!resetPassword}
                className="absolute inset-y-0 right-0 rounded-r-lg px-3 text-xs font-semibold text-slate-600 hover:text-brand disabled:opacity-40"
              >
                {showResetPassword ? "Hide" : "Show"}
              </button>
            </div>
            <button
              type="button"
              onClick={handleGeneratePassword}
              disabled={credentialBusy}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-700 disabled:opacity-60"
            >
              Generate password
            </button>
            <button
              type="button"
              onClick={() => void handleCopyPassword()}
              disabled={!resetPassword}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-700 disabled:opacity-60"
            >
              Copy
            </button>
            <button
              type="submit"
              disabled={credentialBusy}
              className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:opacity-60"
            >
              Reset password
            </button>
          </div>
          <label className="mt-4 flex items-start gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={requirePasswordChange}
              onChange={(event) => setRequirePasswordChange(event.target.checked)}
              disabled={credentialBusy}
              className="mt-0.5 size-4 rounded border-slate-300 text-brand focus:ring-brand"
            />
            <span>Require password change at next login</span>
          </label>
        </form>
      </section>
    </>
  );
}

function ProfileField({
  id,
  label,
  value,
  onChange,
  type = "text",
  maxLength,
  required = false,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  maxLength: number;
  required?: boolean;
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold text-slate-900">
        {label}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        maxLength={maxLength}
        required={required}
        className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
      />
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-live="polite" aria-busy="true">
      <span className="sr-only">Loading user</span>
      <div className="mb-5 h-4 w-28 rounded bg-slate-200" />
      <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
