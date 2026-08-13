import { useCallback, useEffect, useRef, useState } from 'react';
import {
  createThread,
  deleteThread,
  listThreads,
  renameThread,
  shareThread,
  unshareThread,
  ApiError,
} from '../api/client';
import type { ChatThread } from '../api/types';

const TITLE_MAX_LENGTH = 80;

/** Mirrors ChatThreadServiceImpl.truncateTitle so an optimistic title matches what the backend will save. */
function truncateTitle(question: string): string {
  const trimmed = question.trim();
  return trimmed.length <= TITLE_MAX_LENGTH
    ? trimmed
    : trimmed.slice(0, TITLE_MAX_LENGTH - 1).trimEnd() + '…';
}

export function useThreads(repoId: string | null) {
  const [threads, setThreads] = useState<ChatThread[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadTokenRef = useRef(0);

  // Reset during render (not an effect) when the repo changes: matches
  // React's documented pattern for adjusting state on a prop change, and
  // avoids the extra cascading render an effect-based reset would cause.
  const [resetForRepoId, setResetForRepoId] = useState(repoId);
  if (resetForRepoId !== repoId) {
    setResetForRepoId(repoId);
    setActiveId(null);
    setThreads([]);
    setLoading(repoId != null);
  }

  // Reloads whenever the repo changes, and auto-picks the most recently
  // active thread — same "pick up where you left off" idea as the messages
  // inside each thread.
  useEffect(() => {
    const token = ++loadTokenRef.current;
    if (!repoId) return;

    listThreads(repoId)
      .then((data) => {
        if (loadTokenRef.current !== token) return; // a newer repo switch already happened
        setThreads(data);
        setActiveId(data[0]?.id ?? null);
        setError(null);
      })
      .catch((err) => {
        if (loadTokenRef.current !== token) return;
        setError(err instanceof ApiError ? err.message : 'Could not load conversations');
      })
      .finally(() => {
        if (loadTokenRef.current === token) setLoading(false);
      });
  }, [repoId]);

  /** repoIds defaults to just the current repo — pass more for a cross-repo conversation. */
  const create = useCallback(
    async (repoIds?: string[]) => {
      const ids = repoIds && repoIds.length > 0 ? repoIds : repoId ? [repoId] : null;
      if (!ids) return null;
      try {
        const thread = await createThread(ids);
        setThreads((prev) => [thread, ...prev]);
        setActiveId(thread.id);
        return thread;
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Could not start a new conversation');
        return null;
      }
    },
    [repoId],
  );

  const rename = useCallback(async (id: string, title: string) => {
    try {
      const updated = await renameThread(id, title);
      setThreads((prev) => prev.map((t) => (t.id === id ? updated : t)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not rename conversation');
    }
  }, []);

  const remove = useCallback(async (id: string) => {
    try {
      await deleteThread(id);
      setThreads((prev) => prev.filter((t) => t.id !== id));
      setActiveId((current) => (current === id ? null : current));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete conversation');
    }
  }, []);

  const share = useCallback(async (id: string) => {
    try {
      const updated = await shareThread(id);
      setThreads((prev) => prev.map((t) => (t.id === id ? updated : t)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create share link');
    }
  }, []);

  const unshare = useCallback(async (id: string) => {
    try {
      const updated = await unshareThread(id);
      setThreads((prev) => prev.map((t) => (t.id === id ? updated : t)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not stop sharing');
    }
  }, []);

  /**
   * Called the moment a message is sent (not when the answer finishes) —
   * bumps the thread to the top of the list and gives it a title if this
   * was its first message, same as any chat app's sidebar.
   */
  const touch = useCallback((id: string, question: string) => {
    setThreads((prev) => {
      const existing = prev.find((t) => t.id === id);
      if (!existing) return prev;
      const updated: ChatThread = {
        ...existing,
        title: existing.title ?? truncateTitle(question),
        updatedAt: new Date().toISOString(),
      };
      return [updated, ...prev.filter((t) => t.id !== id)];
    });
  }, []);

  return { threads, activeId, setActiveId, loading, error, create, rename, remove, share, unshare, touch };
}
