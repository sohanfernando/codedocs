import { useCallback, useEffect, useRef, useState } from 'react';
import { listRepos, submitRepo, ApiError, retryRepo, syncRepo, deleteRepo } from '../api/client';
import { isTerminal, type Repo } from '../api/types';

const POLL_INTERVAL_MS = 2000;

export function useRepos() {
  const [repos, setRepos] = useState<Repo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Survives re-renders without retriggering the polling effect.
  const cancelled = useRef(false);

  const refresh = useCallback(async () => {
    try {
      const data = await listRepos();
      if (!cancelled.current) {
        setRepos(data);
        setError(null);
      }
    } catch (err) {
      if (!cancelled.current) {
        setError(err instanceof ApiError ? err.message : 'Could not load repositories');
      }
    } finally {
      if (!cancelled.current) setLoading(false);
    }
  }, []);

  // Poll only while something is mid-ingestion. Once every repo is READY or
  // FAILED there is nothing to watch, so the interval is torn down.
  //
  // The fetch is inlined here (rather than calling the `refresh` callback
  // above) so the effect awaits before touching state itself, instead of
  // synchronously handing off to a function the linter can't verify is safe.
  useEffect(() => {
    cancelled.current = false;

    async function poll() {
      try {
        const data = await listRepos();
        if (!cancelled.current) {
          setRepos(data);
          setError(null);
        }
      } catch (err) {
        if (!cancelled.current) {
          setError(err instanceof ApiError ? err.message : 'Could not load repositories');
        }
      } finally {
        if (!cancelled.current) setLoading(false);
      }
    }

    poll();

    const anyInProgress = repos.some((repo) => !isTerminal(repo.status));
    if (!anyInProgress) return;

    const timer = setInterval(poll, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [repos.map((r) => r.status).join(',')]);

  useEffect(() => {
    return () => {
      cancelled.current = true;
    };
  }, []);

  const submit = useCallback(
    async (gitUrl: string) => {
      setSubmitting(true);
      setError(null);
      try {
        const created = await submitRepo(gitUrl);
        setRepos((prev) => [created, ...prev]);
        return created;
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Could not submit repository');
        return null;
      } finally {
        setSubmitting(false);
      }
    },
    [],
  );

  const retry = useCallback(async (id: string) => {
    try {
      const updated = await retryRepo(id);
      setRepos((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not retry');
    }
  }, []);

  const sync = useCallback(async (id: string) => {
    try {
      const updated = await syncRepo(id);
      setRepos((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not sync');
    }
  }, []);

  const remove = useCallback(async (id: string) => {
    try {
      await deleteRepo(id);
      setRepos((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete repository');
    }
  }, []);

  return { repos, loading, error, submitting, submit, refresh, retry, sync, remove };
}