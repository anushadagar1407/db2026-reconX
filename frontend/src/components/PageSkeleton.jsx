import React from 'react';

export default function PageSkeleton() {
  return (
    <div className="page-skeleton" aria-busy="true" aria-live="polite">
      <div className="loader">Loading…</div>
    </div>
  );
}