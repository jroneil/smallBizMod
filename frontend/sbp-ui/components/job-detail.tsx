"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { DocumentsPanel } from "@/components/documents-panel";
import { JobPriorityBadge, JobStatusBadge } from "@/components/job-list";
import {
  customerApi,
  jobApi,
  type Customer,
  type Job,
  type JobAuditEvent,
  type JobPriority,
} from "@/lib/api";

export function JobDetail({ jobId }: { jobId: string }) {
  const [job, setJob] = useState<Job | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [auditEvents, setAuditEvents] = useState<JobAuditEvent[]>([]);
  const [auditLoading, setAuditLoading] = useState(true);
  const [auditError, setAuditError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<JobPriority>("NORMAL");
  const [scheduledDate, setScheduledDate] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadJob = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await jobApi.get(jobId);
      setJob(loaded);
      setTitle(loaded.title);
      setDescription(loaded.description ?? "");
      setPriority(loaded.priority);
      setScheduledDate(loaded.scheduledDate ?? "");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The job could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [jobId]);

  const loadAuditEvents = useCallback(async () => {
    try {
      setAuditEvents(await jobApi.listAuditEvents(jobId));
    } catch (reason) {
      setAuditError(
        reason instanceof Error ? reason.message : "The audit history could not be loaded.",
      );
    }
  }, [jobId]);

  useEffect(() => {
    let cancelled = false;

    void jobApi
      .get(jobId)
      .then((result) => {
        if (cancelled) return;
        setJob(result);
        setTitle(result.title);
        setDescription(result.description ?? "");
        setPriority(result.priority);
        setScheduledDate(result.scheduledDate ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "The job could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    void customerApi
      .list()
      .then((result) => {
        if (!cancelled) setCustomers(result);
      })
      .catch(() => {
        // Customer names are presentation only; the detail still renders without them.
      });

    void jobApi
      .listAuditEvents(jobId)
      .then((result) => {
        if (!cancelled) setAuditEvents(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setAuditError(
            reason instanceof Error ? reason.message : "The audit history could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setAuditLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [jobId]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!title.trim()) {
      setNotice("Job title is required.");
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const updated = await jobApi.update(jobId, {
        title: title.trim(),
        description: description.trim() || "",
        priority,
        scheduledDate: scheduledDate || null,
      });
      setJob(updated);
      setTitle(updated.title);
      setDescription(updated.description ?? "");
      setPriority(updated.priority);
      setScheduledDate(updated.scheduledDate ?? "");
      setNotice("Job saved.");
      void loadAuditEvents();
    } catch (reason) {
      setNotice(reason instanceof Error ? reason.message : "The job could not be updated.");
    } finally {
      setSaving(false);
    }
  }

  async function runLifecycleAction(action: "start" | "complete" | "cancel") {
    setSaving(true);
    setNotice(null);
    try {
      const updated =
        action === "start"
          ? await jobApi.start(jobId)
          : action === "complete"
            ? await jobApi.complete(jobId)
            : await jobApi.cancel(jobId);
      setJob(updated);
      setNotice(
        action === "start"
          ? "Job started."
          : action === "complete"
            ? "Job completed."
            : "Job cancelled.",
      );
      void loadAuditEvents();
    } catch (reason) {
      setNotice(
        reason instanceof Error
          ? reason.message
          : "The job lifecycle could not be changed.",
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <DetailSkeleton />;
  }

  if (error || !job) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">Job unavailable</h1>
        <p className="mt-2 text-sm text-red-800">{error ?? "No job was returned."}</p>
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={() => void loadJob()}
            className="rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
          <Link href="/jobs" className="px-3 py-2 text-sm font-semibold text-red-900">
            Back to jobs
          </Link>
        </div>
      </section>
    );
  }

  const customerName =
    customers.find((customer) => customer.id === job.customerId)?.name ?? "Customer";

  return (
    <>
      <Link
        href="/jobs"
        className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand"
      >
        <span aria-hidden="true">←</span> Jobs
      </Link>
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            Job record
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {job.title}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <JobStatusBadge status={job.status} />
          <JobPriorityBadge priority={job.priority} />
        </div>
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Job information</h2>
          <p className="mt-1 text-sm text-slate-500">
            Core work details and lifecycle state.
          </p>
        </div>
        <dl className="mt-6 grid gap-6 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Customer</dt>
            <dd className="mt-2 font-medium text-slate-950">{customerName}</dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Status</dt>
            <dd className="mt-2 font-medium text-slate-950">{job.status.replace("_", " ")}</dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Priority</dt>
            <dd className="mt-2 font-medium text-slate-950">{job.priority}</dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Scheduled date</dt>
            <dd className="mt-2 font-medium text-slate-950">
              {job.scheduledDate
                ? new Date(`${job.scheduledDate}T00:00:00`).toLocaleDateString(undefined, {
                    month: "short",
                    day: "numeric",
                    year: "numeric",
                  })
                : "Not scheduled"}
            </dd>
          </div>
          {job.description ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Description</dt>
              <dd className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">
                {job.description}
              </dd>
            </div>
          ) : null}
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Created</dt>
            <dd className="mt-2 text-sm text-slate-600">
              {new Date(job.createdAt).toLocaleString(undefined, {
                dateStyle: "medium",
                timeStyle: "short",
              })}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Last updated</dt>
            <dd className="mt-2 text-sm text-slate-600">
              {new Date(job.updatedAt).toLocaleString(undefined, {
                dateStyle: "medium",
                timeStyle: "short",
              })}
            </dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Job ID</dt>
            <dd className="mt-2 break-all font-mono text-sm text-slate-600">{job.id}</dd>
          </div>
        </dl>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Details</h2>
          <p className="mt-1 text-sm text-slate-500">
            Edit the work details. Lifecycle state changes through the actions below, never through
            this form.
          </p>
        </div>
        {notice ? (
          <p className="mt-5 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
            {notice}
          </p>
        ) : null}
        <form onSubmit={handleSave} className="mt-6 grid gap-5 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label htmlFor="edit-title" className="block text-sm font-semibold text-slate-900">
              Title
            </label>
            <input
              id="edit-title"
              name="title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={200}
              required
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <label htmlFor="edit-priority" className="block text-sm font-semibold text-slate-900">
              Priority
            </label>
            <select
              id="edit-priority"
              name="priority"
              value={priority}
              onChange={(event) => setPriority(event.target.value as JobPriority)}
              className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            >
              <option value="LOW">Low</option>
              <option value="NORMAL">Normal</option>
              <option value="HIGH">High</option>
            </select>
          </div>
          <div>
            <label htmlFor="edit-scheduledDate" className="block text-sm font-semibold text-slate-900">
              Scheduled date
            </label>
            <input
              id="edit-scheduledDate"
              name="scheduledDate"
              type="date"
              value={scheduledDate}
              onChange={(event) => setScheduledDate(event.target.value)}
              className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="edit-description" className="block text-sm font-semibold text-slate-900">
              Description
            </label>
            <textarea
              id="edit-description"
              name="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={4}
              maxLength={2000}
              className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save details"}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Lifecycle</h2>
          <p className="mt-1 text-sm text-slate-500">
            Move the job from Open through In progress to Completed, or cancel it. Completed and
            cancelled jobs are kept for history and cannot be reopened.
          </p>
        </div>
        <div className="mt-5 flex flex-wrap gap-3">
          {job.status === "OPEN" ? (
            <>
              <button
                type="button"
                disabled={saving}
                onClick={() => void runLifecycleAction("start")}
                className="rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-sky-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700 disabled:opacity-60"
              >
                Start job
              </button>
              <CancelButton saving={saving} onCancel={() => void runLifecycleAction("cancel")} />
            </>
          ) : job.status === "IN_PROGRESS" ? (
            <>
              <button
                type="button"
                disabled={saving}
                onClick={() => void runLifecycleAction("complete")}
                className="rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 disabled:opacity-60"
              >
                Complete job
              </button>
              <CancelButton saving={saving} onCancel={() => void runLifecycleAction("cancel")} />
            </>
          ) : (
            <p className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
              This job is {job.status.toLowerCase()} and cannot be changed further.
            </p>
          )}
        </div>
      </section>

      <DocumentsPanel ownerType="JOB" ownerId={job.id} />

      <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Audit history</h2>
          <p className="mt-1 text-sm text-slate-500">
            Who changed this job, when, and which fields. Audit records never contain field values.
          </p>
        </div>
        {auditLoading ? (
          <p className="mt-6 text-sm text-slate-500" aria-live="polite">Loading audit history…</p>
        ) : auditError ? (
          <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-800" role="alert">{auditError}</p>
        ) : auditEvents.length === 0 ? (
          <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
            No audit events have been recorded for this job.
          </p>
        ) : (
          <ul className="mt-6 divide-y divide-slate-100">
            {auditEvents.map((event) => (
              <li key={event.id} className="flex flex-col gap-2 py-4 first:pt-2 last:pb-2 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <p className="font-semibold text-slate-950">{auditActionLabel(event.action)}</p>
                  <p className="mt-1 text-sm text-slate-600">
                    by <span className="font-medium text-slate-800">{event.actor ?? "system"}</span>{" "}
                    on{" "}
                    {new Date(event.occurredAt).toLocaleString(undefined, {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })}
                  </p>
                  {event.detail ? (
                    <p className="mt-1 font-mono text-xs text-slate-500">{event.detail}</p>
                  ) : null}
                </div>
                {event.changedFields.length > 0 ? (
                  <div className="flex shrink-0 flex-wrap gap-1.5 sm:max-w-xs sm:justify-end">
                    {event.changedFields.map((field) => (
                      <span
                        key={field}
                        className="rounded-full border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-xs font-medium text-slate-600"
                      >
                        {field}
                      </span>
                    ))}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function auditActionLabel(action: string): string {
  switch (action) {
    case "JOB_CREATED":
      return "Job created";
    case "JOB_UPDATED":
      return "Job updated";
    case "JOB_STATUS_CHANGED":
      return "Status changed";
    default:
      return action;
  }
}

function CancelButton({ saving, onCancel }: { saving: boolean; onCancel: () => void }) {
  return (
    <button
      type="button"
      disabled={saving}
      onClick={onCancel}
      className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-2.5 text-sm font-semibold text-amber-800 hover:bg-amber-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-700 disabled:opacity-60"
    >
      Cancel job
    </button>
  );
}

function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-live="polite" aria-busy="true">
      <span className="sr-only">Loading job</span>
      <div className="mb-5 h-4 w-28 rounded bg-slate-200" />
      <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
