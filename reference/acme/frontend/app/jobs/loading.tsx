export default function JobsLoading() {
  return (
    <div className="animate-pulse" aria-live="polite" aria-busy="true">
      <span className="sr-only">Loading work order workspace</span>
      <div className="mb-3 h-4 w-36 rounded bg-slate-200" />
      <div className="mb-8 h-10 w-64 rounded bg-slate-200" />
      <div className="h-72 rounded-2xl border border-slate-200 bg-white" />
    </div>
  );
}
