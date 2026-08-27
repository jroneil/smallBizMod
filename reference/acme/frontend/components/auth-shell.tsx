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
  signIn: (username: string, password: string) => Promise<void>;
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
    if (checked && !session && pathname !== "/signin") {
      router.replace("/signin");
    }
  }, [checked, pathname, router, session]);

  const contextValue = useMemo<AuthContextValue>(
    () => ({
      checked,
      session,
      signIn: async (username, password) => {
        await authenticate(username, password);
      },
      signOut: () => {
        clearAuthSession();
        router.replace("/signin");
      },
    }),
    [checked, router, session],
  );

  if (pathname === "/signin") {
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
            <nav aria-label="Primary navigation">
              <Link
                href="/customers"
                className="rounded-lg bg-brand-soft px-3 py-2 text-sm font-semibold text-brand-strong transition-colors hover:bg-brand-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand sm:px-4"
              >
                Customers
              </Link>
            </nav>
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
