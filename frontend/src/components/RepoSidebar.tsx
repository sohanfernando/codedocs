import { useState, type ReactNode } from 'react';
import type { Repo } from '../api/types';
import { RepoCard } from './RepoCard';

interface Props {
  repos: Repo[];
  loading: boolean;
  submitting: boolean;
  error: string | null;
  selectedId: string | null;
  /** Below the md breakpoint this renders as an off-canvas drawer instead of a static column. */
  mobileOpen: boolean;
  onCloseMobile: () => void;
  onSelect: (repo: Repo) => void;
  onSubmit: (gitUrl: string) => void;
  onRetry: (id: string) => void;
  onSync: (id: string) => void;
  onDelete: (id: string) => void;
  /** Rendered directly under the matching repo card — kept opaque here so this component stays repo-only. */
  threadsPanel?: { repoId: string; node: ReactNode };
}

export function RepoSidebar({
  repos, loading, submitting, error, selectedId, mobileOpen, onCloseMobile,
  onSelect, onSubmit, onRetry, onSync, onDelete, threadsPanel,
}: Props) {
  const [url, setUrl] = useState('');

  function handleSubmit() {
    const trimmed = url.trim();
    if (!trimmed || submitting) return;
    onSubmit(trimmed);
    setUrl('');
  }

  return (
    <aside
      className={[
        'fixed inset-y-0 left-0 z-40 flex w-72 max-w-[85vw] shrink-0 flex-col',
        'border-r border-neutral-200 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900',
        'transition-transform duration-200 md:static md:z-auto md:translate-x-0',
        mobileOpen ? 'translate-x-0' : '-translate-x-full',
      ].join(' ')}
    >
      <div className="flex items-center justify-between border-b border-neutral-200 p-3 dark:border-neutral-800 md:hidden">
        <span className="text-xs font-medium text-neutral-500 dark:text-neutral-400">
          Repositories
        </span>
        <button
          type="button"
          onClick={onCloseMobile}
          aria-label="Close"
          className="rounded p-1 text-neutral-400 hover:text-neutral-900 dark:text-neutral-500 dark:hover:text-neutral-100"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-4 w-4">
            <path strokeLinecap="round" d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </div>

      <div className="border-b border-neutral-200 p-3 dark:border-neutral-800">
        <div className="flex gap-1.5">
          <input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
            placeholder="github.com/user/repo"
            className="min-w-0 flex-1 rounded-md border border-neutral-300 bg-white px-2.5 py-1.5 text-xs outline-none placeholder:text-neutral-400 focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:focus:border-blue-400"
          />
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting || !url.trim()}
            className="shrink-0 rounded-md bg-blue-600 px-3 text-xs font-medium text-white disabled:opacity-40 dark:bg-blue-500"
          >
            {submitting ? '…' : 'Add'}
          </button>
        </div>
        {error && <p className="mt-2 text-xs text-red-600 dark:text-red-400">{error}</p>}
      </div>

      <div className="flex-1 space-y-1.5 overflow-y-auto p-3">
        {loading && <p className="text-xs text-neutral-400 dark:text-neutral-500">Loading…</p>}
        {!loading && repos.length === 0 && (
          <p className="text-xs text-neutral-400 dark:text-neutral-500">
            Add a public GitHub repository to get started.
          </p>
        )}
        {repos.map((repo) => (
          <div key={repo.id}>
            <RepoCard
              repo={repo}
              selected={repo.id === selectedId}
              onSelect={onSelect}
              onRetry={onRetry}
              onSync={onSync}
              onDelete={onDelete}
            />
            {threadsPanel?.repoId === repo.id && (
              <div className="-mx-3 mt-1.5">{threadsPanel.node}</div>
            )}
          </div>
        ))}
      </div>
    </aside>
  );
}