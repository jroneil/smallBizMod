"use client";

import { useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth-shell";
import { credentialApi } from "@/lib/acme-api";
import { branding } from "@/lib/branding";

export default function ChangePasswordPage() {
  const { signOut } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (newPassword.length < 8) {
      setError("New password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("New passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      await credentialApi.changePassword(currentPassword, newPassword);
      // The flag is cleared server-side; drop the local session and return to sign-in.
      signOut();
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The password could not be changed.",
      );
      setSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-5 py-10 sm:px-8">
      <div className="w-full max-w-md">
        <div className="mb-7 text-center">
          <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-brand text-lg font-bold tracking-wide text-white shadow-sm">
            {branding.mark}
          </span>
          <p className="mt-5 text-sm font-semibold uppercase tracking-[0.18em] text-brand-strong">
            {branding.name}
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
            Choose a new password
          </h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Your temporary password must be replaced before you can continue.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="current-password" className="block text-sm font-semibold text-slate-800">
                Current password
              </label>
              <input
                id="current-password"
                name="currentPassword"
                type="password"
                autoComplete="current-password"
                required
                autoFocus
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                disabled={submitting}
                className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted disabled:bg-slate-100"
              />
            </div>

            <div>
              <label htmlFor="new-password" className="block text-sm font-semibold text-slate-800">
                New password
              </label>
              <div className="relative mt-2">
                <input
                  id="new-password"
                  name="newPassword"
                  type={showNewPassword ? "text" : "password"}
                  autoComplete="new-password"
                  required
                  minLength={8}
                  maxLength={72}
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  disabled={submitting}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 pr-16 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted disabled:bg-slate-100"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword((value) => !value)}
                  className="absolute inset-y-0 right-0 rounded-r-lg px-3 text-xs font-semibold text-slate-600 hover:text-brand"
                >
                  {showNewPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            <div>
              <label htmlFor="confirm-password" className="block text-sm font-semibold text-slate-800">
                Confirm new password
              </label>
              <input
                id="confirm-password"
                name="confirmPassword"
                type={showNewPassword ? "text" : "password"}
                autoComplete="new-password"
                required
                minLength={8}
                maxLength={72}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                disabled={submitting}
                className="mt-2 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-slate-950 outline-none placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand-muted disabled:bg-slate-100"
              />
            </div>

            {error ? (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm leading-5 text-red-800" role="alert">
                {error}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={submitting}
              className="inline-flex w-full items-center justify-center rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-70"
            >
              {submitting ? "Changing password…" : "Change password"}
            </button>
          </form>
        </div>
        <p className="mt-5 text-center text-xs leading-5 text-slate-500">
          You will be signed out after changing your password.
        </p>
      </div>
    </div>
  );
}
