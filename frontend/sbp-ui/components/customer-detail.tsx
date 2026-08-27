"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { ContactsPanel } from "@/components/contacts-panel";
import { DocumentsPanel } from "@/components/documents-panel";
import { JobsPanel } from "@/components/jobs-panel";
import { customerApi, type Customer } from "@/lib/api";

export function CustomerDetail({ customerId }: { customerId: string }) {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCustomer = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      setCustomer(await customerApi.get(customerId));
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "The customer could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    let cancelled = false;

    void customerApi
      .get(customerId)
      .then((result) => {
        if (!cancelled) setCustomer(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error
              ? reason.message
              : "The customer could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [customerId]);

  if (loading) {
    return <DetailSkeleton />;
  }

  if (error || !customer) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">Customer unavailable</h1>
        <p className="mt-2 text-sm text-red-800">
          {error ?? "No customer was returned."}
        </p>
        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={() => void loadCustomer()}
            className="rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
          >
            Try again
          </button>
          <Link href="/customers" className="px-3 py-2 text-sm font-semibold text-red-900">
            Back to customers
          </Link>
        </div>
      </section>
    );
  }

  return (
    <>
      <Link
        href="/customers"
        className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand"
      >
        <span aria-hidden="true">←</span> Customers
      </Link>
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            Customer record
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {customer.name}
          </h1>
        </div>
        <span className="w-fit rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700 ring-1 ring-inset ring-emerald-200">
          {customer.status}
        </span>
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Customer information</h2>
          <p className="mt-1 text-sm text-slate-500">
            Core details shared across the application.
          </p>
        </div>
        <dl className="mt-6 grid gap-6 sm:grid-cols-2">
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Name</dt>
            <dd className="mt-2 font-medium text-slate-950">{customer.name}</dd>
          </div>
          <div>
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">Status</dt>
            <dd className="mt-2 font-medium text-slate-950">{customer.status}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Customer ID
            </dt>
            <dd className="mt-2 break-all font-mono text-sm text-slate-600">
              {customer.id}
            </dd>
          </div>
        </dl>
      </section>
      <ContactsPanel customerId={customer.id} />
      <JobsPanel customerId={customer.id} />
      <DocumentsPanel ownerType="CUSTOMER" ownerId={customer.id} />
    </>
  );
}

function DetailSkeleton() {
  return (
    <div className="animate-pulse" aria-live="polite" aria-busy="true">
      <span className="sr-only">Loading customer</span>
      <div className="mb-5 h-4 w-28 rounded bg-slate-200" />
      <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
      <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
