"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { customerApi, jobApi, type Customer, type JobPriority } from "@/lib/api";

export function JobCreateForm() {
  const router = useRouter();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customerId, setCustomerId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<JobPriority>("NORMAL");
  const [scheduledDate, setScheduledDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;

    void customerApi
      .list()
      .then((result) => {
        if (!cancelled) setCustomers(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error
              ? reason.message
              : "Customers could not be loaded.",
          );
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!customerId) {
      setError("Choose the customer this work order is for.");
      return;
    }
    if (!title.trim()) {
      setError("Work order title is required.");
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const job = await jobApi.create({
        customerId,
        title: title.trim(),
        description: description.trim() || "",
        priority,
        scheduledDate: scheduledDate || null,
      });
      router.push(`/jobs/${job.id}`);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The work order could not be created.",
      );
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="sm:col-span-2">
          <label htmlFor="customerId" className="block text-sm font-semibold text-slate-900">
            Customer
          </label>
          <p className="mt-1 text-sm text-slate-500">
            The customer this work is performed for.
          </p>
          <select
            id="customerId"
            name="customerId"
            value={customerId}
            onChange={(event) => setCustomerId(event.target.value)}
            required
            className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
          >
            <option value="">Select a customer…</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.name}
              </option>
            ))}
          </select>
        </div>
        <JobField
          id="title"
          label="Title"
          hint="A short name for the work, such as “Replace water heater”."
          value={title}
          onChange={setTitle}
          autoFocus
          maxLength={200}
          required
        />
        <div>
          <label htmlFor="priority" className="block text-sm font-semibold text-slate-900">
            Priority
          </label>
          <p className="mt-1 text-sm text-slate-500">How urgent is this work?</p>
          <select
            id="priority"
            name="priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value as JobPriority)}
            className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
          >
            <option value="LOW">Low</option>
            <option value="NORMAL">Normal</option>
            <option value="HIGH">High</option>
          </select>
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="scheduledDate" className="block text-sm font-semibold text-slate-900">
            Scheduled date
          </label>
          <p className="mt-1 text-sm text-slate-500">
            When the work is currently planned to start. Optional.
          </p>
          <input
            id="scheduledDate"
            name="scheduledDate"
            type="date"
            value={scheduledDate}
            onChange={(event) => setScheduledDate(event.target.value)}
            className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>
        <div className="sm:col-span-2">
          <label htmlFor="description" className="block text-sm font-semibold text-slate-900">
            Description
          </label>
          <p className="mt-1 text-sm text-slate-500">
            What the work involves. Optional.
          </p>
          <textarea
            id="description"
            name="description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={4}
            maxLength={2000}
            className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>
      </div>

      {error && (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      )}

      <div className="mt-7 flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
        >
          {saving ? "Creating…" : "Create work order"}
        </button>
        <Link
          href="/jobs"
          className="rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Cancel
        </Link>
      </div>
    </form>
  );
}

function JobField({
  id,
  label,
  hint,
  value,
  onChange,
  type = "text",
  autoFocus = false,
  maxLength,
  required = false,
}: {
  id: string;
  label: string;
  hint?: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  autoFocus?: boolean;
  maxLength: number;
  required?: boolean;
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold text-slate-900">
        {label}
      </label>
      {hint ? <p className="mt-1 text-sm text-slate-500">{hint}</p> : null}
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoFocus={autoFocus}
        maxLength={maxLength}
        required={required}
        className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
      />
    </div>
  );
}
