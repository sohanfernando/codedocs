import { useEffect, useState } from 'react';
import { getSharedThread, ApiError } from '../api/client';
import type { SharedThread } from '../api/types';
import type { Message } from '../hooks/useChat';
import { MessageBubble } from './MessageBubble';

interface Props {
  token: string;
}

function toMessages(shared: SharedThread): Message[] {
  return shared.messages.map((m) => ({
    id: m.id,
    role: m.role === 'USER' ? 'user' : 'assistant',
    content: m.content,
    sources: m.sources.length > 0 ? m.sources : undefined,
    failed: m.failed || undefined,
  }));
}

/**
 * The standalone page a share link opens to — rendered outside the
 * authenticated app entirely (see main.tsx), for a visitor who isn't
 * logged in and never needs to be. Read-only: no input box, no repo
 * sidebar, nothing that assumes a session.
 */
export function SharedThreadView({ token }: Props) {
  const [thread, setThread] = useState<SharedThread | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSharedThread(token)
      .then(setThread)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'This link is invalid or has expired.');
      })
      .finally(() => setLoading(false));
  }, [token]);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center bg-white dark:bg-neutral-950">
        <p className="text-sm text-neutral-400 dark:text-neutral-500">Loading…</p>
      </div>
    );
  }

  if (error || !thread) {
    return (
      <div className="flex h-full items-center justify-center bg-white dark:bg-neutral-950">
        <p className="text-sm text-neutral-400 dark:text-neutral-500">
          {error ?? 'This link is invalid or has expired.'}
        </p>
      </div>
    );
  }

  const messages = toMessages(thread);

  return (
    <div className="flex h-full flex-col bg-white dark:bg-neutral-950">
      <header className="flex h-12 shrink-0 items-center gap-2 border-b border-neutral-200 px-4 dark:border-neutral-800">
        <span className="text-sm font-medium text-neutral-900 dark:text-neutral-100">codedocs</span>
        <span className="rounded-full bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600 dark:bg-blue-950/40 dark:text-blue-400">
          Shared, read-only
        </span>
        {thread.repos.length > 0 && (
          <div className="ml-auto flex min-w-0 items-center gap-2">
            {thread.repos.map((repo) => (
              <a
                key={repo.url}
                href={repo.url}
                target="_blank"
                rel="noreferrer"
                className="truncate text-xs text-neutral-400 hover:text-neutral-700 dark:text-neutral-500 dark:hover:text-neutral-300"
              >
                {repo.name ?? repo.url}
              </a>
            ))}
          </div>
        )}
      </header>

      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div className="mx-auto w-full max-w-3xl space-y-6">
          <h1 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
            {thread.title ?? 'Conversation'}
          </h1>

          {messages.map((message) => (
            <div key={message.id}>
              <MessageBubble message={message} onCitationClick={() => {}} />
              {message.sources && message.sources.length > 0 && (
                <div className="mt-2 space-y-1">
                  {message.sources.map((source) => (
                    <a
                      key={source.index}
                      href={source.githubUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="block truncate font-mono text-[11px] text-blue-600 hover:underline dark:text-blue-400"
                    >
                      [{source.index}] {source.filePath}:{source.startLine}-{source.endLine}
                    </a>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
