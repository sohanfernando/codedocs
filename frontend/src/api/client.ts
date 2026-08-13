import type {
  ApiErrorBody,
  ChatThread,
  Feedback,
  PersistedMessage,
  Repo,
  SharedThread,
  SourceRef,
  User,
} from './types';

const BASE = '/api';

/** Fired whenever a request comes back 401 — the session expired or was never there. */
export const UNAUTHORIZED_EVENT = 'codedocs:unauthorized';

/** Carries the backend's error code so the UI can react to RATE_LIMITED etc. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: string[];

  constructor(status: number, code: string, message: string, details?: string[]) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

/** Reads the XSRF-TOKEN cookie Spring Security's CsrfCookieFilter sets. */
function csrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/** GET/HEAD are exempt from CSRF checks server-side, so only attach the header when it matters. */
function buildHeaders(method: string | undefined, extra?: HeadersInit): HeadersInit {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const verb = (method ?? 'GET').toUpperCase();
  if (verb !== 'GET' && verb !== 'HEAD') {
    const token = csrfToken();
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  return { ...headers, ...extra };
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers: buildHeaders(init?.method, init?.headers),
  });

  if (response.status === 401) {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }

  if (!response.ok) {
    // The backend returns ErrorResponse JSON, but a proxy or crash might not.
    let body: ApiErrorBody | null = null;
    try {
      body = await response.json();
    } catch {
      // fall through to the generic message below
    }
    throw new ApiError(
      response.status,
      body?.code ?? 'UNKNOWN',
      body?.message ?? `Request failed (${response.status})`,
      body?.details,
    );
  }

  // 204 No Content (logout, delete) has no body — .json() would throw on it.
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function register(email: string, password: string): Promise<User> {
  return request<User>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function login(email: string, password: string): Promise<User> {
  return request<User>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function logout(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' });
}

export function getCurrentUser(): Promise<User> {
  return request<User>('/auth/me');
}

export function listRepos(): Promise<Repo[]> {
  return request<Repo[]>('/repositories');
}

export function retryRepo(id: string): Promise<Repo> {
  return request<Repo>(`/repositories/${id}/retry`, { method: 'POST' });
}

/** Incremental refresh — only files that changed get re-embedded. */
export function syncRepo(id: string): Promise<Repo> {
  return request<Repo>(`/repositories/${id}/sync`, { method: 'POST' });
}

export function getRepo(id: string): Promise<Repo> {
  return request<Repo>(`/repositories/${id}`);
}

export function deleteRepo(id: string): Promise<void> {
  return request<void>(`/repositories/${id}`, { method: 'DELETE' });
}

export function submitRepo(gitUrl: string, branch?: string): Promise<Repo> {
  return request<Repo>('/repositories', {
    method: 'POST',
    body: JSON.stringify({ gitUrl, branch: branch || null }),
  });
}

export function listThreads(repoId: string): Promise<ChatThread[]> {
  return request<ChatThread[]>(`/threads?repoId=${encodeURIComponent(repoId)}`);
}

export function createThread(repoIds: string[]): Promise<ChatThread> {
  return request<ChatThread>('/threads', {
    method: 'POST',
    body: JSON.stringify({ repoIds }),
  });
}

export function getThreadMessages(threadId: string): Promise<PersistedMessage[]> {
  return request<PersistedMessage[]>(`/threads/${threadId}/messages`);
}

export function renameThread(threadId: string, title: string): Promise<ChatThread> {
  return request<ChatThread>(`/threads/${threadId}`, {
    method: 'PATCH',
    body: JSON.stringify({ title }),
  });
}

export function deleteThread(threadId: string): Promise<void> {
  return request<void>(`/threads/${threadId}`, { method: 'DELETE' });
}

export function shareThread(threadId: string): Promise<ChatThread> {
  return request<ChatThread>(`/threads/${threadId}/share`, { method: 'POST' });
}

export function unshareThread(threadId: string): Promise<ChatThread> {
  return request<ChatThread>(`/threads/${threadId}/share`, { method: 'DELETE' });
}

/** vote: null clears any existing feedback. */
export function setMessageFeedback(
  threadId: string,
  messageId: string,
  vote: Feedback | null,
): Promise<PersistedMessage> {
  return request<PersistedMessage>(`/threads/${threadId}/messages/${messageId}/feedback`, {
    method: 'PUT',
    body: JSON.stringify({ vote }),
  });
}

/** No auth, no session — this is the one public read path in the API. */
export function getSharedThread(token: string): Promise<SharedThread> {
  return request<SharedThread>(`/shared/${encodeURIComponent(token)}`);
}

export interface ChatStreamHandlers {
  onSources: (sources: SourceRef[]) => void;
  onToken: (text: string) => void;
  /** messageId is the persisted assistant message's real id — needed before feedback can be sent on it. */
  onDone: (messageId: string) => void;
  onError: (message: string, code?: string) => void;
}

/**
 * Reads /api/chat/stream by hand rather than via `EventSource`: EventSource
 * can only issue GET requests, and the question needs to travel in a POST
 * body. `onError` (not a throw) carries stream failures, since by the time
 * one can happen the response has already started and partial content may
 * already be on screen — the caller decides whether to keep it.
 */
export async function streamQuestion(
  threadId: string,
  question: string,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch(`${BASE}/chat/stream`, {
    method: 'POST',
    headers: buildHeaders('POST'),
    body: JSON.stringify({ threadId, question }),
    signal,
  });

  if (response.status === 401) {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }

  if (!response.ok || !response.body) {
    // Same shape as the non-streaming endpoint: a plain JSON ErrorResponse,
    // sent whenever the request never reaches the SSE emitter at all
    // (validation failure, rate limit, repo not found).
    let body: ApiErrorBody | null = null;
    try {
      body = await response.json();
    } catch {
      // fall through to the generic message below
    }
    handlers.onError(body?.message ?? `Request failed (${response.status})`, body?.code);
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let boundary: number;
    // SSE events are separated by a blank line.
    while ((boundary = buffer.indexOf('\n\n')) !== -1) {
      dispatchEvent(buffer.slice(0, boundary), handlers);
      buffer = buffer.slice(boundary + 2);
    }
  }
}

function dispatchEvent(rawEvent: string, handlers: ChatStreamHandlers) {
  let eventName = 'message';
  let data = '';
  for (const line of rawEvent.split('\n')) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      data += (data ? '\n' : '') + line.slice(5).trim();
    }
  }
  if (!data) return;

  switch (eventName) {
    case 'sources':
      handlers.onSources(JSON.parse(data) as SourceRef[]);
      break;
    case 'token':
      handlers.onToken((JSON.parse(data) as { text: string }).text);
      break;
    case 'done':
      handlers.onDone((JSON.parse(data) as { messageId: string }).messageId);
      break;
    case 'error': {
      const err = JSON.parse(data) as { code: string; message: string };
      handlers.onError(err.message, err.code);
      break;
    }
  }
}
