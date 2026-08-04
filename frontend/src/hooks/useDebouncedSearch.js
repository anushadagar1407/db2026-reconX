// TICKET-ADV117 — useDebouncedSearch(query, delay).
import { useEffect, useState } from 'react';

export function useDebouncedSearch(query, delay = 300) {
  const [debounced, setDebounced] = useState(query);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(query), delay);

    return () => clearTimeout(timer);
  }, [query, delay]);

  return debounced;
}
