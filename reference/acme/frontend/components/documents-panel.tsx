"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { documentApi, type Document, type DocumentOwnerType } from "@/lib/api";

const PAGE_SIZE = 20;

/**
 * Acme copy of the reusable Documents panel (DOC-FR-11), relabeled with the customer-owned "Files"
 * terminology to demonstrate the copy-and-customize model (Section 4 of the Document supplement).
 * Lists one owner's files and supports upload, download, replace, rename, and delete; composed on
 * the Acme Customer detail and Work Order detail screens. Uses the paginated list contract.
 */
export function DocumentsPanel({
  ownerType,
  ownerId,
}: {
  ownerType: DocumentOwnerType;
  ownerId: string;
}) {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [uploading, setUploading] = useState(false);

  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [replaceId, setReplaceId] = useState<string | null>(null);
  const [deleteArmedId, setDeleteArmedId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const uploadInputRef = useRef<HTMLInputElement>(null);
  const replaceInputRef = useRef<HTMLInputElement>(null);

  const ownerLabel = ownerType === "CUSTOMER" ? "customer" : "work order";

  const loadFirstPage = useCallback(async (q: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await documentApi.list({
        ownerType,
        ownerId,
        query: q.trim() || undefined,
        page: 0,
        size: PAGE_SIZE,
      });
      setDocuments(result.items);
      setPage(result.page);
      setTotalElements(result.totalElements);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "Files could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [ownerType, ownerId]);

  useEffect(() => {
    let cancelled = false;

    void documentApi
      .list({ ownerType, ownerId, page: 0, size: PAGE_SIZE })
      .then((result) => {
        if (!cancelled) {
          setDocuments(result.items);
          setPage(result.page);
          setTotalElements(result.totalElements);
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(
            reason instanceof Error ? reason.message : "Files could not be loaded.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [ownerType, ownerId]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!uploadFile) {
      setError("Choose a file to upload.");
      return;
    }

    setUploading(true);
    setError(null);
    setNotice(null);
    try {
      await documentApi.create({
        file: uploadFile,
        ownerType,
        ownerId,
        displayName: displayName.trim() || undefined,
      });
      setUploadFile(null);
      setDisplayName("");
      if (uploadInputRef.current) {
        uploadInputRef.current.value = "";
      }
      setNotice("File uploaded.");
      void loadFirstPage(query);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The file could not be uploaded.",
      );
    } finally {
      setUploading(false);
    }
  }

  async function handleDownload(doc: Document) {
    setError(null);
    setNotice(null);
    try {
      const blob = await documentApi.download(doc.id);
      const url = URL.createObjectURL(blob);
      const anchor = window.document.createElement("a");
      anchor.href = url;
      anchor.download = doc.displayName;
      window.document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      setNotice(`${doc.displayName} downloaded.`);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The file could not be downloaded.",
      );
    }
  }

  function beginRename(doc: Document) {
    setRenamingId(doc.id);
    setRenameValue(doc.displayName);
    setError(null);
    setNotice(null);
  }

  async function saveRename(doc: Document) {
    if (!renameValue.trim()) {
      setError("Enter a file name.");
      return;
    }
    setBusyId(doc.id);
    setError(null);
    setNotice(null);
    try {
      await documentApi.updateDisplayName(doc.id, renameValue.trim());
      setRenamingId(null);
      setNotice("File renamed.");
      void loadFirstPage(query);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The file could not be renamed.",
      );
    } finally {
      setBusyId(null);
    }
  }

  function beginReplace(doc: Document) {
    setReplaceId(doc.id);
    setError(null);
    setNotice(null);
    if (replaceInputRef.current) {
      replaceInputRef.current.click();
    }
  }

  async function replaceWithFile(file: File) {
    const documentId = replaceId;
    if (!documentId) {
      return;
    }
    setBusyId(documentId);
    setError(null);
    setNotice(null);
    try {
      await documentApi.replaceContent(documentId, file);
      setReplaceId(null);
      setNotice("File replaced.");
      void loadFirstPage(query);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The file could not be replaced.",
      );
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(doc: Document) {
    setBusyId(doc.id);
    setError(null);
    setNotice(null);
    try {
      await documentApi.delete(doc.id);
      setDeleteArmedId(null);
      setNotice("File deleted.");
      void loadFirstPage(query);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "The file could not be deleted.",
      );
    } finally {
      setBusyId(null);
    }
  }

  async function loadMore() {
    setLoadingMore(true);
    setError(null);
    try {
      const result = await documentApi.list({
        ownerType,
        ownerId,
        query: query.trim() || undefined,
        page: page + 1,
        size: PAGE_SIZE,
      });
      setDocuments((current) => [...current, ...result.items]);
      setPage(result.page);
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : "More files could not be loaded.",
      );
    } finally {
      setLoadingMore(false);
    }
  }

  const hasMore = documents.length < totalElements;

  return (
    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-lg font-bold text-slate-950">Files</h2>
        <p className="mt-1 text-sm text-slate-500">
          Files attached to this {ownerLabel}, most recently uploaded first.
        </p>
      </div>

      {notice ? (
        <p className="mt-5 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700" role="status">
          {notice}
        </p>
      ) : null}

      <form
        onSubmit={(event) => {
          event.preventDefault();
          void loadFirstPage(query);
        }}
        className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-center"
      >
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search files by name"
          aria-label="Search files"
          className="w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft sm:max-w-xs"
        />
        <div className="flex shrink-0 gap-2">
          <button
            type="submit"
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
          >
            Search
          </button>
          {query ? (
            <button
              type="button"
              onClick={() => {
                setQuery("");
                void loadFirstPage("");
              }}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              Clear
            </button>
          ) : null}
        </div>
      </form>

      {loading ? (
        <p className="mt-6 text-sm text-slate-500" aria-live="polite">Loading files…</p>
      ) : error && documents.length === 0 ? (
        <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm text-red-800" role="alert">{error}</p>
      ) : documents.length === 0 ? (
        <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
          No files have been uploaded for this {ownerLabel}.
        </p>
      ) : (
        <>
          <ul className="mt-6 divide-y divide-slate-100">
            {documents.map((doc) => (
              <li key={doc.id} className="py-4 first:pt-2 last:pb-2">
                {renamingId === doc.id ? (
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                    <input
                      type="text"
                      value={renameValue}
                      onChange={(event) => setRenameValue(event.target.value)}
                      maxLength={255}
                      aria-label="File name"
                      className="w-full rounded-xl border border-slate-300 px-4 py-2.5 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
                    />
                    <div className="flex shrink-0 gap-2">
                      <button
                        type="button"
                        disabled={busyId === doc.id}
                        onClick={() => void saveRename(doc)}
                        className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-wait disabled:opacity-60"
                      >
                        {busyId === doc.id ? "Saving…" : "Save name"}
                      </button>
                      <button
                        type="button"
                        onClick={() => setRenamingId(null)}
                        className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="min-w-0">
                      <button
                        type="button"
                        onClick={() => void handleDownload(doc)}
                        className="font-semibold text-slate-950 hover:text-brand hover:underline"
                      >
                        {doc.displayName}
                      </button>
                      <p className="mt-1 text-sm text-slate-500">
                        {doc.contentType} · {formatBytes(doc.sizeBytes)} · uploaded{" "}
                        {new Date(doc.createdAt).toLocaleDateString(undefined, {
                          month: "short",
                          day: "numeric",
                          year: "numeric",
                        })}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap items-center gap-2">
                      {deleteArmedId === doc.id ? (
                        <>
                          <button
                            type="button"
                            disabled={busyId === doc.id}
                            onClick={() => void handleDelete(doc)}
                            className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
                          >
                            {busyId === doc.id ? "Deleting…" : "Confirm delete"}
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteArmedId(null)}
                            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                          >
                            Keep
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => void handleDownload(doc)}
                            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                          >
                            Download
                          </button>
                          <button
                            type="button"
                            disabled={busyId === doc.id}
                            onClick={() => beginReplace(doc)}
                            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                          >
                            Replace
                          </button>
                          <button
                            type="button"
                            onClick={() => beginRename(doc)}
                            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                          >
                            Rename
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteArmedId(doc.id)}
                            className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-50"
                          >
                            Delete
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
          {hasMore ? (
            <div className="mt-4">
              <button
                type="button"
                disabled={loadingMore}
                onClick={() => void loadMore()}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
              >
                {loadingMore
                  ? "Loading more…"
                  : `Load more (${documents.length} of ${totalElements})`}
              </button>
            </div>
          ) : null}
        </>
      )}

      <input
        ref={replaceInputRef}
        type="file"
        className="hidden"
        onChange={(event) => {
          const file = event.currentTarget.files?.[0];
          if (file) void replaceWithFile(file);
          event.currentTarget.value = "";
        }}
      />

      <form onSubmit={handleUpload} className="mt-8 border-t border-slate-100 pt-6">
        <h3 className="font-bold text-slate-950">Upload file</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor={`doc-file-${ownerType}-${ownerId}`} className="block text-sm font-semibold text-slate-900">
              File
            </label>
            <input
              ref={uploadInputRef}
              id={`doc-file-${ownerType}-${ownerId}`}
              name="file"
              type="file"
              onChange={(event) => {
                const file = event.currentTarget.files?.[0] ?? null;
                setUploadFile(file);
                if (file && !displayName.trim()) {
                  setDisplayName(file.name);
                }
              }}
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
          <div>
            <label htmlFor={`doc-name-${ownerType}-${ownerId}`} className="block text-sm font-semibold text-slate-900">
              Display name (optional)
            </label>
            <input
              id={`doc-name-${ownerType}-${ownerId}`}
              name="displayName"
              type="text"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={255}
              placeholder="Defaults to the file name"
              className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 shadow-sm outline-none placeholder:text-slate-400 focus:border-brand focus:ring-3 focus:ring-brand-soft"
            />
          </div>
        </div>
        {error && documents.length > 0 && (
          <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>
        )}
        <div className="mt-5 flex gap-3">
          <button
            type="submit"
            disabled={uploading}
            className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-wait disabled:opacity-60"
          >
            {uploading ? "Uploading…" : "Upload file"}
          </button>
          {!loading && error && (
            <button
              type="button"
              onClick={() => void loadFirstPage(query)}
              className="px-3 py-2 text-sm font-semibold text-slate-700"
            >
              Reload files
            </button>
          )}
        </div>
      </form>
    </section>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
