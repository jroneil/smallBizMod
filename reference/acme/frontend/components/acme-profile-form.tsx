"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { acmeCustomerApi } from "@/lib/acme-api";

export function AcmeProfileForm({ customerId }: { customerId: string }) {
  const [serviceRegion, setServiceRegion] = useState("");
  const [accountNotes, setAccountNotes] = useState("");
  const [hasProfile, setHasProfile] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await acmeCustomerApi.get(customerId);
      setHasProfile(result !== null);
      setServiceRegion(result?.profile.serviceRegion ?? "");
      setAccountNotes(result?.profile.accountNotes ?? "");
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Service details could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    let cancelled = false;

    void acmeCustomerApi
      .get(customerId)
      .then((result) => {
        if (cancelled) return;
        setHasProfile(result !== null);
        setServiceRegion(result?.profile.serviceRegion ?? "");
        setAccountNotes(result?.profile.accountNotes ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error
              ? reason.message
              : "Service details could not be loaded.",
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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedRegion = serviceRegion.trim();

    if (!normalizedRegion) {
      setError("Enter a service region.");
      setSaved(false);
      return;
    }

    setSaving(true);
    setError(null);
    setSaved(false);

    try {
      const result = await acmeCustomerApi.saveProfile(customerId, {
        serviceRegion: normalizedRegion,
        accountNotes: accountNotes.trim(),
      });
      setServiceRegion(result.profile.serviceRegion);
      setAccountNotes(result.profile.accountNotes ?? "");
      setHasProfile(true);
      setSaved(true);
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Service details could not be saved.",
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <section
        className="mt-6 h-96 animate-pulse rounded-2xl border border-orange-100 bg-white"
        aria-live="polite"
        aria-busy="true"
      >
        <span className="sr-only">Loading Acme service details</span>
      </section>
    );
  }

  if (error && !hasProfile && !serviceRegion) {
    return (
      <section className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h2 className="font-bold text-red-950">Service details are unavailable</h2>
        <p className="mt-2 text-sm text-red-800">{error}</p>
        <button
          type="button"
          onClick={() => void loadProfile()}
          className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
        >
          Try again
        </button>
      </section>
    );
  }

  return (
    <section className="mt-6 overflow-hidden rounded-2xl border border-orange-200 bg-white shadow-sm">
      <div className="border-b border-orange-100 bg-brand-soft px-6 py-5 sm:px-8">
        <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand">
              Acme service details
            </p>
            <h2 className="mt-1 text-lg font-bold text-slate-950">Service profile</h2>
          </div>
          <span className="w-fit rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-brand-strong ring-1 ring-inset ring-orange-200">
            {hasProfile ? "Profile on file" : "Profile not completed"}
          </span>
        </div>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
          Capture the territory and practical notes Acme needs when serving this customer.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="p-6 sm:p-8">
        <div>
          <label htmlFor="serviceRegion" className="block text-sm font-semibold text-slate-900">
            Service region
          </label>
          <input
            id="serviceRegion"
            name="serviceRegion"
            type="text"
            value={serviceRegion}
            onChange={(event) => {
              setServiceRegion(event.target.value);
              setSaved(false);
            }}
            maxLength={100}
            required
            placeholder="Example: North Metro"
            className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>

        <div className="mt-6">
          <div className="flex items-end justify-between gap-4">
            <label htmlFor="accountNotes" className="block text-sm font-semibold text-slate-900">
              Account notes
            </label>
            <span className="text-xs text-slate-400">{accountNotes.length}/2000</span>
          </div>
          <textarea
            id="accountNotes"
            name="accountNotes"
            value={accountNotes}
            onChange={(event) => {
              setAccountNotes(event.target.value);
              setSaved(false);
            }}
            maxLength={2000}
            rows={5}
            placeholder="Access notes, service preferences, or other useful context"
            className="mt-2 w-full resize-y rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>

        {error && (
          <p className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
            {error}
          </p>
        )}
        {saved && (
          <p className="mt-5 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800" role="status">
            Service profile saved. These details will remain available after reload.
          </p>
        )}

        <div className="mt-6 flex items-center gap-4">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
          >
            {saving ? "Saving…" : hasProfile ? "Update service profile" : "Save service profile"}
          </button>
          {saved && <span className="text-sm font-medium text-emerald-700">Saved</span>}
        </div>
      </form>
    </section>
  );
}
