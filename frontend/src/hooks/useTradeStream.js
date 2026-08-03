import { useEffect, useState } from 'react';

export function useTradeStream(
  url = '/api/v1/trades/stream',
  maxTrades = 200,
) {
  const [trades, setTrades] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(url);

    eventSource.onopen = () => {
      setIsConnected(true);
    };

    eventSource.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);

        setTrades((previousTrades) =>
          [trade, ...previousTrades].slice(0, maxTrades),
        );
      } catch {
        // Ignore malformed SSE messages.
      }
    };

    eventSource.onerror = () => {
      setIsConnected(false);
    };

    return () => {
      eventSource.close();
      setIsConnected(false);
    };
  }, [url, maxTrades]);

  return {
    trades,
    isConnected,
  };
}

export default useTradeStream;
