"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { RoleStatusBadge } from "@/components/role-list";
import { roleApi, type Role } from "@/lib/api";

export function RoleDetail({ roleId }: { roleId: string }) {
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadRole = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await roleApi.get(roleId);
      setRole(loaded);
      setName(loaded.name);
      setDescription(loaded.description ?? "");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The role could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    let cancelled = false;

    void roleApi
      .get(roleId)
      .then((result) => {
        if (cancelled) return;
        setRole(result);
        setName(result.name);
        setDescription(result.description ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : "The role could not be loaded.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [roleId]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!name.trim()) {
      setNotice("Enter a role name.");
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const updated = await roleApi.update(roleId, {
        name: name.trim(),
        description: description.trim(),
      });
      setRole(updated);
      setName(updated.name);
      setDescription(updated.description ?? "");
      setNotice("Role saved.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The role could not be updated.");
    } finally {
      setSaving(false);
    }
  }

  async function retire() {
    if (!window.confirm("Retire this role? Existing assignments are preserved.")) {
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const retired = await roleApi.retire(roleId);
      setRole(retired);
      setNotice("Role retired.");
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The role could not be retired.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <DetailSkeleton />;
  }

  if (error || !role) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">Role unavailable</h1>
        <p className="mt-2 text-sm text-red-800">{error ?? "No role was returned."}</p>
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={() => void loadRole()}
            className="rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
          <Link href="/roles" className="px-3 py-2 text-sm font-semibold text-red-900">
            Back to roles
          </Link>
        </div>
      </section>
    );
  }

  return (
    <>
      <Link
        href="/roles"
        className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand"
      >
        <span aria-hidden="true">←</span> Roles
      </Link>
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            Role record
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {role.name}
          </h1>
        </div>
        <RoleStatusBadge status={role.status} />
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Role details</h2>
          <p className="mt-1 text-sm text-slate-500">
            Name and description shared across the application.
          </p>
        </div>
        <form onSubmit={handleSave} className="mt-6 grid gap-5">
          <div>
            <label htmlFor="edit-name" className="block text-sm font-semibold text-slate-900">
              Role name
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
              {saving ? "Saving…" : "Save role"}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Role status</h2>
          <p className="mt-1 text-sm text-slate-500">
            Retiring a role keeps existing assignments for audit rather than deleting them.
          </p>
        </div>
        {notice ? (
          <p className="mt-5 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
            {notice}
          </p>
        ) : null}
        {role.status === "ACTIVE" ? (
          <div className="mt-5">
            <button
              type="button"
              disabled={saving}
              onClick={() => void retire()}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-700 disabled:opacity-60"
            >
              Retire role
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
      <span className="sr-only">Loading role</span>
      <div className="mb-5 h-4 w-28 rounded bg-slate-200" />
      <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
