"use client";

import { useRouter } from "next/navigation";
import { clearCredentials } from "@/lib/auth";

export function SignOutButton() {
  const router = useRouter();

  function handleSignOut() {
    clearCredentials();
    router.replace("/signin");
  }

  return (
    <button
      type="button"
      onClick={handleSignOut}
      className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
    >
      Sign out
    </button>
  );
}
