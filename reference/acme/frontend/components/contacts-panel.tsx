"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { contactApi, type Contact } from "@/lib/api";

export function ContactsPanel({ customerId }: { customerId: string }) {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [roleTitle, setRoleTitle] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadContacts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setContacts(await contactApi.listByCustomer(customerId));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Contacts could not be loaded.");
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
          setError(reason instanceof Error ? reason.message : "Contacts could not be loaded.");
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
    if (!fullName.trim()) {
      setError("Enter a contact name.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await contactApi.create({ customerId, fullName: fullName.trim(), email: email.trim(), phone: phone.trim(), roleTitle: roleTitle.trim() });
      setContacts((current) => [...current, created].sort((a, b) => a.fullName.localeCompare(b.fullName)));
      setFullName(""); setEmail(""); setPhone(""); setRoleTitle("");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "The contact could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-lg font-bold text-slate-950">Contacts</h2>
        <p className="mt-1 text-sm text-slate-500">People associated with this customer.</p>
      </div>
      {loading ? <p className="mt-6 text-sm text-slate-500" aria-live="polite">Loading contacts…</p> : contacts.length === 0 ? (
        <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">No contacts have been added.</p>
      ) : (
        <ul className="mt-6 grid gap-3 sm:grid-cols-2">
          {contacts.map((contact) => <li key={contact.id} className="rounded-xl border border-slate-200 p-4">
            <div className="flex items-start justify-between gap-3"><div><p className="font-semibold text-slate-950">{contact.fullName}</p>{contact.roleTitle && <p className="mt-1 text-sm text-slate-500">{contact.roleTitle}</p>}</div><span className="rounded-full bg-emerald-50 px-2 py-1 text-xs font-bold text-emerald-700">{contact.status}</span></div>
            {contact.email && <p className="mt-3 break-all text-sm text-slate-700">{contact.email}</p>}
            {contact.phone && <p className="mt-1 text-sm text-slate-700">{contact.phone}</p>}
          </li>)}
        </ul>
      )}
      <form onSubmit={handleSubmit} className="mt-8 border-t border-slate-100 pt-6">
        <h3 className="font-bold text-slate-950">Add contact</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <ContactField id="contactFullName" label="Full name" value={fullName} onChange={setFullName} required maxLength={200} />
          <ContactField id="contactRoleTitle" label="Role or title" value={roleTitle} onChange={setRoleTitle} maxLength={100} />
          <ContactField id="contactEmail" label="Email" value={email} onChange={setEmail} type="email" maxLength={200} />
          <ContactField id="contactPhone" label="Phone" value={phone} onChange={setPhone} type="tel" maxLength={40} />
        </div>
        {error && <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>}
        <div className="mt-5 flex gap-3"><button type="submit" disabled={saving} className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-wait disabled:opacity-60">{saving ? "Saving…" : "Add contact"}</button>{!loading && error && <button type="button" onClick={() => void loadContacts()} className="px-3 py-2 text-sm font-semibold text-slate-700">Reload contacts</button>}</div>
      </form>
    </section>
  );
}

function ContactField({ id, label, value, onChange, type = "text", required = false, maxLength }: { id: string; label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean; maxLength: number }) {
  return <div><label htmlFor={id} className="block text-sm font-semibold text-slate-900">{label}</label><input id={id} name={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} required={required} maxLength={maxLength} className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft" /></div>;
}
