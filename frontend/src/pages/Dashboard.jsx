import { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard() {
  const { trades, isConnected } = useTradeStream();

  const portfolioValue = useMemo(
    () =>
      trades.reduce(
        (total, trade) =>
          total + Number(trade.quantity ?? 0) * Number(trade.price ?? 0),
        0,
      ),
    [trades],
  );

  const matched = useMemo(
    () =>
      trades.filter(
        (trade) => trade.status === 'MATCHED',
      ).length,
    [trades],
  );

  const openBreaks = useMemo(
    () =>
      trades.filter((trade) =>
        ['UNMATCHED', 'DISPUTED'].includes(trade.status),
      ).length,
    [trades],
  );

  return (
    <section>
      <h2>Dashboard</h2>

      <div className="stat-grid">
        <StatCard
          label="Portfolio value"
          value={portfolioValue.toLocaleString('en-US', {
            style: 'currency',
            currency: 'USD',
          })}
        />

        <StatCard
          label="Trades streamed"
          value={trades.length}
        />

        <StatCard
          label="Matched"
          value={matched}
        />

        <StatCard
          label="Open breaks"
          value={openBreaks}
        />
      </div>

      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
