/**
 * engage-auth token storage.
 *
 * Dev: localStorage (simple, observable, easy to inspect).
 * Prod upgrade: switch to httpOnly + Secure + SameSite=Strict cookie set by the BFF;
 * the `authLink` in apolloClient.ts becomes a no-op and the browser sends the
 * cookie automatically — one-line change.
 */
const KEY = 'engageAuth';

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem(KEY);
}

export function setToken(token: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(KEY, token);
}

export function clearToken(): void {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(KEY);
}
