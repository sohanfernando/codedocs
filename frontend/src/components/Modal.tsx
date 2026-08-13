import { useEffect } from 'react';
import { createPortal } from 'react-dom';

interface Props {
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

/**
 * Generic overlay + centered panel, portaled to <body>. Closes on Escape or
 * a backdrop click. Not opinionated about content — compose it (see
 * ConfirmDialog) for specific dialogs rather than adding one-off props here.
 */
export function Modal({ open, onClose, children }: Props) {
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 dark:bg-black/60"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl dark:bg-neutral-900 dark:ring-1 dark:ring-neutral-800"
      >
        {children}
      </div>
    </div>,
    document.body,
  );
}
