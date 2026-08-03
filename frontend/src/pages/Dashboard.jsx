// TICKET-ADV120 — useMemo for portfolio-value calc.
// TICKET-ADV116 — useTradeStream live feed.
import React, { useMemo } from 'react';
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

  // Worth caching: this is an O(n) aggregation over the live feed, so it should
  // only recompute when the trade list itself changes.
  const portfolioValue = useMemo(
    () => trades.reduce((sum, trade) => sum + (trade.quantity * trade.price || 0), 0),
    [trades]
  );

  // Worth caching: this groups and counts trades from the same source array,
  // and returns a stable object for the stat cards that depend on it.
  const tradeStats = useMemo(() => {
    const matchedTrades = trades.filter((trade) => trade.status === 'MATCHED');
    const unmatchedTrades = trades.filter((trade) => trade.status === 'UNMATCHED');
    const disputedTrades = trades.filter((trade) => trade.status === 'DISPUTED');

    return {
      matched: matchedTrades.length,
      unmatched: unmatchedTrades.length,
      disputed: disputedTrades.length,
      matchedValue: matchedTrades.reduce(
        (sum, trade) => sum + (trade.quantity * trade.price || 0),
        0
      ),
    };
  }, [trades]);

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio value (USD)" value={portfolioValue.toLocaleString()} />
        <StatCard label="Trades streamed" value={trades.length} />
        <StatCard label="Matched" value={tradeStats.matched} />
        <StatCard label="Unmatched" value={tradeStats.unmatched} />
        <StatCard label="Disputed" value={tradeStats.disputed} />
        <StatCard label="Matched value (USD)" value={tradeStats.matchedValue.toLocaleString()} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
