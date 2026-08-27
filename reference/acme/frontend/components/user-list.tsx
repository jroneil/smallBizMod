"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  groupApi,
  roleApi,
  userApi,
  type Group,
  type Role,
  type User,
  type UserStatus,
} from "@/lib/api";

export function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<UserStatus | "">("");
  const [roleId, setRoleId] = useState("");
  const [groupId, setGroupId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadUsers = useCallback(
    async (q: string, s: UserStatus | "", r: string, g: string) => {
      setLoading(true);
      setError(null);
      try {
        setUsers(
          await userApi.list(q.trim() || undefined, s || undefined, r || undefined, g || undefined),
        );
      } catch (reason) {
        setError(
          reason instanceof Error ? reason.message : "Users could not be loaded.",
        );
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    let cancelled = false;

    void userApi
      .list()
      .then((result) => {
        if (!cancelled) setUsers(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Users could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    void roleApi
      .list()
      .then((result) => {
        if (!cancelled) setRoles(result);
      })
      .catch(() => {
        // Role filter options are optional; search still works without them.
      });

    void groupApi
      .list()
      .then((result) => {
        if (!cancelled) setGroups(result);
      })
      .catch(() => {
        // Group filter options are optional; search still works without them.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void loadUsers(query, status, roleId, groupId);
  }

  function applyFilters(nextStatus: UserStatus | "" = status, nextRole: string = roleId, nextGroup: string = groupId) {
    setStatus(nextStatus);
    setRoleId(nextRole);
    setGroupId(nextGroup);
    void loadUsers(query, nextStatus, nextRole, nextGroup);
  }

  async function handleExport() {
    try {
      const users = await userApi.export();
      const blob = new Blob([JSON.stringify(users, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "users.json";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The export could not be generated.");
    }
  }

  const hasFilters = Boolean(query || status || roleId || groupId);

  return (
    <div>
      <form
        onSubmit={handleSearch}
        className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-2 lg:grid-cols-5"
      >
        <div className="lg:col-span-2">
          <label htmlFor="user-query" className="block text-xs font-bold uppercase tracking-wider text-slate-500">
            Search
          </label>
          <input
            id="user-query"
            name="query"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Name, email, or username"
            className="mt-2 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted"
          />
        </div>
        <FilterSelect
          id="user-status"
          label="Status"
          value={status}
          onChange={(value) => applyFilters(value as UserStatus | "")}
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </FilterSelect>
        <FilterSelect
          id="user-role"
          label="Role"
          value={roleId}
          onChange={(value) => applyFilters(status, value, groupId)}
        >
          <option value="">All roles</option>
          {roles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          id="user-group"
          label="Group"
          value={groupId}
          onChange={(value) => applyFilters(status, roleId, value)}
        >
          <option value="">All groups</option>
          {groups.map((group) => (
            <option key={group.id} value={group.id}>
              {group.name}
            </option>
          ))}
        </FilterSelect>
        <div className="flex items-end gap-3 lg:col-span-5">
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60 sm:px-6"
          >
            {loading ? "Searching…" : "Search"}
          </button>
          <button
            type="button"
            onClick={() => void handleExport()}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            Export
          </button>
        </div>
      </form>

      {loading ? (
        <div className="grid gap-3" aria-live="polite" aria-busy="true">
          <span className="sr-only">Loading users</span>
          {[0, 1, 2].map((item) => (
            <div
              key={item}
              className="h-20 animate-pulse rounded-2xl border border-slate-200 bg-white"
            />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
          <h2 className="font-semibold text-red-950">Users are unavailable</h2>
          <p className="mt-2 text-sm text-red-800">{error}</p>
          <button
            type="button"
            onClick={() => void loadUsers(query, status, roleId, groupId)}
            className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
        </div>
      ) : users.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
          <div className="mx-auto mb-4 grid size-12 place-items-center rounded-full bg-brand-soft text-xl text-brand-strong">
            +
          </div>
          <h2 className="font-semibold text-slate-950">No users found</h2>
          <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
            {hasFilters
              ? "Adjust your search or filters to find who you are looking for."
              : "Create the first user to begin managing access to the application."}
          </p>
          {!hasFilters ? (
            <Link
              href="/users/new"
              className="mt-6 inline-flex rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
              Create user
            </Link>
          ) : null}
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="hidden grid-cols-[1fr_10rem_9rem_7rem] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
            <span>User</span>
            <span>Email</span>
            <span>Status</span>
            <span className="text-right">Action</span>
          </div>
          <ul className="divide-y divide-slate-100">
            {users.map((user) => (
              <li
                key={user.id}
                className="grid gap-3 px-5 py-5 transition-colors hover:bg-slate-50 sm:grid-cols-[1fr_10rem_9rem_7rem] sm:items-center sm:gap-6 sm:px-6"
              >
                <div>
                  <p className="font-semibold text-slate-950">
                    {user.givenName} {user.familyName}
                  </p>
                  <p className="mt-1 truncate text-sm text-slate-500">
                    @{user.username}
                  </p>
                </div>
                <div className="truncate text-sm text-slate-700">{user.email}</div>
                <div>
                  <UserStatusBadge status={user.status} />
                </div>
                <Link
                  href={`/users/${user.id}`}
                  className="text-sm font-semibold text-brand hover:text-brand-strong hover:underline sm:text-right"
                >
                  Open <span aria-hidden="true">→</span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function FilterSelect({
  id,
  label,
  value,
  onChange,
  children,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-xs font-bold uppercase tracking-wider text-slate-500">
        {label}
      </label>
      <select
        id={id}
        name={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none focus:border-brand focus:ring-2 focus:ring-brand-muted"
      >
        {children}
      </select>
    </div>
  );
}

export function UserStatusBadge({ status }: { status: UserStatus }) {
  const classes =
    status === "ACTIVE"
      ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
      : "bg-amber-50 text-amber-700 ring-amber-200";

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${classes}`}
    >
      {status}
    </span>
  );
}
