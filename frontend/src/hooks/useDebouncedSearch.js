import { useEffect, useState } from 'react';

export function useDebouncedSearch(query, delay = 300) {
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedQuery(query);
    }, delay);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [query, delay]);

  return debouncedQuery;
}

export default useDebouncedSearch;
