import { useState } from 'react';
import type { ChatThread } from '../api/types';
import { ConfirmDialog } from './ConfirmDialog';

function relativeTime(iso: string): string {
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

interface RowProps {
  thread: ChatThread;
  active: boolean;
  onSelect: () => void;
  onRename: (title: string) => void;
  onDelete: () => void;
}

function ThreadRow({ thread, active, onSelect, onRename, onDelete }: RowProps) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(thread.title ?? '');
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  function commitRename() {
    setEditing(false);
    const trimmed = draft.trim();
    if (trimmed && trimmed !== thread.title) onRename(trimmed);
  }

  return (
    <div
      className={[
        'group relative w-full rounded-md px-2.5 py-1.5 transition',
        active ? 'bg-blue-50 dark:bg-blue-950/40' : 'hover:bg-neutral-100 dark:hover:bg-neutral-800',
      ].join(' ')}
    >
      <ConfirmDialog
        open={confirmingDelete}
        title="Delete conversation?"
        message={`Delete "${thread.title ?? 'New conversation'}"? This can't be undone.`}
        confirmLabel="Delete"
        danger
        onConfirm={() => {
          setConfirmingDelete(false);
          onDelete();
        }}
        onCancel={() => setConfirmingDelete(false)}
      />

      {editing ? (
        <input
          autoFocus
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={commitRename}
          onKeyDown={(e) => {
            if (e.key === 'Enter') commitRename();
            if (e.key === 'Escape') {
              setDraft(thread.title ?? '');
              setEditing(false);
            }
          }}
          className="w-full rounded border border-blue-400 bg-white px-1 py-0.5 text-xs outline-none dark:border-blue-500 dark:bg-neutral-900 dark:text-neutral-100"
        />
      ) : (
        <button type="button" onClick={onSelect} className="block w-full cursor-pointer text-left">
          <div className="truncate pr-10 text-xs text-neutral-700 dark:text-neutral-300">
            {thread.title ?? 'New conversation'}
          </div>
          <div className="mt-0.5 text-[10px] text-neutral-400 dark:text-neutral-500">
            {relativeTime(thread.updatedAt)}
          </div>
        </button>
      )}

      {!editing && (
        <div className="absolute right-1.5 top-1.5 hidden gap-0.5 group-hover:flex">
          <button
            type="button"
            aria-label="Rename conversation"
            onClick={(e) => {
              e.stopPropagation();
              setDraft(thread.title ?? '');
              setEditing(true);
            }}
            className="cursor-pointer rounded p-0.5 text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-100"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-3 w-3">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"
              />
            </svg>
          </button>
          <button
            type="button"
            aria-label="Delete conversation"
            onClick={(e) => {
              e.stopPropagation();
              setConfirmingDelete(true);
            }}
            className="cursor-pointer rounded p-0.5 text-xs text-neutral-400 hover:text-red-600 dark:hover:text-red-400"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

interface Props {
  threads: ChatThread[];
  activeId: string | null;
  loading: boolean;
  onSelect: (id: string) => void;
  onCreate: () => void;
  onRename: (id: string, title: string) => void;
  onDelete: (id: string) => void;
}

export function ThreadList({ threads, activeId, loading, onSelect, onCreate, onRename, onDelete }: Props) {
  return (
    <div className="border-t border-neutral-200 bg-neutral-100/60 px-2 py-2 dark:border-neutral-800 dark:bg-neutral-900/60">
      <div className="mb-1.5 flex items-center justify-between px-1">
        <span className="text-[10px] font-medium uppercase tracking-wide text-neutral-400 dark:text-neutral-500">
          Conversations
        </span>
        <button
          type="button"
          onClick={onCreate}
          className="cursor-pointer rounded px-1.5 py-0.5 text-xs font-medium text-blue-600 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-950/40"
        >
          + New
        </button>
      </div>

      {loading && <p className="px-1 text-xs text-neutral-400 dark:text-neutral-500">Loading…</p>}
      {!loading && threads.length === 0 && (
        <p className="px-1 text-xs text-neutral-400 dark:text-neutral-500">No conversations yet.</p>
      )}

      <div className="space-y-0.5">
        {threads.map((thread) => (
          <ThreadRow
            key={thread.id}
            thread={thread}
            active={thread.id === activeId}
            onSelect={() => onSelect(thread.id)}
            onRename={(title) => onRename(thread.id, title)}
            onDelete={() => onDelete(thread.id)}
          />
        ))}
      </div>
    </div>
  );
}
