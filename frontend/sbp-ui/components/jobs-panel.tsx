"use client";

import Link from "next/link";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import { JobPriorityBadge, JobStatusBadge } from "@/components/job-list";
import { jobApi, type Job, type JobPriority } from "@/lib/api";

export function JobsPanel({ customerId }: { customerId: string }) {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<JobPriority>("NORMAL");
  const [scheduledDate, setScheduledDate] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadJobs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setJobs(await jobApi.listByCustomer(customerId));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Jobs could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    let cancelled = false;

    void jobApi
      .listByCustomer(customerId)
      .then((result) => {
        if (!cancelled) setJobs(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : "Jobs could not be loaded.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [customerId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim()) {
      setError("Enter a job title.");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const created = await jobApi.create({
        customerId,
        title: title.trim(),
        description: description.trim() || "",
        priority,
        scheduledDate: scheduledDate || null,
      });
      setJobs((current) => [created, ...current]);
      setTitle("");
      setDescription("");
      setPriority("NORMAL");
      setScheduledDate("");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The job could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-lg font-bold text-slate-950">Jobs</h2>
        <p className="mt-1 text-sm text-slate-500">
          Work recorded for this customer, most recently created first.
        </p>
      </div>

      {loading ? (
        <p className="mt-6 text-sm text-slate-500" aria-live="polite">Loading jobs…</p>
      ) : error && jobs.length === 0 ? (
        <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>
      ) : jobs.length === 0 ? (
        <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">No jobs have been recorded for this customer.</p>
      ) : (
        <ul className="mt-6 divide-y divide-slate-100">
          {jobs.map((job) => (
            <li key={job.id} className="flex flex-col gap-3 py-4 first:pt-2 last:pb-2 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <Link
                  href={`/jobs/${job.id}`}
                  className="font-semibold text-slate-950 hover:text-brand hover:underline"
                >
                  {job.title}
                </Link>
                {job.scheduledDate && (
                  <p className="mt-1 text-sm text-slate-500">
                    Scheduled{" "}
                    {new Date(`${job.scheduledDate}T00:00:00`).toLocaleDateString(undefined, {
                      month: "short",
                      day: "numeric",
                      year: "numeric",
                    })}
                  </p>
                )}
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <JobStatusBadge status={job.status} />
                <JobPriorityBadge priority={job.priority} />
              </div>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={handleSubmit} className="mt-8 border-t border-slate-100 pt-6">
        <h3 className="font-bold text-slate-950">Add job</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor={`job-title-${customerId}`} className="block text-sm font-semibold text-slate-900">
              Title
            </label>
            <input
              id={`job-title-${customerId}`}
              name="title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
              maxLength={200}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <label htmlFor={`job-priority-${customerId}`} className="block text-sm font-semibold text-slate-900">
              Priority
            </label>
            <select
              id={`job-priority-${customerId}`}
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
            <label htmlFor={`job-scheduled-${customerId}`} className="block text-sm font-semibold text-slate-900">
              Scheduled date
            </label>
            <input
              id={`job-scheduled-${customerId}`}
              name="scheduledDate"
              type="date"
              value={scheduledDate}
              onChange={(event) => setScheduledDate(event.target.value)}
              className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <label htmlFor={`job-description-${customerId}`} className="block text-sm font-semibold text-slate-900">
              Description
            </label>
            <input
              id={`job-description-${customerId}`}
              name="description"
              type="text"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={2000}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
        </div>
        {error && jobs.length > 0 && (
          <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>
        )}
        <div className="mt-5 flex gap-3">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-wait disabled:opacity-60"
          >
            {saving ? "Saving…" : "Add job"}
          </button>
          {!loading && error && (
            <button
              type="button"
              onClick={() => void loadJobs()}
              className="px-3 py-2 text-sm font-semibold text-slate-700"
            >
              Reload jobs
            </button>
          )}
        </div>
      </form>
    </section>
  );
}
