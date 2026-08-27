"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { auditApi, type AuditEvent } from "@/lib/api";

const ACTIONS = [
  "USER_CREATED",
  "USER_UPDATED",
  "USER_DEACTIVATED",
  "USER_ACTIVATED",
  "ROLE_CREATED",
  "ROLE_UPDATED",
  "ROLE_RETIRED",
  "GROUP_CREATED",
  "GROUP_UPDATED",
  "GROUP_RETIRED",
  "ROLE_ASSIGNED",
  "ROLE_REMOVED",
  "GROUP_ASSIGNED",
  "GROUP_REMOVED",
];

const SUBJECT_TYPES = ["USER", "ROLE", "GROUP"];

export function AuditLog() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [actor, setActor] = useState("");
  const [subjectType, setSubjectType] = useState("");
  const [action, setAction] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (filter: { actor?: string; subjectType?: string; action?: string }) => {
      setLoading(true);
      setError(null);
      try {
        setEvents(await auditApi.list(filter));
      } catch (reason) {
        setError(
          reason instanceof Error ? reason.message : "The audit log could not be loaded.",
        );
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    let cancelled = false;

    void auditApi
      .list()
      .then((result) => {
        if (!cancelled) setEvents(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "The audit log could not be loaded.",
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

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void load({
      actor: actor.trim() || undefined,
      subjectType: subjectType || undefined,
      action: action || undefined,
    });
  }

  return (
    <div>
      <form
        onSubmit={handleSearch}
        className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-2 lg:grid-cols-4"
      >
        <FilterField label="Actor">
          <input
            id="audit-actor"
            type="text"
            value={actor}
            onChange={(event) => setActor(event.target.value)}
            placeholder="Who made the change"
            className="mt-2 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted"
          />
        </FilterField>
        <FilterField label="Subject type">
          <select
            id="audit-subject-type"
            value={subjectType}
            onChange={(event) => setSubjectType(event.target.value)}
            className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none focus:border-brand focus:ring-2 focus:ring-brand-muted"
          >
            <option value="">All types</option>
            {SUBJECT_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </FilterField>
        <FilterField label="Event">
          <select
            id="audit-action"
            value={action}
            onChange={(event) => setAction(event.target.value)}
            className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none focus:border-brand focus:ring-2 focus:ring-brand-muted"
          >
            <option value="">All events</option>
            {ACTIONS.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </FilterField>
        <div className="flex items-end">
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
          >
            {loading ? "Loading…" : "Filter"}
          </button>
        </div>
      </form>

      {loading ? (
        <div className="grid gap-3" aria-live="polite" aria-busy="true">
          <span className="sr-only">Loading audit log</span>
          {[0, 1, 2].map((item) => (
            <div
              key={item}
              className="h-16 animate-pulse rounded-2xl border border-slate-200 bg-white"
            />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
          <h2 className="font-semibold text-red-950">Audit log unavailable</h2>
          <p className="mt-2 text-sm text-red-800">{error}</p>
          <button
            type="button"
            onClick={() =>
              void load({
                actor: actor.trim() || undefined,
                subjectType: subjectType || undefined,
                action: action || undefined,
              })
            }
            className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
        </div>
      ) : events.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
          <h2 className="font-semibold text-slate-950">No audit events</h2>
          <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
            {actor || subjectType || action
              ? "Adjust your filters to find matching events."
              : "Maintenance changes will appear here once they occur."}
          </p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="hidden grid-cols-[11rem_1fr_10rem_1fr] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
            <span>Time</span>
            <span>Event</span>
            <span>Actor</span>
            <span>Subject</span>
          </div>
          <ul className="divide-y divide-slate-100">
            {events.map((event) => (
              <li
                key={event.id}
                className="grid gap-2 px-5 py-4 sm:grid-cols-[11rem_1fr_10rem_1fr] sm:items-center sm:gap-6 sm:px-6"
              >
                <div className="text-sm text-slate-500">
                  {new Date(event.occurredAt).toLocaleString()}
                </div>
                <div>
                  <p className="font-mono text-sm font-semibold text-slate-900">
                    {event.action}
                  </p>
                  {event.changedFields.length > 0 ? (
                    <p className="mt-1 truncate text-xs text-slate-500">
                      fields: {event.changedFields.join(", ")}
                    </p>
                  ) : null}
                </div>
                <div className="truncate text-sm text-slate-700">
                  {event.actor ?? "—"}
                </div>
                <div className="truncate text-sm text-slate-700">
                  {event.subjectType}:{event.subjectId.slice(0, 8)}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function FilterField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
        {label}
      </label>
      {children}
    </div>
  );
}
