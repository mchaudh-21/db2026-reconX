import { useCallback, useEffect, useRef, useState } from 'react';

export function useWebSocket(
  url,
  {
    reconnect = true,
    maxRetries = 5,
    baseDelay = 500,
    maxDelay = 8000,
  } = {},
) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('closed');

  const socketRef = useRef(null);
  const retryCountRef = useRef(0);
  const reconnectTimerRef = useRef(null);
  const shouldReconnectRef = useRef(reconnect);

  useEffect(() => {
    shouldReconnectRef.current = reconnect;
  }, [reconnect]);

  useEffect(() => {
    let cancelled = false;

    function connect() {
      if (!url || cancelled) {
        return;
      }

      setStatus('connecting');

      const socket = new WebSocket(url);
      socketRef.current = socket;

      socket.onopen = () => {
        retryCountRef.current = 0;
        setStatus('open');
      };

      socket.onmessage = (event) => {
        try {
          setData(JSON.parse(event.data));
        } catch {
          setData(event.data);
        }
      };

      socket.onerror = () => {
        setStatus('error');
      };

      socket.onclose = () => {
        socketRef.current = null;
        setStatus('closed');

        if (
          cancelled ||
          !shouldReconnectRef.current ||
          retryCountRef.current >= maxRetries
        ) {
          return;
        }

        const delay = Math.min(
          maxDelay,
          baseDelay * 2 ** retryCountRef.current,
        );

        retryCountRef.current += 1;

        reconnectTimerRef.current = window.setTimeout(
          connect,
          delay,
        );
      };
    }

    connect();

    return () => {
      cancelled = true;
      shouldReconnectRef.current = false;

      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
      }

      socketRef.current?.close();
    };
  }, [url, maxRetries, baseDelay, maxDelay]);

  const send = useCallback((message) => {
    const socket = socketRef.current;

    if (socket?.readyState !== WebSocket.OPEN) {
      return false;
    }

    socket.send(
      typeof message === 'string'
        ? message
        : JSON.stringify(message),
    );

    return true;
  }, []);

  return {
    data,
    status,
    send,
  };
}

export default useWebSocket;
