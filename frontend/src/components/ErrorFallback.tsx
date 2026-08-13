/**
 * Sentry's <ErrorBoundary> fallback for a render crash anywhere in the
 * tree — everything else (dialogs, threads, the chat stream) has its own
 * inline error handling; this is only the last resort so a crash reaches a
 * "something broke" screen instead of leaving a blank tab.
 */
export function ErrorFallback() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 bg-white px-6 text-center dark:bg-neutral-950">
      <p className="text-sm text-neutral-600 dark:text-neutral-400">Something went wrong.</p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="cursor-pointer rounded-full bg-neutral-900 px-4 py-2 text-xs font-medium text-white dark:bg-neutral-100 dark:text-neutral-900"
      >
        Reload
      </button>
    </div>
  );
}
