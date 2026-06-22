import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

type ToastKind = 'success' | 'error' | 'info';
interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastApi {
  toast: (message: string, kind?: ToastKind) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

export function ToastHost({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback((message: string, kind: ToastKind = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((current) => [...current, { id, kind, message }]);
    window.setTimeout(() => dismiss(id), 3000);
  }, [dismiss]);

  const api = useMemo<ToastApi>(() => ({ toast }), [toast]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[1000] flex w-80 flex-col gap-2">
        {toasts.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => dismiss(t.id)}
            className={
              'pointer-events-auto flex items-start gap-2 rounded-2xl px-3 py-2 text-left text-sm font-medium shadow-lg ring-1 transition ' +
              kindStyles(t.kind)
            }
          >
            <span aria-hidden>{kindIcon(t.kind)}</span>
            <span>{t.message}</span>
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) return { toast: () => {} };
  return ctx;
}

function kindStyles(kind: ToastKind): string {
  switch (kind) {
    case 'success': return 'bg-emerald-50 text-emerald-900 ring-emerald-200';
    case 'error':   return 'bg-rose-50 text-rose-900 ring-rose-200';
    case 'info':    return 'bg-purple-900 text-white ring-purple-700';
  }
}

function kindIcon(kind: ToastKind): string {
  switch (kind) {
    case 'success': return '✅';
    case 'error':   return '⚠️';
    case 'info':    return 'ℹ️';
  }
}
