import type { ReactNode } from 'react';
import { useAuth } from '../hooks/useAuth';

export function AuthGuard({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();

  if (loading || !user) {
    return (
      <div className="flex min-h-full items-center justify-center p-6">
        <div className="w-full max-w-md rounded-2xl bg-white/70 p-8 text-center shadow-md ring-2 ring-purple-200 backdrop-blur">
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-purple-200 border-t-purple-600" />
          <p className="text-sm font-medium text-purple-900">Picking you an alias…</p>
          <p className="mt-1 text-xs text-purple-500">
            New here? We assign every visitor a friendly disguise.
          </p>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}
