import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { CreateCircleModal } from './CreateCircleModal';

/**
 * Vibrant top bar — gradient purple → fuchsia → indigo.
 * The display-name pill is bright amber so it pops against the dark header.
 */
export function IdentityBar() {
  const { user, startNewIdentity } = useAuth();
  const [switching, setSwitching] = useState(false);
  const [showCreate, setShowCreate] = useState(false);

  async function handleSwitch() {
    setSwitching(true);
    try {
      await startNewIdentity();
    } finally {
      setSwitching(false);
    }
  }

  return (
    <>
      <header className="sticky top-0 z-30 bg-gradient-to-r from-purple-700 via-fuchsia-600 to-indigo-700 shadow-lg shadow-purple-900/20">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3 text-white">
          <span className="flex items-center gap-2 text-xl font-bold tracking-tight">
            <span aria-hidden className="text-2xl">🎭</span>
            AnonCircles
          </span>
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <button
              type="button"
              onClick={() => setShowCreate(true)}
              className="rounded-full bg-amber-400 px-4 py-1.5 font-semibold text-amber-950 shadow-sm transition hover:bg-amber-300 hover:shadow"
            >
              + Create circle
            </button>
            <span className="text-purple-100/90">You are</span>
            <span className="rounded-full bg-yellow-300 px-3 py-1 font-semibold text-amber-900 shadow-sm">
              {user?.displayName ?? '…'}
            </span>
            <button
              type="button"
              onClick={handleSwitch}
              disabled={switching}
              className="rounded-full border border-white/40 bg-white/10 px-3 py-1 text-white backdrop-blur transition hover:bg-white/20 disabled:opacity-60"
            >
              {switching ? 'Switching…' : 'Switch identity'}
            </button>
          </div>
        </div>
      </header>
      {showCreate && <CreateCircleModal onClose={() => setShowCreate(false)} />}
    </>
  );
}
