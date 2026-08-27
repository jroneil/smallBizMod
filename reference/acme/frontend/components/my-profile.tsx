"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth-shell";
import { credentialApi, meApi } from "@/lib/acme-api";
import { type User } from "@/lib/api";

export function MyProfile() {
  const { signOut } = useAuth();
  const [user, setUser] = useState<User | null>(null);
  const [email, setEmail] = useState("");
  const [givenName, setGivenName] = useState("");
  const [familyName, setFamilyName] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);
  const [changing, setChanging] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await meApi.get();
      setUser(loaded);
      setEmail(loaded.email);
      setGivenName(loaded.givenName);
      setFamilyName(loaded.familyName);
      setPhone(loaded.phone ?? "");
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Your profile could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    void meApi
      .get()
      .then((result) => {
        if (cancelled) return;
        setUser(result);
        setEmail(result.email);
        setGivenName(result.givenName);
        setFamilyName(result.familyName);
        setPhone(result.phone ?? "");
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Your profile could not be loaded.",
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

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!email.trim() || !givenName.trim() || !familyName.trim()) {
      setNotice("Email, given name, and family name are required.");
      return;
    }

    setSaving(true);
    setNotice(null);
    try {
      const updated = await meApi.update({
        email: email.trim(),
        givenName: givenName.trim(),
        familyName: familyName.trim(),
        phone: phone.trim(),
      });
      setUser(updated);
      setEmail(updated.email);
      setGivenName(updated.givenName);
      setFamilyName(updated.familyName);
      setPhone(updated.phone ?? "");
      setNotice("Profile saved.");
    } catch (reason) {
      setNotice(
        reason instanceof Error ? reason.message : "Your profile could not be updated.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleChangePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (newPassword.length < 8) {
      setPasswordNotice("New password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordNotice("New passwords do not match.");
      return;
    }

    setChanging(true);
    setPasswordNotice(null);
    try {
      await credentialApi.changePassword(currentPassword, newPassword);
      signOut();
    } catch (reason) {
      setPasswordNotice(
        reason instanceof Error ? reason.message : "The password could not be changed.",
      );
      setChanging(false);
    }
  }

  if (loading) {
    return (
      <div className="animate-pulse" aria-live="polite" aria-busy="true">
        <span className="sr-only">Loading your profile</span>
        <div className="mb-8 h-11 w-72 max-w-full rounded bg-slate-200" />
        <div className="h-64 rounded-2xl border border-slate-200 bg-white" />
      </div>
    );
  }

  if (error || !user) {
    return (
      <section className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
        <h1 className="text-xl font-bold text-red-950">Profile unavailable</h1>
        <p className="mt-2 text-sm text-red-800">
          {error ?? "No profile was returned."}
        </p>
        <button
          type="button"
          onClick={() => void load()}
          className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
        >
          Try again
        </button>
      </section>
    );
  }

  return (
    <div className="max-w-2xl space-y-6">
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Your information</h2>
          <p className="mt-1 text-sm text-slate-500">
            Your username is your application identity and is not editable here.
          </p>
        </div>

        <div className="mt-6">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Username</span>
          <p className="mt-2 font-medium text-slate-950">@{user.username}</p>
        </div>

        <form onSubmit={handleSave} className="mt-6 grid gap-5 sm:grid-cols-2">
          <ProfileField id="me-email" label="Email" value={email} onChange={setEmail} type="email" maxLength={320} required />
          <ProfileField id="me-givenName" label="Given name" value={givenName} onChange={setGivenName} maxLength={100} required />
          <ProfileField id="me-familyName" label="Family name" value={familyName} onChange={setFamilyName} maxLength={100} required />
          <ProfileField id="me-phone" label="Phone" value={phone} onChange={setPhone} type="tel" maxLength={40} />

          {notice ? (
            <p className="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700 sm:col-span-2" role="status">
              {notice}
            </p>
          ) : null}

          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save profile"}
            </button>
          </div>
        </form>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="border-b border-slate-100 pb-5">
          <h2 className="text-lg font-bold text-slate-950">Change password</h2>
          <p className="mt-1 text-sm text-slate-500">
            You will be signed out after changing your password.
          </p>
        </div>

        <form onSubmit={handleChangePassword} className="mt-6 grid gap-5">
          <ProfileField id="me-current-password" label="Current password" value={currentPassword} onChange={setCurrentPassword} type="password" maxLength={72} required />
          <ProfileField id="me-new-password" label="New password" value={newPassword} onChange={setNewPassword} type="password" maxLength={72} required />
          <ProfileField id="me-confirm-password" label="Confirm new password" value={confirmPassword} onChange={setConfirmPassword} type="password" maxLength={72} required />

          {passwordNotice ? (
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
              {passwordNotice}
            </p>
          ) : null}

          <div>
            <button
              type="submit"
              disabled={changing}
              className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
            >
              {changing ? "Changing…" : "Change password"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function ProfileField({
  id,
  label,
  value,
  onChange,
  type = "text",
  maxLength,
  required = false,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  maxLength: number;
  required?: boolean;
}) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-semibold text-slate-900">
        {label}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        maxLength={maxLength}
        required={required}
        className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
      />
    </div>
  );
}
