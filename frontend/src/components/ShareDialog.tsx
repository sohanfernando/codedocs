import { useState } from 'react';
import { Modal } from './Modal';

interface Props {
  open: boolean;
  onClose: () => void;
  shareToken: string | null;
  onShare: () => Promise<void>;
  onUnshare: () => Promise<void>;
}

export function ShareDialog({ open, onClose, shareToken, onShare, onUnshare }: Props) {
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);

  const url = shareToken ? `${window.location.origin}/shared/${shareToken}` : null;

  async function handleCreate() {
    setBusy(true);
    try {
      await onShare();
    } finally {
      setBusy(false);
    }
  }

  async function handleRevoke() {
    setBusy(true);
    try {
      await onUnshare();
    } finally {
      setBusy(false);
    }
  }

  async function handleCopy() {
    if (!url) return;
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <Modal open={open} onClose={onClose}>
      <h2 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">Share conversation</h2>

      {url ? (
        <>
          <p className="mt-1.5 text-sm text-neutral-600 dark:text-neutral-400">
            Anyone with this link can view this conversation — no sign-in required.
          </p>
          <div className="mt-3 flex gap-1.5">
            <input
              readOnly
              value={url}
              onFocus={(e) => e.currentTarget.select()}
              className="min-w-0 flex-1 rounded-md border border-neutral-300 bg-neutral-50 px-2.5 py-1.5 text-xs text-neutral-700 outline-none dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-300"
            />
            <button
              type="button"
              onClick={handleCopy}
              className="shrink-0 cursor-pointer rounded-md bg-neutral-900 px-3 text-xs font-medium text-white dark:bg-neutral-100 dark:text-neutral-900"
            >
              {copied ? 'Copied' : 'Copy'}
            </button>
          </div>
          <button
            type="button"
            onClick={handleRevoke}
            disabled={busy}
            className="mt-4 cursor-pointer text-xs font-medium text-red-600 hover:text-red-700 disabled:opacity-40 dark:text-red-400 dark:hover:text-red-300"
          >
            {busy ? '…' : 'Stop sharing'}
          </button>
        </>
      ) : (
        <>
          <p className="mt-1.5 text-sm text-neutral-600 dark:text-neutral-400">
            Create a public, read-only link to this conversation. Anyone with the link can view
            it without signing in.
          </p>
          <button
            type="button"
            onClick={handleCreate}
            disabled={busy}
            className="mt-4 cursor-pointer rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-40 dark:bg-blue-500"
          >
            {busy ? '…' : 'Create link'}
          </button>
        </>
      )}
    </Modal>
  );
}
