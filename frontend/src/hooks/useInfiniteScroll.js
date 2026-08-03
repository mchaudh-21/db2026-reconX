import { useEffect, useRef } from 'react';

export function useInfiniteScroll(loadMore) {
  const sentinelRef = useRef(null);
  const loadMoreRef = useRef(loadMore);

  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);

  useEffect(() => {
    const sentinel = sentinelRef.current;

    if (!sentinel) {
      return undefined;
    }

    const observer = new IntersectionObserver((entries) => {
      const firstEntry = entries[0];

      if (firstEntry?.isIntersecting) {
        loadMoreRef.current();
      }
    });

    observer.observe(sentinel);

    return () => {
      observer.disconnect();
    };
  }, []);

  return sentinelRef;
}

export default useInfiniteScroll;
