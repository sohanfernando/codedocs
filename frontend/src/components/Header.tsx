import type { Theme } from '../hooks/useTheme';
import { ThemeToggle } from './ThemeToggle';

interface SourcesButton {
  count: number;
  onClick: () => void;
}

interface Props {
  theme: Theme;
  onToggleTheme: () => void;
  onOpenSidebar: () => void;
  userEmail: string;
  onLogout: () => void;
  /** Present only when there's a ready, selected repo — nothing to show otherwise. */
  sourcesButton?: SourcesButton;
}

export function Header({ theme, onToggleTheme, onOpenSidebar, userEmail, onLogout, sourcesButton }: Props) {
  return (
    <header className="flex h-12 shrink-0 items-center gap-2 border-b border-neutral-200 px-3 dark:border-neutral-800 md:px-4">
      <button
        type="button"
        onClick={onOpenSidebar}
        aria-label="Open repositories"
        className="rounded-md p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100 md:hidden"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-4 w-4">
          <path strokeLinecap="round" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <span className="text-sm font-medium text-neutral-900 dark:text-neutral-100">codedocs</span>
      <span className="hidden text-xs text-neutral-400 dark:text-neutral-500 sm:inline">
        ask questions about any GitHub repository
      </span>

      <div className="ml-auto flex items-center gap-1">
        {sourcesButton && (
          <button
            type="button"
            onClick={sourcesButton.onClick}
            className="rounded-md px-2 py-1 text-xs font-medium text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100 md:hidden"
          >
            Sources{sourcesButton.count > 0 ? ` (${sourcesButton.count})` : ''}
          </button>
        )}
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
        <span className="hidden max-w-48 truncate text-xs text-neutral-400 dark:text-neutral-500 sm:inline">
          {userEmail}
        </span>
        <button
          type="button"
          onClick={onLogout}
          className="rounded-md px-2 py-1 text-xs font-medium text-neutral-500 hover:bg-neutral-100 hover:text-neutral-900 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-neutral-100"
        >
          Log out
        </button>
      </div>
    </header>
  );
}
