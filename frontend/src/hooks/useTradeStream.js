// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useRef, useState } from 'react';

const MAX_BUFFER = 200;

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);
  const sourceRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    const source = new EventSource(url);
    sourceRef.current = source;
    setConnected(false);

    const isCurrentSource = () => !cancelled && sourceRef.current === source;

    source.onopen = () => {
      if (isCurrentSource()) setConnected(true);
    };

    source.onmessage = (event) => {
      if (!isCurrentSource()) return;

      try {
        const trade = JSON.parse(event.data);
        setTrades((previous) => [trade, ...previous].slice(0, MAX_BUFFER));
      } catch {
        // Malformed events are ignored so one bad payload cannot break the tree.
      }
    };

    source.onerror = () => {
      if (isCurrentSource()) setConnected(false);
    };

    return () => {
      cancelled = true;
      if (sourceRef.current === source) sourceRef.current = null;
      source.close();
    };
  }, [url]);

  return { trades, isConnected };
}
