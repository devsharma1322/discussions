import { useEffect, useRef, useState } from 'react';

/**
 * Returns a value that updates only after `delayMs` has elapsed without
 * changes. Used to throttle the search input so we don't fire a GraphQL
 * query on every keystroke.
 */
export function useDebounced<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  const handleRef = useRef<number | null>(null);

  useEffect(() => {
    if (handleRef.current !== null) {
      window.clearTimeout(handleRef.current);
    }
    handleRef.current = window.setTimeout(() => setDebounced(value), delayMs);
    return () => {
      if (handleRef.current !== null) {
        window.clearTimeout(handleRef.current);
      }
    };
  }, [value, delayMs]);

  return debounced;
}
