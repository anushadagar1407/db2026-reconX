// TICKET-ADV115 — useWebSocket(url) with bounded auto-reconnect.
import { useCallback, useEffect, useRef, useState } from 'react';

const DEFAULT_BASE_DELAY = 500;
const DEFAULT_MAX_DELAY = 30_000;
const DEFAULT_MAX_RETRIES = 5;

export function useWebSocket(url, options = {}) {
  const {
    reconnect = true,
    maxRetries = DEFAULT_MAX_RETRIES,
    baseDelay = DEFAULT_BASE_DELAY,
    maxDelay = DEFAULT_MAX_DELAY,
  } = options ?? {};
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('connecting');
  const socketRef = useRef(null);
  const retriesRef = useRef(0);
  const reconnectTimerRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    retriesRef.current = 0;
    setStatus('connecting');

    const connect = () => {
      if (cancelled) return;

      const socket = new WebSocket(url);
      socketRef.current = socket;

      const isCurrentSocket = () => !cancelled && socketRef.current === socket;

      socket.onopen = () => {
        if (!isCurrentSocket()) return;
        retriesRef.current = 0;
        setStatus('open');
      };

      socket.onmessage = (event) => {
        if (!isCurrentSocket()) return;

        try {
          setData(JSON.parse(event.data));
        } catch {
          setData(event.data);
        }
      };

      socket.onerror = () => {
        if (isCurrentSocket()) setStatus('error');
      };

      socket.onclose = () => {
        if (!isCurrentSocket()) return;

        socketRef.current = null;
        setStatus('closed');

        if (!reconnect || retriesRef.current >= maxRetries) return;

        const attempt = retriesRef.current;
        retriesRef.current += 1;
        const delay = Math.min(
          DEFAULT_MAX_DELAY,
          maxDelay,
          baseDelay * 2 ** attempt,
        );

        reconnectTimerRef.current = setTimeout(() => {
          reconnectTimerRef.current = null;
          connect();
        }, delay);
      };
    };

    connect();

    return () => {
      cancelled = true;

      if (reconnectTimerRef.current !== null) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }

      const socket = socketRef.current;
      socketRef.current = null;
      if (socket) socket.close();
    };
  }, [url, reconnect, maxRetries, baseDelay, maxDelay]);

  const send = useCallback((payload) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) return;

    socket.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
  }, []);

  return { data, status, send };
}
