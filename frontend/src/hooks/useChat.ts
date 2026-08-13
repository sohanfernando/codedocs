import { useCallback, useEffect, useRef, useState } from 'react';
import { getThreadMessages, setMessageFeedback, streamQuestion } from '../api/client';
import type { Feedback, PersistedMessage, SourceRef } from '../api/types';

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: SourceRef[];
  failed?: boolean;
  feedback?: Feedback | null;
  /** True while an assistant message is still receiving tokens. */
  streaming?: boolean;
}

function fromPersisted(message: PersistedMessage): Message {
  return {
    id: message.id,
    role: message.role === 'USER' ? 'user' : 'assistant',
    content: message.content,
    sources: message.sources.length > 0 ? message.sources : undefined,
    failed: message.failed || undefined,
    feedback: message.feedback,
  };
}

export function useChat(threadId: string | null, onThreadActivity?: (threadId: string, question: string) => void) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [pending, setPending] = useState(false);

  // Survives across the request's async lifetime without being captured by
  // a stale closure; also how a thread switch cancels a stream still in flight.
  const abortRef = useRef<AbortController | null>(null);
  const loadTokenRef = useRef(0);

  // Reset during render (not an effect) when the active thread changes:
  // clears stale messages immediately rather than the extra cascading
  // render an effect-based reset would cause (same pattern as useThreads'
  // repo reset). Refs can't be touched here — the abort lives in the
  // effect below instead, via its cleanup.
  const [resetForThreadId, setResetForThreadId] = useState(threadId);
  if (resetForThreadId !== threadId) {
    setResetForThreadId(threadId);
    setPending(false);
    setMessages([]);
    setLoading(threadId != null);
  }

  // Loads the persisted conversation whenever the active thread changes.
  // The cleanup — not the reset above — is what cancels the previous
  // thread's in-flight stream: cleanups run outside render, where refs are
  // safe to touch, and this one covers unmount too.
  useEffect(() => {
    const token = ++loadTokenRef.current;
    if (threadId) {
      getThreadMessages(threadId)
        .then((data) => {
          if (loadTokenRef.current !== token) return; // a newer switch already happened
          setMessages(data.map(fromPersisted));
        })
        .catch(() => {
          if (loadTokenRef.current !== token) return;
          setMessages([]);
        })
        .finally(() => {
          if (loadTokenRef.current === token) setLoading(false);
        });
    }

    return () => {
      abortRef.current?.abort();
      abortRef.current = null;
    };
  }, [threadId]);

  const send = useCallback(
    async (question: string) => {
      if (!threadId || pending) return;

      onThreadActivity?.(threadId, question);

      const assistantId = crypto.randomUUID();
      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: 'user', content: question },
        { id: assistantId, role: 'assistant', content: '', streaming: true },
      ]);
      setPending(true);

      function updateAssistant(updater: (m: Message) => Message) {
        setMessages((prev) => prev.map((m) => (m.id === assistantId ? updater(m) : m)));
      }

      const controller = new AbortController();
      abortRef.current = controller;

      try {
        await streamQuestion(
          threadId,
          question,
          {
            onSources: (sources) => updateAssistant((m) => ({ ...m, sources })),
            onToken: (text) => updateAssistant((m) => ({ ...m, content: m.content + text })),
            // Swaps the client-side placeholder id for the real persisted
            // one — feedback needs the real id to call the API with.
            onDone: (messageId) => updateAssistant((m) => ({ ...m, id: messageId, streaming: false })),
            onError: (message) =>
              updateAssistant((m) => ({
                ...m,
                // Keep any partial answer already on screen rather than
                // wiping it — the error is additional information, not a
                // replacement for tokens the user already saw.
                content: m.content ? `${m.content}\n\n_${message}_` : message,
                failed: true,
                streaming: false,
              })),
          },
          controller.signal,
        );
      } catch {
        if (controller.signal.aborted) return;
        updateAssistant((m) => ({
          ...m,
          content: m.content || 'Something went wrong. Please try again.',
          failed: true,
          streaming: false,
        }));
      } finally {
        if (abortRef.current === controller) {
          abortRef.current = null;
          setPending(false);
        }
      }
    },
    [threadId, pending, onThreadActivity],
  );

  const setFeedback = useCallback(
    async (messageId: string, vote: Feedback | null) => {
      if (!threadId) return;
      // Optimistic: the vote should feel instant, and a failed PUT just
      // means the next reload shows the previous state — not worth blocking on.
      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, feedback: vote } : m)));
      try {
        await setMessageFeedback(threadId, messageId, vote);
      } catch {
        // Best-effort; the thread reload path is the source of truth if this silently failed.
      }
    },
    [threadId],
  );

  return { messages, loading, pending, send, setFeedback };
}
