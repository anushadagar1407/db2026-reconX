// TICKET-ADV118 — useInfiniteScroll: invokes loadMore() when sentinel is visible.
import { useEffect, useRef } from 'react';

export function useInfiniteScroll(
  loadMore,
  { rootMargin = '200px', threshold = 0.1 } = {}
) {
  const sentinelRef = useRef(null);
  const loadMoreRef = useRef(loadMore);
  const observerRef = useRef(null);
  const observedNodeRef = useRef(null);

  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);

  useEffect(() => {
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadMoreRef.current();
      }
    }, { rootMargin, threshold });

    observerRef.current = observer;

    const node = sentinelRef.current;
    if (node) {
      observer.observe(node);
      observedNodeRef.current = node;
    }

    return () => {
      observer.disconnect();
      observerRef.current = null;
      observedNodeRef.current = null;
    };
  }, [rootMargin, threshold]);

  useEffect(() => {
    const observer = observerRef.current;
    const node = sentinelRef.current;
    const observedNode = observedNodeRef.current;

    if (!observer || node === observedNode) return;

    if (observedNode) observer.unobserve?.(observedNode);
    if (node) observer.observe(node);
    observedNodeRef.current = node;
  });

  return sentinelRef;
}
