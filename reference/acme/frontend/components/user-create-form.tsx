"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { userApi } from "@/lib/api";

export function UserCreateForm() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [givenName, setGivenName] = useState("");
  const [familyName, setFamilyName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!username.trim() || !email.trim() || !givenName.trim() || !familyName.trim()) {
      setError("Username, email, given name, and family name are required.");
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const user = await userApi.create({
        username: username.trim(),
        email: email.trim(),
        givenName: givenName.trim(),
        familyName: familyName.trim(),
        phone: phone.trim(),
      });
      router.push(`/users/${user.id}`);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The user could not be created.",
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
        <UserField
          id="username"
          label="Username"
          hint="The application identity used to recognize this user."
          value={username}
          onChange={setUsername}
          autoComplete="username"
          autoFocus
          maxLength={100}
          required
        />
        <UserField
          id="email"
          label="Email"
          value={email}
          onChange={setEmail}
          type="email"
          autoComplete="email"
          maxLength={320}
          required
        />
        <UserField
          id="givenName"
          label="Given name"
          value={givenName}
          onChange={setGivenName}
          autoComplete="given-name"
          maxLength={100}
          required
        />
        <UserField
          id="familyName"
          label="Family name"
          value={familyName}
          onChange={setFamilyName}
          autoComplete="family-name"
          maxLength={100}
          required
        />
        <UserField
          id="phone"
          label="Phone"
          value={phone}
          onChange={setPhone}
          type="tel"
          autoComplete="tel"
          maxLength={40}
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
          {saving ? "Creating…" : "Create user"}
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

function UserField({
  id,
  label,
  hint,
  value,
  onChange,
  type = "text",
  autoComplete,
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
  autoComplete?: string;
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
        autoComplete={autoComplete}
        autoFocus={autoFocus}
        maxLength={maxLength}
        required={required}
        className="mt-3 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-base text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
      />
    </div>
  );
}
