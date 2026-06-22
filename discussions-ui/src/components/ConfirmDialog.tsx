import type { ReactNode } from 'react';

interface ConfirmDialogProps {
  title: string;
  body: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  title,
  body,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  destructive = false,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-purple-950/60 p-4 backdrop-blur-sm"
      onClick={onCancel}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className={
          'w-full max-w-sm overflow-hidden rounded-2xl bg-white shadow-2xl ring-2 ' +
          (destructive ? 'ring-rose-300' : 'ring-purple-300')
        }
      >
        <div
          className={
            'px-6 py-4 text-white ' +
            (destructive
              ? 'bg-gradient-to-r from-rose-600 to-red-600'
              : 'bg-gradient-to-r from-purple-600 to-fuchsia-600')
          }
        >
          <h2 className="text-lg font-bold">{title}</h2>
        </div>
        <div className="p-6">
          <div className="text-sm text-gray-700">{body}</div>
          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              className="rounded-full border border-gray-300 px-4 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-60"
            >
              {cancelLabel}
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={loading}
              className={
                'rounded-full px-4 py-1.5 text-sm font-semibold text-white shadow-sm disabled:opacity-60 ' +
                (destructive
                  ? 'bg-rose-600 hover:bg-rose-700'
                  : 'bg-purple-600 hover:bg-purple-700')
              }
            >
              {loading ? 'Working…' : confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
