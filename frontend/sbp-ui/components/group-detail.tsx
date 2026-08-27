"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { GroupStatusBadge } from "@/components/group-list";
import { RoleStatusBadge } from "@/components/role-list";
import { groupApi, roleApi, type Group, type Role, type User } from "@/lib/api";

export function GroupDetail({ groupId }: { groupId: string }) {
  const [group, setGroup] = useState<Group | null>(null);
  const [members, setMembers] = useState<User[]>([]);
  const [groupRoles, setGroupRoles] = useState<Role[]>([]);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [roleError, setRoleError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadGroup = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await groupApi.get(groupId);
      setGroup(loaded);
      setName(loaded.name);
      setDescription(loaded.description ?? "");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The group could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    let cancelled = false;

    void groupApi
      .get(groupId)
      .then((result) => {
        if (cancelled) return;
        setGroup(result);
        setName(result.name);
        setDescription(result.description ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : "The group could not be loaded.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    void groupApi
      .members(groupId)
      .then((result) => {
        if (!cancelled) setMembers(result);
      })
      .catch(() => {
        // Member listing errors are surfaced when the admin acts on them.
      });

    void groupApi
      .listRoles(groupId)
      .then((result) => {
        if (!cancelled) setGroupRoles(result);
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

    return () => {
      cancelled = true;
    };
  }, [groupId]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!name.trim()) {
      setNotice("Enter a group name.");
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const updated = await groupApi.update(groupId, {
        name: name.trim(),
        description: description.trim(),
      });
      setGroup(updated);
      setName(updated.name);
      setDescription(updated.description ?? "");
      setNotice("Group saved.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The group could not be updated.");
    } finally {
      setSaving(false);
    }
  }

  async function retire() {
    if (!window.confirm("Retire this group? Existing memberships are preserved.")) {
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const retired = await groupApi.retire(groupId);
      setGroup(retired);
      setNotice("Group retired.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The group could not be retired.");
    } finally {
      setSaving(false);
    }
  }

  async function handleAssignRole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedRoleId) return;

    setRoleError(null);
    try {
      const updated = await groupApi.assignRole(groupId, selectedRoleId);
      setGroupRoles(updated);
      setSelectedRoleId("");
    } catch (reason) {
      setRoleError(reason instanceof Error ? reason.message : "The role could not be assigned.");
    }
  }

  async function handleRemoveRole(roleId: string) {
    setRoleError(null);
    try {
      const updated = await groupApi.removeRole(groupId, roleId);
      setGroupRoles(updated);
    } catch (reason) {
      setRoleError(reason instanceof Error ? reason.message : "The role could not be removed.");
    }
  }

  if (loading) {
    return <DetailSkeleton />;
  }

  if (error || !group) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">Group unavailable</h1>
        <p className="mt-2 text-sm text-red-800">{error ?? "No group was returned."}</p>
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={() => void loadGroup()}
            className="rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
          <Link href="/groups" className="px-3 py-2 text-sm font-semibold text-red-900">
            Back to groups
          </Link>
        </div>
      </section>
    );
  }

  const assignedRoleIds = new Set(groupRoles.map((role) => role.id));
  const assignableRoles = allRoles.filter(
    (role) => role.status === "ACTIVE" && !assignedRoleIds.has(role.id),
  );

  return (
    <>
      <Link
        href="/groups"
        className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand"
      >
        <span aria-hidden="true">←</span> Groups
      </Link>
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            Group record
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {group.name}
          </h1>
        </div>
        <GroupStatusBadge status={group.status} />
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Group details</h2>
          <p className="mt-1 text-sm text-slate-500">
            Name and description shared across the application.
          </p>
        </div>
        <form onSubmit={handleSave} className="mt-6 grid gap-5">
          <div>
            <label htmlFor="edit-name" className="block text-sm font-semibold text-slate-900">
              Group name
            </label>
            <input
              id="edit-name"
              name="name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={100}
              required
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <label htmlFor="edit-description" className="block text-sm font-semibold text-slate-900">
              Description
            </label>
            <textarea
              id="edit-description"
              name="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={3}
              maxLength={500}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save group"}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Members</h2>
          <p className="mt-1 text-sm text-slate-500">
            Users assigned to this group. Manage membership from the user&apos;s detail page.
          </p>
        </div>

        {members.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No users have been assigned to this group.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {members.map((member) => (
              <li key={member.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="font-semibold text-slate-950">
                    {member.givenName} {member.familyName}
                  </p>
                  <p className="mt-1 text-sm text-slate-500">@{member.username}</p>
                </div>
                <Link
                  href={`/users/${member.id}`}
                  className="text-sm font-semibold text-brand hover:text-brand-strong hover:underline"
                >
                  Open user
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Roles</h2>
          <p className="mt-1 text-sm text-slate-500">
            Members of this group inherit these roles. Retired roles remain visible for history but
            no longer flow to members&apos; effective roles.
          </p>
        </div>

        {groupRoles.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No roles have been assigned to this group.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {groupRoles.map((role) => (
              <li key={role.id} className="flex items-center justify-between gap-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="font-semibold text-slate-950">{role.name}</span>
                  <RoleStatusBadge status={role.status} />
                </div>
                <button
                  type="button"
                  onClick={() => void handleRemoveRole(role.id)}
                  className="text-sm font-semibold text-slate-500 hover:text-red-700 hover:underline"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}

        {group.status === "ACTIVE" ? (
          <form onSubmit={handleAssignRole} className="mt-6 border-t border-slate-100 pt-6">
            <h3 className="font-bold text-slate-950">Assign a role</h3>
            {roleError ? (
              <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
                {roleError}
              </p>
            ) : null}
            <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
              <select
                id="assign-group-role"
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
        ) : roleError ? (
          <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
            {roleError}
          </p>
        ) : null}
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Group status</h2>
          <p className="mt-1 text-sm text-slate-500">
            Retiring a group keeps existing memberships for audit rather than deleting them.
          </p>
        </div>
        {notice ? (
          <p className="mt-5 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
            {notice}
          </p>
        ) : null}
        {group.status === "ACTIVE" ? (
          <div className="mt-5">
            <button
              type="button"
              disabled={saving}
              onClick={() => void retire()}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-700 disabled:opacity-60"
            >
              Retire group
            </button>
          </div>
        ) : null}
      </section>
    </>
  );
}

function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-live="polite" aria-busy="true">
      <span className="sr-only">Loading group</span>
      <div className="mb-5 h-4 w-28 rounded bg-slate-200" />
      <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
