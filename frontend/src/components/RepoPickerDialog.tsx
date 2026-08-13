import { useState } from 'react';
import type { Repo } from '../api/types';
import { Modal } from './Modal';

interface Props {
  open: boolean;
  repos: Repo[];
  defaultRepoId: string;
  onClose: () => void;
  onConfirm: (repoIds: string[]) => void;
}

export function RepoPickerDialog({ open, repos, defaultRepoId, onClose, onConfirm }: Props) {
  const [selected, setSelected] = useState<Set<string>>(new Set([defaultRepoId]));

  // Reset the selection each time the dialog is (re)opened for a different
  // default repo — during render, not an effect, so it's ready before paint.
  const resetKey = `${open}:${defaultRepoId}`;
  const [lastResetKey, setLastResetKey] = useState(resetKey);
  if (lastResetKey !== resetKey) {
    setLastResetKey(resetKey);
    setSelected(new Set([defaultRepoId]));
  }

  const readyRepos = repos.filter((r) => r.status === 'READY');

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  return (
    <Modal open={open} onClose={onClose}>
      <h2 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">New conversation</h2>
      <p className="mt-1.5 text-sm text-neutral-600 dark:text-neutral-400">
        Pick one or more repositories to ask about.
      </p>

      <div className="mt-3 max-h-64 space-y-0.5 overflow-y-auto">
        {readyRepos.map((repo) => (
          <label
            key={repo.id}
            className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            <input
              type="checkbox"
              checked={selected.has(repo.id)}
              onChange={() => toggle(repo.id)}
              className="h-3.5 w-3.5 accent-blue-600"
            />
            <span className="truncate text-sm text-neutral-700 dark:text-neutral-300">
              {repo.name ?? repo.remoteUrl}
            </span>
          </label>
        ))}
      </div>

      <div className="mt-4 flex justify-end gap-2">
        <button
          type="button"
          onClick={onClose}
          className="cursor-pointer rounded-md border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:border-neutral-400 dark:border-neutral-700 dark:text-neutral-300 dark:hover:border-neutral-500"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={() => onConfirm([...selected])}
          disabled={selected.size === 0}
          className="cursor-pointer rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-40 dark:bg-blue-500"
        >
          Start conversation
        </button>
      </div>
    </Modal>
  );
}
