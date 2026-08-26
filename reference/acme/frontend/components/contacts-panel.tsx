"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { contactApi, type Contact } from "@/lib/api";

export function ContactsPanel({ customerId }: { customerId: string }) {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadContacts = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      setContacts(await contactApi.listByCustomer(customerId));
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Contacts could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    let cancelled = false;

    void contactApi
      .listByCustomer(customerId)
      .then((result) => {
        if (!cancelled) setContacts(result);
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Contacts could not be loaded.",
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
    const normalizedName = fullName.trim();

    if (!normalizedName) {
      setFormError("Enter a contact name.");
      return;
    }

    setSaving(true);
    setFormError(null);

    try {
      const created = await contactApi.create({
        customerId,
        fullName: normalizedName,
        role: role.trim(),
        email: email.trim(),
        phone: phone.trim(),
      });
      setContacts((current) => [...current, created]);
      setFullName("");
      setRole("");
      setEmail("");
      setPhone("");
    } catch (reason) {
      setFormError(
        reason instanceof Error ? reason.message : "The contact could not be saved.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 px-6 py-5 sm:px-8">
        <h2 className="text-lg font-bold text-slate-950">Contacts</h2>
        <p className="mt-1 text-sm text-slate-500">
          People who represent or work at this customer.
        </p>
      </div>

      <div className="p-6 sm:p-8">
        {loading ? (
          <div className="grid gap-3" aria-live="polite" aria-busy="true">
            <span className="sr-only">Loading contacts</span>
            {[0, 1].map((item) => (
              <div
                key={item}
                className="h-16 animate-pulse rounded-xl border border-slate-200 bg-slate-50"
              />
            ))}
          </div>
        ) : error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-5" role="alert">
            <p className="text-sm font-semibold text-red-950">Contacts are unavailable</p>
            <p className="mt-1 text-sm text-red-800">{error}</p>
            <button
              type="button"
              onClick={() => void loadContacts()}
              className="mt-4 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
            >
              Try again
            </button>
          </div>
        ) : contacts.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-300 px-5 py-8 text-center text-sm text-slate-500">
            No contacts yet. Add the first one below.
          </p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {contacts.map((contact) => (
              <li key={contact.id} className="flex flex-col gap-1 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                <div>
                  <p className="font-semibold text-slate-950">{contact.fullName}</p>
                  {contact.role && (
                    <p className="mt-0.5 text-sm text-slate-600">{contact.role}</p>
                  )}
                </div>
                <div className="text-sm text-slate-600">
                  {contact.email && <p className="break-all">{contact.email}</p>}
                  {contact.phone && <p className="mt-0.5">{contact.phone}</p>}
                </div>
              </li>
            ))}
          </ul>
        )}

        <form
          onSubmit={handleSubmit}
          className="mt-6 rounded-xl border border-slate-200 bg-slate-50 p-5 sm:p-6"
        >
          <h3 className="font-semibold text-slate-900">Add a contact</h3>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="contact-name" className="block text-sm font-semibold text-slate-900">
                Name
              </label>
              <input
                id="contact-name"
                type="text"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                maxLength={200}
                required
                placeholder="Example: Casey Nolan"
                className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
              />
            </div>
            <div>
              <label htmlFor="contact-role" className="block text-sm font-semibold text-slate-900">
                Role
              </label>
              <input
                id="contact-role"
                type="text"
                value={role}
                onChange={(event) => setRole(event.target.value)}
                maxLength={100}
                placeholder="Example: Property Manager"
                className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
              />
            </div>
            <div>
              <label htmlFor="contact-email" className="block text-sm font-semibold text-slate-900">
                Email
              </label>
              <input
                id="contact-email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                maxLength={200}
                placeholder="name@example.com"
                className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
              />
            </div>
            <div>
              <label htmlFor="contact-phone" className="block text-sm font-semibold text-slate-900">
                Phone
              </label>
              <input
                id="contact-phone"
                type="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                maxLength={40}
                placeholder="Example: 555-0100"
                className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
              />
            </div>
          </div>

          {formError && (
            <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
              {formError}
            </p>
          )}

          <div className="mt-5">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {saving ? "Adding…" : "Add contact"}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
