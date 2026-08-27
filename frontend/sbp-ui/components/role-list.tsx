"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { roleApi, type Role, type RoleStatus } from "@/lib/api";

export function RoleList() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRoles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRoles(await roleApi.list());
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Roles could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    void roleApi
      .list()
      .then((result) => {
        if (!cancelled) setRoles(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Roles could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function retire(role: Role) {
    if (
      !window.confirm(
        `Retire the "${role.name}" role? Existing assignments are preserved.`,
      )
    ) {
      return;
    }

    setError(null);
    try {
      await roleApi.retire(role.id);
      await loadRoles();
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The role could not be retired.",
      );
    }
  }

  if (loading) {
    return (
      <div className="grid gap-3" aria-live="polite" aria-busy="true">
        <span className="sr-only">Loading roles</span>
        {[0, 1, 2].map((item) => (
          <div
            key={item}
            className="h-20 animate-pulse rounded-2xl border border-slate-200 bg-white"
          />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h2 className="font-semibold text-red-950">Roles are unavailable</h2>
        <p className="mt-2 text-sm text-red-800">{error}</p>
        <button
          type="button"
          onClick={() => void loadRoles()}
          className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
        >
          Try again
        </button>
      </div>
    );
  }

  if (roles.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
        <div className="mx-auto mb-4 grid size-12 place-items-center rounded-full bg-brand-soft text-xl text-brand-strong">
          +
        </div>
        <h2 className="font-semibold text-slate-950">No roles yet</h2>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
          Create the first role to begin grouping what people are allowed to do.
        </p>
        <Link
          href="/roles/new"
          className="mt-6 inline-flex rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Create role
        </Link>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden grid-cols-[1fr_9rem_9rem_7rem] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
        <span>Role</span>
        <span>Status</span>
        <span className="text-right">Action</span>
        <span aria-hidden="true" />
      </div>
      <ul className="divide-y divide-slate-100">
        {roles.map((role) => (
          <li
            key={role.id}
            className="grid gap-3 px-5 py-5 transition-colors hover:bg-slate-50 sm:grid-cols-[1fr_9rem_9rem_7rem] sm:items-center sm:gap-6 sm:px-6"
          >
            <div>
              <p className="font-semibold text-slate-950">{role.name}</p>
              {role.description ? (
                <p className="mt-1 truncate text-sm text-slate-500">{role.description}</p>
              ) : null}
            </div>
            <div>
              <RoleStatusBadge status={role.status} />
            </div>
            <div className="flex items-center gap-3 sm:justify-end">
              {role.status === "ACTIVE" ? (
                <button
                  type="button"
                  onClick={() => void retire(role)}
                  className="text-sm font-semibold text-slate-500 hover:text-slate-800 hover:underline"
                >
                  Retire
                </button>
              ) : null}
              <Link
                href={`/roles/${role.id}`}
                className="text-sm font-semibold text-brand hover:text-brand-strong hover:underline"
              >
                Open <span aria-hidden="true">→</span>
              </Link>
            </div>
            <span aria-hidden="true" />
          </li>
        ))}
      </ul>
    </div>
  );
}

export function RoleStatusBadge({ status }: { status: RoleStatus }) {
  const classes =
    status === "ACTIVE"
      ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
      : "bg-slate-100 text-slate-600 ring-slate-300";

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${classes}`}
    >
      {status}
    </span>
  );
}
