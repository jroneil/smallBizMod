"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import { userApi, type BulkUserImportResult } from "@/lib/api";

export function BulkImportForm() {
  const [json, setJson] = useState("");
  const [result, setResult] = useState<BulkUserImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);

    let rows: unknown;
    try {
      rows = JSON.parse(json);
    } catch {
      setError("The import data is not valid JSON.");
      return;
    }

    if (!Array.isArray(rows)) {
      setError("The import data must be a JSON array of user objects.");
      return;
    }

    setImporting(true);
    try {
      setResult(await userApi.bulkImport(rows as never[]));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The import failed.");
    } finally {
      setImporting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="max-w-3xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
    >
      <div>
        <label htmlFor="bulk-json" className="block text-sm font-semibold text-slate-900">
          Import data (JSON array)
        </label>
        <p id="bulk-json-hint" className="mt-1 text-sm text-slate-500">
          Each row must include username, email, givenName, and familyName. Invalid rows are reported individually.
        </p>
        <textarea
          id="bulk-json"
          name="json"
          value={json}
          onChange={(event) => setJson(event.target.value)}
          aria-describedby="bulk-json-hint"
          rows={10}
          spellCheck={false}
          className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 font-mono text-sm text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          placeholder='[{"username":"jane.doe","email":"jane@example.com","givenName":"Jane","familyName":"Doe","phone":"555-0100"}]'
        />
      </div>

      {error ? (
        <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          {error}
        </p>
      ) : null}

      {result ? (
        <div className="mt-4 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
          <p className="font-semibold">
            {result.created.length} user{result.created.length === 1 ? "" : "s"} created,{" "}
            {result.errors.length} error{result.errors.length === 1 ? "" : "s"}.
          </p>
          {result.errors.length > 0 ? (
            <ul className="mt-2 list-inside list-disc space-y-1">
              {result.errors.map((entry) => (
                <li key={entry.index}>
                  Row {entry.index + 1}: {entry.message}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}

      <div className="mt-7 flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={importing}
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
        >
          {importing ? "Importing…" : "Import users"}
        </button>
        <Link
          href="/users"
          className="rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Cancel
        </Link>
      </div>
    </form>
  );
}
