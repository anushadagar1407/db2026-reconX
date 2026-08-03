// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
import React, { memo, useCallback, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

const demoTrades = [
  { id: '1', tradeRef: 'AAA-20260803-0001', symbol: 'AAPL', qty: 100, price: 184.25, status: 'PENDING' },
  { id: '2', tradeRef: 'BBB-20260803-0002', symbol: 'MSFT', qty: 75, price: 412.1, status: 'MATCHED' },
];

const TradeRow = memo(function TradeRow({ trade, onClick, selected }) {
  return (
    <button
      type="button"
      className={selected ? 'trade-row trade-row--selected' : 'trade-row'}
      onClick={() => onClick(trade.id)}
    >
      <span>{trade.tradeRef}</span>
      <span>{trade.symbol}</span>
      <span>{trade.qty}</span>
      <span>{trade.price}</span>
      <span>{trade.status}</span>
    </button>
  );
});

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [selectedId, setSelectedId] = useState(null);

  const handleSelect = useCallback((id) => setSelectedId(id), []);
  const visibleTrades = data.items.length > 0 ? data.items : demoTrades;

  // TODO(TICKET-ADV114 + ADV117): useEffect that:
  //   - builds a query string from `page` and `debounced` (status filter)
  //   - calls api.listTrades(params) and stores the response in `data`
  //   - re-runs whenever `page` or `debounced` changes
  //   - degrades gracefully on error (set empty page).

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        {/* TODO(TICKET-ADV114): render a DataTable.Body with `rows={data.items}`
            and a `render` prop that returns one <span> per column. */}
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
      <p aria-live="polite">Selected trade: {selectedId ?? 'none'}</p>
      <div className="trade-list">
        {visibleTrades.map((trade) => (
          <TradeRow
            key={trade.id}
            trade={trade}
            selected={selectedId === trade.id}
            onClick={handleSelect}
          />
        ))}
      </div>
    </section>
  );
}

export default withAuth(Trades);
