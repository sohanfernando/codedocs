import type { SourceRef } from '../api/types';
import { SourceCard } from './SourceCard';

interface Props {
  sources: SourceRef[];
  highlightedIndex: number | null;
  /** Below the md breakpoint this renders as an off-canvas drawer instead of a static column. */
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

export function SourcesPanel({ sources, highlightedIndex, mobileOpen, onCloseMobile }: Props) {
  return (
    <aside
      className={[
        'fixed inset-y-0 right-0 z-40 flex w-56 max-w-[85vw] shrink-0 flex-col',
        'border-l border-neutral-200 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900',
        'transition-transform duration-200 md:static md:z-auto md:translate-x-0',
        mobileOpen ? 'translate-x-0' : 'translate-x-full',
      ].join(' ')}
    >
      <div className="flex items-center justify-between border-b border-neutral-200 px-3 py-2.5 dark:border-neutral-800">
        <span className="text-xs text-neutral-500 dark:text-neutral-400">
          {sources.length > 0 ? `${sources.length} sources` : 'Sources'}
        </span>
        <button
          type="button"
          onClick={onCloseMobile}
          aria-label="Close"
          className="rounded p-1 text-neutral-400 hover:text-neutral-900 dark:text-neutral-500 dark:hover:text-neutral-100 md:hidden"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-4 w-4">
            <path strokeLinecap="round" d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </div>

      <div className="flex-1 space-y-1.5 overflow-y-auto p-3">
        {sources.length === 0 ? (
          <p className="text-xs text-neutral-400 dark:text-neutral-500">
            Cited files appear here once you ask a question.
          </p>
        ) : (
          sources.map((source) => (
            <SourceCard
              key={source.index}
              source={source}
              highlighted={source.index === highlightedIndex}
            />
          ))
        )}
      </div>
    </aside>
  );
}