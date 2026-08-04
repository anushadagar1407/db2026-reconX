import { useEffect, useRef, useState } from 'react';

type LoadState = 'idle' | 'loading' | 'ready' | 'unavailable';

interface LiveDemoFrameProps {
  src: string;
  loadTimeoutMs?: number;
}

export function LiveDemoFrame({ src, loadTimeoutMs = 10_000 }: LiveDemoFrameProps) {
  const [loadState, setLoadState] = useState<LoadState>('idle');
  const frameRef = useRef<HTMLIFrameElement>(null);
  const demoUrl = new URL(src, window.location.href);
  const demoOrigin = demoUrl.origin;
  const frameUrl = new URL(demoUrl);
  frameUrl.searchParams.set('presentationOrigin', window.location.origin);

  useEffect(() => {
    function handleMessage(event: MessageEvent) {
      if (
        event.origin === demoOrigin
        && event.source === frameRef.current?.contentWindow
        && event.data?.type === 'reconx:ready'
      ) {
        setLoadState((current) => current === 'loading' ? 'ready' : current);
      }
    }

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [demoOrigin]);

  useEffect(() => {
    if (loadState !== 'loading') return undefined;

    const timeout = window.setTimeout(() => setLoadState('unavailable'), loadTimeoutMs);
    return () => window.clearTimeout(timeout);
  }, [loadState, loadTimeoutMs]);

  function launch() {
    setLoadState('loading');
  }

  return (
    <div className="live-demo">
      <div className="live-demo__toolbar">
        <span role="status" aria-live="polite">
          Demo: {loadState}
        </span>
        {loadState === 'idle' && (
          <button type="button" onClick={launch}>Launch embedded demo</button>
        )}
        {loadState === 'unavailable' && (
          <button type="button" onClick={launch}>Retry</button>
        )}
        <a href={src} target="_blank" rel="noreferrer">Open full demo</a>
      </div>

      {(loadState === 'loading' || loadState === 'ready') && (
        <iframe
          ref={frameRef}
          className="live-demo__frame"
          title="ReconX live demo"
          src={frameUrl.toString()}
          allow="fullscreen"
          sandbox="allow-forms allow-popups allow-same-origin allow-scripts"
          onError={() => setLoadState('unavailable')}
        />
      )}

      {loadState === 'idle' && (
        <div className="live-demo__idle" aria-label="Embedded demo idle state">
          <span className="live-demo__idle-label">Guarded live window · export-safe idle state</span>
          <strong>Activate the frame only when the presenter is ready.</strong>
          <span>No runtime capture is claimed here. The full-demo link and source/API walkthrough remain available.</span>
        </div>
      )}

      {loadState === 'unavailable' && (
        <div className="live-demo__fallback" role="alert">
          The embedded app did not become available. Use the full-demo link or switch to a source/API walkthrough.
        </div>
      )}
    </div>
  );
}
