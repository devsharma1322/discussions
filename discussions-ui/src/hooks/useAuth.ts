import { useCallback, useEffect, useRef, useState } from 'react';
import { useApolloClient, useMutation } from '@apollo/client/react';
import {
  LOGOUT_MUTATION,
  ME_QUERY,
  START_SESSION_MUTATION,
} from '../graphql/session';
import { clearToken, getToken, setToken } from '../lib/auth';
import type { AuthResult, User } from '../types/api';

interface MeData {
  me: User | null;
}

interface StartSessionData {
  startSession: AuthResult;
}

/**
 * Source of truth for the current anonymous identity.
 *
 * Imperative (no `useQuery` hooks) so the switch-identity flow doesn't have
 * to fight with Apollo's `useQuery` cache/skip interactions during
 * `resetStore`.
 *
 * Flow:
 *   - On mount: if a token is in storage, verify it via `me`; otherwise mint
 *     a new session via `startSession` and persist its JWT.
 *   - `startNewIdentity()`: best-effort logout server-side, clear local state,
 *     reset the Apollo cache, then mint and persist a fresh session.
 */
export function useAuth() {
  const apollo = useApolloClient();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const inFlightRef = useRef(false);

  const [startSession] = useMutation<StartSessionData>(START_SESSION_MUTATION);
  const [logoutMutation] = useMutation(LOGOUT_MUTATION);

  const mintNewSession = useCallback(async () => {
    const result = await startSession();
    const auth = result.data?.startSession;
    if (!auth) {
      throw new Error('startSession returned no data');
    }
    setToken(auth.engageAuth);
    setUser(auth.user);
  }, [startSession]);

  const verifyExistingSession = useCallback(async () => {
    try {
      const { data } = await apollo.query<MeData>({
        query: ME_QUERY,
        fetchPolicy: 'network-only',
      });
      if (data?.me) {
        setUser(data.me);
        return true;
      }
      return false;
    } catch {
      // Any failure verifying the stored token — expired JWT, secret rotation,
      // user deleted server-side — means the stored token is unusable. Returning
      // false lets the bootstrap caller fall through to mintNewSession() instead
      // of bubbling the error and leaving the splash hung.
      return false;
    }
  }, [apollo]);

  // Bootstrap on mount (or whenever we've torn down identity).
  useEffect(() => {
    if (inFlightRef.current) return;
    if (user) return; // already resolved
    inFlightRef.current = true;
    setLoading(true);

    (async () => {
      try {
        const token = getToken();
        if (token) {
          const ok = await verifyExistingSession();
          if (ok) return;
          // Stale/invalid token — fall through to mint a fresh one.
          clearToken();
        }
        await mintNewSession();
      } catch (err) {
        // Server unreachable. Surface as "no user, no longer loading" so the
        // UI can render an error state instead of an infinite skeleton.
        // eslint-disable-next-line no-console
        console.error('[useAuth] bootstrap failed', err);
      } finally {
        inFlightRef.current = false;
        setLoading(false);
      }
    })();
  }, [user, verifyExistingSession, mintNewSession]);

  const startNewIdentity = useCallback(async () => {
    setLoading(true);
    try {
      await logoutMutation();
    } catch {
      /* best effort */
    }
    clearToken();
    setUser(null);
    try {
      await apollo.clearStore(); // no refetch; we'll repopulate via mintNewSession
    } catch {
      /* clearStore can reject if there's a pending op — safe to ignore here */
    }
    try {
      await mintNewSession();
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('[useAuth] startNewIdentity failed', err);
    } finally {
      setLoading(false);
    }
  }, [apollo, logoutMutation, mintNewSession]);

  return { user, loading, startNewIdentity };
}
