"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { customerApi } from "@/lib/api";

export function CustomerCreateForm() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedName = name.trim();

    if (!normalizedName) {
      setError("Enter a customer name.");
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const customer = await customerApi.create(normalizedName);
      router.push(`/customers/${customer.id}`);
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "The customer could not be created.",
      );
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
    >
      <div>
        <label htmlFor="name" className="block text-sm font-semibold text-slate-900">
          Customer name
        </label>
        <p id="name-hint" className="mt-1 text-sm text-slate-500">
          Use the name your team will recognize in daily work.
        </p>
        <input
          id="name"
          name="name"
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          aria-describedby="name-hint"
          aria-invalid={Boolean(error)}
          autoComplete="organization"
          autoFocus
          maxLength={200}
          required
          className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          placeholder="Example: Lakeview Property Services"
        />
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
          {saving ? "Creating…" : "Create customer"}
        </button>
        <Link
          href="/customers"
          className="rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Cancel
        </Link>
      </div>
    </form>
  );
}
