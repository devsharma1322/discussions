import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';

/**
 * Wraps the app with a "waking the demo…" overlay during cold-start on
 * Render's free tier.
 *
 * Why this exists: Render free Web Services sleep after 15 min idle and take
 * ~30–40s to cold-boot.  Without this overlay, a recruiter clicking the
 * portfolio link sees a frozen blank page while the JVM warms up — looks
 * broken, drives bounces.
 *
 * Strategy:
 *   - On mount, ping the GraphQL endpoint's actuator health (derived from
 *     VITE_GRAPHQL_URL).
 *   - If the first ping returns 200 in < 1s, hide the overlay and render the
 *     app (no flash for the happy path of a warm server).
 *   - Otherwise show a friendly progress overlay and keep retrying every 2s
 *     until /actuator/health=200, then hide.
 *   - Gives up at 90s and renders the app anyway — the user sees the real
 *     loading state and any errors that surface.
 *
 * Local dev: a localhost backend always answers fast so the overlay never
 * appears.  No conditional needed.
 */
export function WakingSplash({ children }: { children: ReactNode }) {
  // In local dev (vite dev server) the backend is on the same machine and
  // either responds in milliseconds or isn't running at all (in which case
  // the splash would just hide the real "ECONNREFUSED" error from the user).
  // Either way, never gate on it locally — only in production builds where
  // the Render free-tier sleep is the real concern.
  if (import.meta.env.DEV) {
    return <>{children}</>;
  }

  const [awake, setAwake] = useState(false);
  const [elapsedSec, setElapsedSec] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const start = Date.now();
    const healthUrl = deriveHealthUrl();

    const tick = setInterval(() => {
      if (!cancelled) setElapsedSec(Math.floor((Date.now() - start) / 1000));
    }, 500);

    async function probe() {
      const probeStart = Date.now();
      try {
        const ctrl = new AbortController();
        const timeout = setTimeout(() => ctrl.abort(), 5000);
        const res = await fetch(healthUrl, { signal: ctrl.signal, cache: 'no-store' });
        clearTimeout(timeout);
        if (res.ok) {
          if (cancelled) return;
          // Suppress the flash for already-warm servers: only show the
          // overlay if the *first* probe took noticeable time.
          if (Date.now() - probeStart < 1000 && elapsedSec === 0) {
            setAwake(true);
            return;
          }
          setAwake(true);
          return;
        }
      } catch {
        // 5xx, network error, abort — keep retrying.
      }
      if (!cancelled && Date.now() - start < 90_000) {
        setTimeout(probe, 2000);
      } else if (!cancelled) {
        // Give up: let the app try and surface its own errors.
        setAwake(true);
      }
    }
    probe();

    return () => {
      cancelled = true;
      clearInterval(tick);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (awake) return <>{children}</>;

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed inset-0 z-[100] flex items-center justify-center bg-gradient-to-br from-purple-50 via-amber-50 to-emerald-50 p-6"
    >
      <div className="w-full max-w-md rounded-2xl bg-white/80 p-8 text-center shadow-xl ring-2 ring-purple-200 backdrop-blur">
        <div className="mx-auto mb-5 h-14 w-14 animate-spin rounded-full border-4 border-purple-200 border-t-purple-600" />
        <h1 className="text-lg font-bold text-purple-900">Waking the demo…</h1>
        <p className="mt-2 text-sm text-purple-700">
          This portfolio runs on a free hosting tier that sleeps when idle.
          First load takes about 30 seconds while the server boots.
        </p>
        <p className="mt-4 text-xs text-purple-400">
          {elapsedSec}s elapsed · subsequent loads are instant
        </p>
      </div>
    </div>
  );
}

/**
 * Derives the actuator health URL from VITE_GRAPHQL_URL.
 *  https://x.onrender.com/graphql → https://x.onrender.com/actuator/health
 *  http://localhost:4003/graphql  → http://localhost:4003/actuator/health
 */
function deriveHealthUrl(): string {
  const graphqlUrl = import.meta.env.VITE_GRAPHQL_URL ?? 'http://localhost:4003/graphql';
  try {
    const u = new URL(graphqlUrl);
    u.pathname = '/actuator/health';
    u.search = '';
    return u.toString();
  } catch {
    return graphqlUrl.replace(/\/graphql.*$/, '/actuator/health');
  }
}
