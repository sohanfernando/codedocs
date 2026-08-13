import { useEffect, useRef, useState } from 'react';
import type { Feedback } from '../api/types';
import type { Message } from '../hooks/useChat';
import { MessageBubble } from './MessageBubble';

interface Props {
  /** One name for a normal thread, several for a cross-repo one. */
  repoNames: string[];
  threadTitle: string | null;
  shared: boolean;
  messages: Message[];
  pending: boolean;
  /** True while the persisted history for the active conversation is being fetched. */
  historyLoading: boolean;
  onSend: (question: string) => void;
  onCitationClick: (index: number) => void;
  onFeedback: (messageId: string, vote: Feedback | null) => void;
  onOpenShare: () => void;
  onExport: () => void;
}

const SUGGESTIONS = [
  'What does this project do?',
  'How is authentication handled?',
  'What is the overall project structure?',
  'What are the main API endpoints?',
];

export function ChatPanel({
  repoNames, threadTitle, shared, messages, pending, historyLoading,
  onSend, onCitationClick, onFeedback, onOpenShare, onExport,
}: Props) {
  const [draft, setDraft] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const hasMessages = messages.length > 0;
  const repoLabel = repoNames.join(', ');

  // Total content length, not just message count, so the view keeps
  // following along as a streaming answer grows token by token.
  const scrollKey = messages.reduce((sum, m) => sum + m.content.length, messages.length);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [scrollKey]);

  function handleSend() {
    const trimmed = draft.trim();
    if (!trimmed || pending) return;
    onSend(trimmed);
    setDraft('');
  }

  return (
    <div className="flex h-full min-w-0 flex-1 flex-col">
      <div className="flex h-11 shrink-0 items-center gap-2 border-b border-neutral-200 px-4 dark:border-neutral-800">
        <span className="truncate text-xs font-medium text-neutral-600 dark:text-neutral-400">
          {threadTitle ?? 'New conversation'}
        </span>
        {shared && (
          <span className="shrink-0 rounded-full bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600 dark:bg-blue-950/40 dark:text-blue-400">
            Shared
          </span>
        )}
        <div className="ml-auto flex shrink-0 items-center gap-1">
          <button
            type="button"
            onClick={onExport}
            disabled={!hasMessages}
            className="cursor-pointer rounded-md px-2 py-1 text-xs font-medium text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 disabled:cursor-default disabled:opacity-40 disabled:hover:bg-transparent dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
          >
            Export
          </button>
          <button
            type="button"
            onClick={onOpenShare}
            disabled={!hasMessages}
            className="cursor-pointer rounded-md px-2 py-1 text-xs font-medium text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 disabled:cursor-default disabled:opacity-40 disabled:hover:bg-transparent dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
          >
            Share
          </button>
        </div>
      </div>

      <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">
        {messages.length === 0 && historyLoading && (
          <div className="flex h-full items-center justify-center">
            <p className="text-sm text-neutral-400 dark:text-neutral-500">Loading conversation…</p>
          </div>
        )}

        {messages.length === 0 && !historyLoading && (
          <div className="flex h-full flex-col items-center justify-center gap-3">
            <p className="text-sm text-neutral-500 dark:text-neutral-400">
              Ask anything about {repoLabel}
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map((question) => (
                <button
                  key={question}
                  type="button"
                  onClick={() => onSend(question)}
                  className="rounded-full border border-neutral-200 px-3 py-1.5 text-xs text-neutral-600 hover:border-neutral-400 hover:text-neutral-900 dark:border-neutral-800 dark:text-neutral-400 dark:hover:border-neutral-600 dark:hover:text-neutral-100"
                >
                  {question}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            onCitationClick={onCitationClick}
            onFeedback={onFeedback}
          />
        ))}

        <div ref={bottomRef} />
      </div>

      <div className="border-t border-neutral-200 p-4 dark:border-neutral-800">
        <div className="flex items-end gap-2 rounded-2xl border border-neutral-300 bg-white px-4 py-3 focus-within:border-blue-500 dark:border-neutral-700 dark:bg-neutral-900 dark:focus-within:border-blue-400">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            rows={1}
            placeholder={`Ask about ${repoLabel}…`}
            className="max-h-32 min-h-6 flex-1 resize-none bg-transparent py-0.5 text-sm leading-relaxed outline-none placeholder:text-neutral-400 dark:text-neutral-100 dark:placeholder:text-neutral-500"
          />
          <button
            type="button"
            onClick={handleSend}
            disabled={pending || !draft.trim()}
            className="cursor-pointer shrink-0 rounded-full bg-neutral-900 px-4 py-2 text-xs font-medium text-white disabled:opacity-30 dark:bg-neutral-100 dark:text-neutral-900"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}