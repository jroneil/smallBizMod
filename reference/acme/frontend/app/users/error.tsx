"use client";

import { useEffect } from "react";

export default function UsersError({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="rounded-2xl border border-red-200 bg-red-50 p-6" role="alert">
      <h1 className="text-xl font-bold text-red-950">Something went wrong</h1>
      <p className="mt-2 text-sm text-red-800">
        The user administration workspace could not be displayed. Please try again.
      </p>
      <button
        type="button"
        onClick={() => retry()}
        className="mt-5 rounded-lg border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-900 hover:bg-red-100"
      >
        Try again
      </button>
    </div>
  );
}
