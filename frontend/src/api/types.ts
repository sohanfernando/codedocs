export type RepoStatus =
  | 'PENDING'
  | 'CLONING'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'READY'
  | 'FAILED';

/** Mirrors RepoResponse.java */
export interface Repo {
  id: string;
  name: string | null;
  remoteUrl: string;
  branch: string | null;
  status: RepoStatus;
  documentCount: number;
  chunkCount: number;
  errorMessage: string | null;
  indexedAt: string | null;
}

/** Mirrors SourceRef.java. `index` matches the [n] citation in the answer. */
export interface SourceRef {
  index: number;
  filePath: string;
  startLine: number;
  endLine: number;
  githubUrl: string;
  similarity: number;
}

/** Mirrors ChatThreadResponse.java */
export interface ChatThread {
  id: string;
  /** The repo this thread was created from — used for sidebar nesting. */
  repoId: string;
  /** Every repo this thread actually searches — one entry for an ordinary single-repo thread. */
  repoIds: string[];
  title: string | null;
  /** Non-null while a public share link is active for this thread. */
  shareToken: string | null;
  createdAt: string;
  updatedAt: string;
}

export type Feedback = 'UP' | 'DOWN';

/** Mirrors ChatMessageResponse.java */
export interface PersistedMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sources: SourceRef[];
  failed: boolean;
  feedback: Feedback | null;
  createdAt: string;
}

/** Mirrors SharedThreadResponse.RepoSummary */
export interface SharedRepoSummary {
  name: string | null;
  url: string;
}

/** Mirrors SharedThreadResponse.java — the public, unauthenticated view. */
export interface SharedThread {
  title: string | null;
  repos: SharedRepoSummary[];
  messages: PersistedMessage[];
  createdAt: string;
}

/** Mirrors UserResponse.java */
export interface User {
  id: string;
  email: string;
  createdAt: string;
}

/** Mirrors ErrorResponse.java */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: string[];
  timestamp: string;
}

export const TERMINAL_STATUSES: RepoStatus[] = ['READY', 'FAILED'];

export function isTerminal(status: RepoStatus): boolean {
  return TERMINAL_STATUSES.includes(status);
}