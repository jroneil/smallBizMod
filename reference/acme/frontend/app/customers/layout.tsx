"use client";

import { useRouter } from "next/navigation";
import { useEffect, useSyncExternalStore } from "react";
import { getCredentials, subscribeAuth } from "@/lib/auth";

export default function CustomersLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const hasCredentials = useSyncExternalStore(
    subscribeAuth,
    () => getCredentials() !== null,
    () => true,
  );

  useEffect(() => {
    if (!hasCredentials) {
      router.replace("/signin");
    }
  }, [hasCredentials, router]);

  if (!hasCredentials) {
    return (
      <div className="animate-pulse" aria-live="polite" aria-busy="true">
        <span className="sr-only">Checking access</span>
        <div className="mb-3 h-4 w-36 rounded bg-slate-200" />
        <div className="mb-8 h-10 w-64 rounded bg-slate-200" />
        <div className="h-72 rounded-2xl border border-slate-200 bg-white" />
      </div>
    );
  }

  return <>{children}</>;
}
