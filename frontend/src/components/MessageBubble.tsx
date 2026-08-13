import { Fragment } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import type { Message } from '../hooks/useChat';
import type { Feedback } from '../api/types';

interface Props {
  message: Message;
  onCitationClick: (index: number) => void;
  /** Omit entirely for a read-only view (e.g. a public share link) — hides the vote buttons rather than wiring them to nothing. */
  onFeedback?: (messageId: string, vote: Feedback | null) => void;
}

/**
 * Splits on [n] citations, keeping any trailing punctuation attached so the
 * sentence doesn't end with a floating full stop.
 */
function linkCitations(text: string, onClick: (index: number) => void) {
  const parts = text.split(/(\[\d+\])/g);
  return parts.map((part, i) => {
    const match = part.match(/^\[(\d+)\]$/);
    if (!match) return <Fragment key={i}>{part}</Fragment>;
    const index = Number(match[1]);
    return (
      <button
        key={i}
        type="button"
        onClick={() => onClick(index)}
        className="mx-0.5 rounded bg-blue-50 px-1 align-baseline text-[11px] font-medium text-blue-600 hover:bg-blue-100 dark:bg-blue-950/60 dark:text-blue-400 dark:hover:bg-blue-900/60"
      >
        [{index}]
      </button>
    );
  });
}

export function MessageBubble({ message, onCitationClick, onFeedback }: Props) {
  if (message.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[80%] rounded-2xl bg-blue-600 px-3.5 py-2 text-sm text-white dark:bg-blue-500">
          {message.content}
        </div>
      </div>
    );
  }

  // Nothing back yet at all — no sources, no first token. A failure this
  // early carries the whole message as plain text, same as before streaming.
  if (!message.content) {
    if (message.failed) {
      return (
        <div className="rounded-lg border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300">
          {message.content}
        </div>
      );
    }
    if (message.streaming) {
      // A 5–10s wait with no feedback reads as broken.
      return (
        <div className="flex items-center gap-1.5 text-sm text-neutral-400 dark:text-neutral-500">
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-neutral-400 dark:bg-neutral-600" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-neutral-400 [animation-delay:150ms] dark:bg-neutral-600" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-neutral-400 [animation-delay:300ms] dark:bg-neutral-600" />
          <span className="ml-1.5">Searching the codebase…</span>
        </div>
      );
    }
  }

  return (
    <div className="prose-sm max-w-none text-sm leading-relaxed text-neutral-800 dark:text-neutral-300">
      <ReactMarkdown
        rehypePlugins={[rehypeHighlight]}
        components={{
          p: ({ children }) => (
            <p className="mb-3">
              {typeof children === 'string'
                ? linkCitations(children, onCitationClick)
                : Array.isArray(children)
                  ? children.map((child, i) =>
                      typeof child === 'string' ? (
                        <Fragment key={i}>{linkCitations(child, onCitationClick)}</Fragment>
                      ) : (
                        <Fragment key={i}>{child}</Fragment>
                      ),
                    )
                  : children}
            </p>
          ),
          li: ({ children }) => (
            <li className="mb-1 ml-4 list-disc">
              {Array.isArray(children)
                ? children.map((child, i) =>
                    typeof child === 'string' ? (
                      <Fragment key={i}>{linkCitations(child, onCitationClick)}</Fragment>
                    ) : (
                      <Fragment key={i}>{child}</Fragment>
                    ),
                  )
                : children}
            </li>
          ),
          pre: ({ children }) => (
            <pre className="mb-3 overflow-x-auto rounded-lg text-xs last:mb-0">{children}</pre>
          ),
          code: ({ className, children }) => {
            // rehype-highlight prepends its own "hljs" class ahead of the
            // "language-xxx" one (e.g. "hljs language-jsx"), so this can't
            // be a startsWith check — that missed every real code block and
            // fell through to the inline-pill style below, one pill per line.
            const isBlock = className?.includes('language-');
            return isBlock ? (
              <code className={`${className} block p-3`}>{children}</code>
            ) : (
              <code className="rounded bg-neutral-100 px-1 py-0.5 text-xs text-neutral-800 dark:bg-neutral-800 dark:text-neutral-200">
                {children}
              </code>
            );
          },
          strong: ({ children }) => (
            <strong className="font-medium text-neutral-900 dark:text-neutral-100">
              {children}
            </strong>
          ),
        }}
      >
        {message.content}
      </ReactMarkdown>
      {message.streaming && (
        <span className="ml-0.5 inline-block h-3.5 w-1.5 animate-pulse bg-neutral-400 align-text-bottom dark:bg-neutral-600" />
      )}

      {/* Only on a completed, successful answer — a failed message has
          nothing worth rating, one still streaming has nothing final yet,
          and no onFeedback at all means this is a read-only view. */}
      {!message.streaming && !message.failed && onFeedback && (
        <div className="mt-1.5 flex items-center gap-0.5">
          <button
            type="button"
            aria-label="Good answer"
            onClick={() => onFeedback(message.id, message.feedback === 'UP' ? null : 'UP')}
            className={[
              'cursor-pointer rounded p-1',
              message.feedback === 'UP'
                ? 'text-emerald-600 dark:text-emerald-400'
                : 'text-neutral-300 hover:text-neutral-600 dark:text-neutral-600 dark:hover:text-neutral-300',
            ].join(' ')}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-3.5 w-3.5">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M7 11v9H4a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1h3Zm0 0 4.5-7.5a1.5 1.5 0 0 1 2.7 1.2L13 10h5.3a2 2 0 0 1 1.94 2.48l-1.5 6A2 2 0 0 1 16.8 20H10a3 3 0 0 1-3-3v-6Z"
              />
            </svg>
          </button>
          <button
            type="button"
            aria-label="Bad answer"
            onClick={() => onFeedback(message.id, message.feedback === 'DOWN' ? null : 'DOWN')}
            className={[
              'cursor-pointer rounded p-1',
              message.feedback === 'DOWN'
                ? 'text-red-600 dark:text-red-400'
                : 'text-neutral-300 hover:text-neutral-600 dark:text-neutral-600 dark:hover:text-neutral-300',
            ].join(' ')}
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-3.5 w-3.5">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M17 13V4h3a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-3Zm0 0-4.5 7.5a1.5 1.5 0 0 1-2.7-1.2L11 14H5.7a2 2 0 0 1-1.94-2.48l1.5-6A2 2 0 0 1 7.2 4H14a3 3 0 0 1 3 3v6Z"
              />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
}