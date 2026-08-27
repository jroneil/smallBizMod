"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  customerApi,
  jobApi,
  type Customer,
  type Job,
  type JobPriority,
  type JobStatus,
} from "@/lib/api";

const PAGE_SIZE = 20;

export function JobList() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<JobStatus | "">("");
  const [priority, setPriority] = useState<JobPriority | "">("");
  const [customerId, setCustomerId] = useState("");
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadJobs = useCallback(
    async (
      nextPage: number,
      q: string,
      s: JobStatus | "",
      p: JobPriority | "",
      c: string,
    ) => {
      setLoading(true);
      setError(null);
      try {
        const result = await jobApi.list({
          query: q.trim() || undefined,
          status: s || undefined,
          priority: p || undefined,
          customerId: c || undefined,
          page: nextPage,
          size: PAGE_SIZE,
        });
        setJobs(result.items);
        setPage(result.page);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : "Work orders could not be loaded.");
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    let cancelled = false;

    void jobApi
      .list({ page: 0, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return;
        setJobs(result.items);
        setPage(result.page);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Work orders could not be loaded.",
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
        // Customer filter options are optional; search still works without them.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void loadJobs(0, query, status, priority, customerId);
  }

  function applyFilter(next: {
    status?: JobStatus | "";
    priority?: JobPriority | "";
    customerId?: string;
  }) {
    const nextStatus = next.status ?? status;
    const nextPriority = next.priority ?? priority;
    const nextCustomerId = next.customerId ?? customerId;
    setStatus(nextStatus);
    setPriority(nextPriority);
    setCustomerId(nextCustomerId);
    void loadJobs(0, query, nextStatus, nextPriority, nextCustomerId);
  }

  const customerNames = new Map(customers.map((customer) => [customer.id, customer.name]));
  const hasFilters = Boolean(query || status || priority || customerId);
  const start = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const end = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <div>
      <form
        onSubmit={handleSearch}
        className="mb-5 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-2 lg:grid-cols-5"
      >
        <div className="lg:col-span-2">
          <label htmlFor="job-query" className="block text-xs font-bold uppercase tracking-wider text-slate-500">
            Search
          </label>
          <input
            id="job-query"
            name="query"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Title or description"
            className="mt-2 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted"
          />
        </div>
        <FilterSelect
          id="job-customer"
          label="Customer"
          value={customerId}
          onChange={(value) => applyFilter({ customerId: value })}
        >
          <option value="">All customers</option>
          {customers.map((customer) => (
            <option key={customer.id} value={customer.id}>
              {customer.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          id="job-status"
          label="Status"
          value={status}
          onChange={(value) => applyFilter({ status: value as JobStatus | "" })}
        >
          <option value="">All statuses</option>
          <option value="OPEN">Open</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </FilterSelect>
        <FilterSelect
          id="job-priority"
          label="Priority"
          value={priority}
          onChange={(value) => applyFilter({ priority: value as JobPriority | "" })}
        >
          <option value="">All priorities</option>
          <option value="LOW">Low</option>
          <option value="NORMAL">Normal</option>
          <option value="HIGH">High</option>
        </FilterSelect>
        <div className="flex items-end gap-3 lg:col-span-5">
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60 sm:px-6"
          >
            {loading ? "Searching…" : "Search"}
          </button>
        </div>
      </form>

      {loading ? (
        <div className="grid gap-3" aria-live="polite" aria-busy="true">
          <span className="sr-only">Loading work orders</span>
          {[0, 1, 2].map((item) => (
            <div
              key={item}
              className="h-20 animate-pulse rounded-2xl border border-slate-200 bg-white"
            />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
          <h2 className="font-semibold text-red-950">Work orders are unavailable</h2>
          <p className="mt-2 text-sm text-red-800">{error}</p>
          <button
            type="button"
            onClick={() => void loadJobs(page, query, status, priority, customerId)}
            className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
        </div>
      ) : jobs.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
          <div className="mx-auto mb-4 grid size-12 place-items-center rounded-full bg-brand-soft text-xl text-brand-strong">
            +
          </div>
          <h2 className="font-semibold text-slate-950">No work orders found</h2>
          <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
            {hasFilters
              ? "Adjust your search or filters to find the work you are looking for."
              : "Create the first work order to start tracking work for your customers."}
          </p>
          {!hasFilters ? (
            <Link
              href="/jobs/new"
              className="mt-6 inline-flex rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
              Create work order
            </Link>
          ) : null}
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="hidden grid-cols-[1fr_10rem_8rem_7rem_7rem] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
              <span>Work order</span>
              <span>Customer</span>
              <span>Status</span>
              <span>Priority</span>
              <span className="text-right">Action</span>
            </div>
            <ul className="divide-y divide-slate-100">
              {jobs.map((job) => (
                <li
                  key={job.id}
                  className="grid gap-3 px-5 py-5 transition-colors hover:bg-slate-50 sm:grid-cols-[1fr_10rem_8rem_7rem_7rem] sm:items-center sm:gap-6 sm:px-6"
                >
                  <div>
                    <p className="font-semibold text-slate-950">{job.title}</p>
                    <p className="mt-1 truncate text-sm text-slate-500">
                      {formatScheduledDate(job.scheduledDate)}
                    </p>
                  </div>
                  <div className="truncate text-sm text-slate-700">
                    {customerNames.get(job.customerId) ?? "Customer"}
                  </div>
                  <div>
                    <JobStatusBadge status={job.status} />
                  </div>
                  <div>
                    <JobPriorityBadge priority={job.priority} />
                  </div>
                  <Link
                    href={`/jobs/${job.id}`}
                    className="text-sm font-semibold text-brand hover:text-brand-strong hover:underline sm:text-right"
                  >
                    Open <span aria-hidden="true">→</span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-4 flex flex-col items-center justify-between gap-3 sm:flex-row">
            <p className="text-sm text-slate-600">
              Showing{" "}
              <span className="font-semibold text-slate-900">
                {totalElements === 0 ? 0 : `${start}–${end}`}
              </span>{" "}
              of <span className="font-semibold text-slate-900">{totalElements}</span> work orders
            </p>
            <div className="flex items-center gap-3">
              <button
                type="button"
                disabled={loading || page <= 0}
                onClick={() => void loadJobs(page - 1, query, status, priority, customerId)}
                className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-50"
              >
                ← Previous
              </button>
              <span className="text-sm text-slate-600">
                Page <span className="font-semibold text-slate-900">{page + 1}</span> of{" "}
                <span className="font-semibold text-slate-900">
                  {Math.max(totalPages, 1)}
                </span>
              </span>
              <button
                type="button"
                disabled={loading || page + 1 >= totalPages}
                onClick={() => void loadJobs(page + 1, query, status, priority, customerId)}
                className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next →
              </button>
            </div>
          </div>
        </>
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

export function JobStatusBadge({ status }: { status: JobStatus }) {
  const classes: Record<JobStatus, string> = {
    OPEN: "bg-sky-50 text-sky-700 ring-sky-200",
    IN_PROGRESS: "bg-amber-50 text-amber-700 ring-amber-200",
    COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    CANCELLED: "bg-slate-100 text-slate-600 ring-slate-300",
  };

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${classes[status]}`}
    >
      {status.replace("_", " ")}
    </span>
  );
}

export function JobPriorityBadge({ priority }: { priority: JobPriority }) {
  const classes: Record<JobPriority, string> = {
    LOW: "bg-slate-100 text-slate-600 ring-slate-300",
    NORMAL: "bg-sky-50 text-sky-700 ring-sky-200",
    HIGH: "bg-rose-50 text-rose-700 ring-rose-200",
  };

  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${classes[priority]}`}
    >
      {priority}
    </span>
  );
}

function formatScheduledDate(value: string | null): string {
  if (!value) return "No scheduled date";
  return `Scheduled ${new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  })}`;
}
