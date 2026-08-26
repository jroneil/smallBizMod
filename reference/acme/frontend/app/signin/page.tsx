"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { setCredentials } from "@/lib/auth";
import { branding } from "@/lib/branding";

export default function SignInPage() {
  const router = useRouter();
  const [username, setUsername] = useState("demo");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [signingIn, setSigningIn] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setSigningIn(true);
    setError(null);

    try {
      const token = btoa(`${username}:${password}`);
      const response = await fetch("/api/customers", {
        headers: {
          Accept: "application/json",
          Authorization: `Basic ${token}`,
        },
      });

      if (response.ok) {
        setCredentials({ username, password });
        router.replace("/customers");
        return;
      }

      setError(
        response.status === 401
          ? "That username or password is not recognized."
          : "Sign in is unavailable right now. Please try again.",
      );
    } catch {
      setError("Sign in is unavailable right now. Please try again.");
    } finally {
      setSigningIn(false);
    }
  }

  return (
    <div className="mx-auto max-w-md pt-6 sm:pt-12">
      <div className="mb-8 text-center">
        <span className="mx-auto mb-4 grid size-12 place-items-center rounded-xl bg-brand text-lg font-bold tracking-wide text-white shadow-sm">
          {branding.mark}
        </span>
        <h1 className="text-2xl font-bold tracking-tight text-slate-950">
          Sign in to {branding.name}
        </h1>
        <p className="mt-2 text-sm text-slate-600">{branding.tagline}</p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
      >
        <div>
          <label htmlFor="username" className="block text-sm font-semibold text-slate-900">
            Username
          </label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
            className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>

        <div className="mt-5">
          <label htmlFor="password" className="block text-sm font-semibold text-slate-900">
            Password
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
            className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
          />
        </div>

        {error && (
          <p className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={signingIn}
          className="mt-6 w-full rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-60"
        >
          {signingIn ? "Signing in…" : "Sign in"}
        </button>

        <p className="mt-5 text-center text-xs leading-5 text-slate-500">
          Demo access uses the credentials configured in the Acme backend
          (<code className="font-mono">ACME_DEMO_USER</code> /{" "}
          <code className="font-mono">ACME_DEMO_PASSWORD</code>). The default is{" "}
          <code className="font-mono">demo</code> /{" "}
          <code className="font-mono">demo-local-development</code>.
        </p>
      </form>

      <p className="mt-6 text-center text-sm">
        <Link href="/customers" className="font-semibold text-brand hover:text-brand-strong">
          Back to the workspace
        </Link>
      </p>
    </div>
  );
}
