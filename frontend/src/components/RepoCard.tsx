import { useState } from 'react';
import type { Repo, RepoStatus } from '../api/types';
import { ConfirmDialog } from './ConfirmDialog';

const STATUS_LABEL: Record<RepoStatus, string> = {
  PENDING: 'Queued',
  CLONING: 'Cloning repository',
  CHUNKING: 'Splitting files',
  EMBEDDING: 'Generating embeddings',
  READY: 'Ready',
  FAILED: 'Failed',
};

const STATUS_TONE: Record<RepoStatus, string> = {
  PENDING: 'bg-neutral-400',
  CLONING: 'bg-amber-500',
  CHUNKING: 'bg-amber-500',
  EMBEDDING: 'bg-amber-500',
  READY: 'bg-emerald-500',
  FAILED: 'bg-red-500',
};

interface Props {
  repo: Repo;
  selected: boolean;
  onSelect: (repo: Repo) => void;
  onRetry: (id: string) => void;
  onSync: (id: string) => void;
  onDelete: (id: string) => void;
}

export function RepoCard({ repo, selected, onSelect, onRetry, onSync, onDelete }: Props) {
  const ready = repo.status === 'READY';
  const inProgress = !ready && repo.status !== 'FAILED';
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  return (
    <div
      className={[
        'group relative w-full rounded-lg border px-3 py-2.5 transition',
        selected
          ? 'border-blue-500 bg-blue-50 dark:border-blue-400 dark:bg-blue-950/40'
          : 'border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900',
      ].join(' ')}
    >
      <div className="absolute right-2 top-2 hidden items-center gap-1.5 group-hover:flex">
        {ready && (
          <button
            type="button"
            aria-label={`Sync ${repo.name ?? repo.remoteUrl}`}
            title="Re-check for changes"
            onClick={(e) => {
              e.stopPropagation();
              onSync(repo.id);
            }}
            className="cursor-pointer text-neutral-400 hover:text-neutral-700 dark:text-neutral-500 dark:hover:text-neutral-300"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-3.5 w-3.5">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M21 12a9 9 0 1 1-2.64-6.36M21 4v5h-5"
              />
            </svg>
          </button>
        )}
        <button
          type="button"
          aria-label={`Delete ${repo.name ?? repo.remoteUrl}`}
          onClick={(e) => {
            e.stopPropagation();
            setConfirmingDelete(true);
          }}
          className="cursor-pointer text-xs text-neutral-400 hover:text-red-600 dark:text-neutral-500 dark:hover:text-red-400"
        >
          ×
        </button>
      </div>

      <ConfirmDialog
        open={confirmingDelete}
        title="Delete repository?"
        message={`Delete ${repo.name}? This removes all indexed data.`}
        confirmLabel="Delete"
        danger
        onConfirm={() => {
          setConfirmingDelete(false);
          onDelete(repo.id);
        }}
        onCancel={() => setConfirmingDelete(false)}
      />

      <button
        type="button"
        onClick={() => onSelect(repo)}
        disabled={!ready}
        className={[
          'block w-full text-left',
          ready ? 'hover:border-neutral-300 cursor-pointer' : 'cursor-default',
        ].join(' ')}
      >
        <div className="truncate pr-8 text-sm font-medium text-neutral-900 dark:text-neutral-100">
          {repo.name ?? repo.remoteUrl}
        </div>

        <div className="mt-1 flex items-center gap-1.5">
          <span
            className={[
              'h-1.5 w-1.5 shrink-0 rounded-full',
              STATUS_TONE[repo.status],
              inProgress ? 'animate-pulse' : '',
            ].join(' ')}
          />
          <span className="truncate text-xs text-neutral-500 dark:text-neutral-400">
            {ready
              ? `${repo.documentCount} files · ${repo.chunkCount} chunks`
              : STATUS_LABEL[repo.status]}
          </span>
        </div>
      </button>

      {/* A sync failure leaves the repo READY with its last-good index —
          the error is worth surfacing, but there's no "Retry" here since
          the repo is already usable and the hover-revealed sync icon above
          covers trying again. */}
      {ready && repo.errorMessage && (
        <p className="mt-1.5 line-clamp-2 text-xs text-amber-600 dark:text-amber-400">
          Last sync failed: {repo.errorMessage}
        </p>
      )}

      {repo.status === 'FAILED' && (
        <div className="mt-1.5">
          {repo.errorMessage && (
            <p className="line-clamp-2 text-xs text-red-600 dark:text-red-400">
              {repo.errorMessage}
            </p>
          )}
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onRetry(repo.id);
            }}
            className="mt-1.5 rounded border border-neutral-300 px-2 py-0.5 text-xs text-neutral-600 hover:border-neutral-400 hover:text-neutral-900 dark:border-neutral-700 dark:text-neutral-400 dark:hover:border-neutral-500 dark:hover:text-neutral-100"
          >
            Retry
          </button>
        </div>
      )}
    </div>
  );
}
