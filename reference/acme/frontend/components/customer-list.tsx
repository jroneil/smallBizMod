"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { customerApi, type Customer } from "@/lib/api";

export function CustomerList() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      setCustomers(await customerApi.list());
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Customers could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

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
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="grid gap-3" aria-live="polite" aria-busy="true">
        <span className="sr-only">Loading customers</span>
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
        <h2 className="font-semibold text-red-950">Customers are unavailable</h2>
        <p className="mt-2 text-sm text-red-800">{error}</p>
        <button
          type="button"
          onClick={() => void loadCustomers()}
          className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700"
        >
          Try again
        </button>
      </div>
    );
  }

  if (customers.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-14 text-center">
        <div className="mx-auto mb-4 grid size-12 place-items-center rounded-full bg-brand-soft text-xl text-brand-strong">
          +
        </div>
        <h2 className="font-semibold text-slate-950">No customers yet</h2>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-600">
          Create the first customer to begin keeping their business information in one place.
        </p>
        <Link
          href="/customers/new"
          className="mt-6 inline-flex rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          Create customer
        </Link>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="hidden grid-cols-[1fr_9rem_7rem] gap-6 border-b border-slate-200 bg-slate-50 px-6 py-3 text-xs font-bold uppercase tracking-wider text-slate-500 sm:grid">
        <span>Customer</span>
        <span>Status</span>
        <span className="text-right">Action</span>
      </div>
      <ul className="divide-y divide-slate-100">
        {customers.map((customer) => (
          <li
            key={customer.id}
            className="grid gap-3 px-5 py-5 transition-colors hover:bg-slate-50 sm:grid-cols-[1fr_9rem_7rem] sm:items-center sm:gap-6 sm:px-6"
          >
            <div>
              <p className="font-semibold text-slate-950">{customer.name}</p>
              <p className="mt-1 truncate font-mono text-xs text-slate-400">
                {customer.id}
              </p>
            </div>
            <div>
              <span className="inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700 ring-1 ring-inset ring-emerald-200">
                {customer.status}
              </span>
            </div>
            <Link
              href={`/customers/${customer.id}`}
              className="text-sm font-semibold text-brand hover:text-brand-strong hover:underline sm:text-right"
            >
              Open <span aria-hidden="true">→</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
