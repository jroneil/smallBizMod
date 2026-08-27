"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useSyncExternalStore,
} from "react";
import {
  authenticate,
  clearAuthSession,
  getServerAuthValue,
  getStoredAuthValue,
  parseAuthSession,
  subscribeToAuthChanges,
  type AuthSession,
} from "@/lib/auth";
import { branding } from "@/lib/branding";

interface AuthContextValue {
  checked: boolean;
  session: AuthSession | null;
  signIn: (username: string, password: string) => Promise<AuthSession>;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const subscribeToHydration = () => () => undefined;

export function AuthShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const storedValue = useSyncExternalStore(
    subscribeToAuthChanges,
    getStoredAuthValue,
    getServerAuthValue,
  );
  const checked = useSyncExternalStore(
    subscribeToHydration,
    () => true,
    () => false,
  );
  const session = useMemo(() => parseAuthSession(storedValue), [storedValue]);

  useEffect(() => {
    if (!checked) return;
    if (!session && pathname !== "/signin") {
      router.replace("/signin");
      return;
    }
    if (session?.passwordChangeRequired && pathname !== "/change-password") {
      router.replace("/change-password");
      return;
    }
    if (session && !session.passwordChangeRequired && pathname === "/change-password") {
      router.replace("/customers");
    }
  }, [checked, pathname, router, session]);

  const contextValue = useMemo<AuthContextValue>(
    () => ({
      checked,
      session,
      signIn: async (username, password) => {
        return await authenticate(username, password);
      },
      signOut: () => {
        clearAuthSession();
        router.replace("/signin");
      },
    }),
    [checked, router, session],
  );

  if (pathname === "/signin" || pathname === "/change-password") {
    return (
      <AuthContext.Provider value={contextValue}>
        <main className="min-h-screen">{children}</main>
      </AuthContext.Provider>
    );
  }

  if (!checked || !session) {
    return (
      <AuthContext.Provider value={contextValue}>
        <main className="grid min-h-screen place-items-center px-5" aria-live="polite">
          <div className="text-center">
            <span className="mx-auto grid size-12 place-items-center rounded-xl bg-brand text-base font-bold text-white shadow-sm">
              {branding.mark}
            </span>
            <p className="mt-4 text-sm font-medium text-slate-600">
              Opening your customer workspace…
            </p>
          </div>
        </main>
      </AuthContext.Provider>
    );
  }

  return (
    <AuthContext.Provider value={contextValue}>
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
          <div className="flex items-center gap-2 sm:gap-4">
            <nav aria-label="Primary navigation" className="flex items-center gap-1">
              <Link
                href="/customers"
                className="rounded-lg bg-brand-soft px-3 py-2 text-sm font-semibold text-brand-strong transition-colors hover:bg-brand-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Customers
              </Link>
              <Link
                href="/jobs"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Work Orders
              </Link>
              <Link
                href="/users"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Users
              </Link>
              <Link
                href="/roles"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Roles
              </Link>
              <Link
                href="/groups"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Groups
              </Link>
              <Link
                href="/audit"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Audit
              </Link>
              <Link
                href="/permissions"
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Permissions
              </Link>
            </nav>
            <Link
              href="/me"
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
            >
              My profile
            </Link>
            <button
              type="button"
              onClick={contextValue.signOut}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-5 py-9 sm:px-8 sm:py-12">
        {children}
      </main>
      <footer className="mx-auto w-full max-w-6xl px-5 pb-8 text-xs text-slate-500 sm:px-8">
        {branding.footer}
      </footer>
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthShell");
  return context;
}
