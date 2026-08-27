"use client";

import { useCallback, useEffect, useState } from "react";
import { permissionApi, type Permission, type Role } from "@/lib/api";

interface PermissionRow {
  permission: Permission;
  roles: Role[];
}

export function PermissionCatalog() {
  const [rows, setRows] = useState<PermissionRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const permissions = await permissionApi.list();
      const populated = await Promise.all(
        permissions.map(async (permission) => ({
          permission,
          roles: await permissionApi.listRolesByPermission(permission.key),
        })),
      );
      setRows(populated);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The permission catalog could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        const permissions = await permissionApi.list();
        const populated = await Promise.all(
          permissions.map(async (permission) => ({
            permission,
            roles: await permissionApi.listRolesByPermission(permission.key),
          })),
        );
        if (!cancelled) setRows(populated);
      } catch (reason: unknown) {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "The permission catalog could not be loaded.",
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="grid gap-3" aria-live="polite" aria-busy="true">
        <span className="sr-only">Loading permission catalog</span>
        {[0, 1, 2].map((item) => (
          <div
            key={item}
            className="h-16 animate-pulse rounded-2xl border border-slate-200 bg-white"
          />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h2 className="font-semibold text-red-950">Permission catalog unavailable</h2>
        <p className="mt-2 text-sm text-red-800">{error}</p>
        <button
          type="button"
          onClick={() => void load()}
          className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
        >
          Try again
        </button>
      </div>
    );
  }

  if (rows.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
        <h2 className="font-semibold text-slate-950">No permissions defined</h2>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
          The permission catalog is defined by the customer application. None are present yet.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden grid-cols-[1fr_1.5fr_1fr] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
        <span>Permission</span>
        <span>Description</span>
        <span>Granted to roles</span>
      </div>
      <ul className="divide-y divide-slate-100">
        {rows.map(({ permission, roles }) => (
          <li
            key={permission.id}
            className="grid gap-3 px-5 py-4 sm:grid-cols-[1fr_1.5fr_1fr] sm:items-center sm:gap-6 sm:px-6"
          >
            <div className="truncate font-mono text-sm font-semibold text-slate-900">
              {permission.key}
            </div>
            <div className="text-sm text-slate-700">{permission.description}</div>
            <div className="text-sm text-slate-700">
              {roles.length === 0
                ? "—"
                : roles.map((role) => role.name).join(", ")}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
