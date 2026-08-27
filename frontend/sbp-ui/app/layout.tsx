import type { Metadata } from "next";
import Link from "next/link";
import { branding } from "@/lib/branding";
import "./globals.css";

export const metadata: Metadata = {
  title: `${branding.name} | Customers`,
  description: branding.description,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full">
        <header className="border-b border-slate-200 bg-white">
          <div className="mx-auto flex min-h-18 max-w-6xl items-center justify-between gap-6 px-5 py-3 sm:px-8">
            <Link
              href="/customers"
              className="group flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand"
            >
              <span className="grid size-10 place-items-center rounded-xl bg-brand text-sm font-bold tracking-wide text-white shadow-sm transition-transform group-hover:-translate-y-0.5">
                {branding.mark}
              </span>
              <span>
                <span className="block text-sm font-semibold text-slate-950">
                  {branding.name}
                </span>
                <span className="block text-xs text-slate-500">
                  {branding.tagline}
                </span>
              </span>
            </Link>
            <nav aria-label="Primary navigation" className="flex items-center gap-1">
              <Link
                href="/customers"
                className="rounded-lg bg-brand-soft px-4 py-2 text-sm font-semibold text-brand-strong transition-colors hover:bg-brand-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Customers
              </Link>
              <Link
                href="/jobs"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Jobs
              </Link>
              <Link
                href="/users"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Users
              </Link>
              <Link
                href="/roles"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Roles
              </Link>
              <Link
                href="/groups"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Groups
              </Link>
              <Link
                href="/audit"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Audit
              </Link>
              <Link
                href="/permissions"
                className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
              >
                Permissions
              </Link>
            </nav>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl px-5 py-9 sm:px-8 sm:py-12">
          {children}
        </main>
        <footer className="mx-auto w-full max-w-6xl px-5 pb-8 text-xs text-slate-500 sm:px-8">
          {branding.footer}
        </footer>
      </body>
    </html>
  );
}
