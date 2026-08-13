import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';

interface Props {
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string) => Promise<void>;
}

const inputClasses =
  'w-full rounded-md border border-neutral-300 bg-white px-2.5 py-1.5 text-xs outline-none ' +
  'placeholder:text-neutral-400 focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-900 ' +
  'dark:text-neutral-100 dark:placeholder:text-neutral-500 dark:focus:border-blue-400';

export function AuthScreen({ onLogin, onRegister }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      if (mode === 'login') {
        await onLogin(email, password);
      } else {
        await onRegister(email, password);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.');
      setSubmitting(false);
    }
    // No `finally` reset on success: the component unmounts as soon as the
    // parent swaps in the authenticated view, so there's no state left to update.
  }

  return (
    <div className="flex h-full items-center justify-center bg-white dark:bg-neutral-950">
      <div className="w-full max-w-xs px-4">
        <div className="mb-6 text-center">
          <div className="text-sm font-medium text-neutral-900 dark:text-neutral-100">codedocs</div>
          <p className="mt-1 text-xs text-neutral-400 dark:text-neutral-500">
            {mode === 'login' ? 'Sign in to continue' : 'Create an account to get started'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-2.5">
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            className={inputClasses}
          />
          <input
            type="password"
            required
            minLength={8}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Password"
            className={inputClasses}
          />

          {error && <p className="text-xs text-red-600 dark:text-red-400">{error}</p>}

          <button
            type="submit"
            disabled={submitting || !email.trim() || !password}
            className="w-full cursor-pointer rounded-md bg-blue-600 py-1.5 text-xs font-medium text-white disabled:cursor-default disabled:opacity-40 dark:bg-blue-500"
          >
            {submitting ? '…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        <button
          type="button"
          onClick={() => {
            setMode((m) => (m === 'login' ? 'register' : 'login'));
            setError(null);
          }}
          className="mt-3 w-full cursor-pointer text-center text-xs text-neutral-400 hover:text-neutral-700 dark:text-neutral-500 dark:hover:text-neutral-300"
        >
          {mode === 'login' ? "Don't have an account? Create one" : 'Already have an account? Sign in'}
        </button>
      </div>
    </div>
  );
}
