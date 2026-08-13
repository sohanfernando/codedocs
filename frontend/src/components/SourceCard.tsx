import type { SourceRef } from '../api/types';

interface Props {
  source: SourceRef;
  highlighted: boolean;
}

export function SourceCard({ source, highlighted }: Props) {
  const fileName = source.filePath.split('/').pop() ?? source.filePath;
  const directory = source.filePath.slice(0, source.filePath.lastIndexOf('/'));

  return (
    <a
      id={'source-' + source.index}
      href={source.githubUrl}
      target="_blank"
      rel="noreferrer"
      className={[
        'block rounded-lg border px-2.5 py-2 transition',
        highlighted
          ? 'border-blue-500 bg-blue-50 dark:border-blue-400 dark:bg-blue-950/40'
          : 'border-neutral-200 bg-white hover:border-neutral-300 dark:border-neutral-800 dark:bg-neutral-900 dark:hover:border-neutral-600',
      ].join(' ')}
    >
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-xs font-medium text-blue-600 dark:text-blue-400">
          [{source.index}]
        </span>
        <span className="text-[11px] text-neutral-400 dark:text-neutral-500">
          {Math.round(source.similarity * 100)}%
        </span>
      </div>

      <div className="mt-0.5 truncate font-mono text-[11px] text-neutral-900 dark:text-neutral-100">
        {fileName}
      </div>

      {directory && (
        <div className="truncate font-mono text-[10px] text-neutral-400 dark:text-neutral-500">
          {directory}
        </div>
      )}

      <div className="mt-0.5 text-[11px] text-neutral-500 dark:text-neutral-400">
        L{source.startLine}-{source.endLine}
      </div>
    </a>
  );
}