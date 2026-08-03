// TICKET-ADV119 — memoised TradeResponse row primitive.
import React from 'react';

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '—' : String(value);
}

function TradeRowImpl({ trade, onClick }) {
  const currentTrade = trade ?? {};
  const status = displayValue(currentTrade.status);
  const statusClass = typeof currentTrade.status === 'string'
    ? currentTrade.status.toLowerCase()
    : '';
  const isInteractive = typeof onClick === 'function';
  const activate = () => onClick?.(currentTrade.id);
  const handleKeyDown = (event) => {
    if (isInteractive && (event.key === 'Enter' || event.key === ' ')) {
      event.preventDefault();
      activate();
    }
  };

  return (
    <div
      className="data-table__row trade-row"
      role="row"
      data-trade-id={currentTrade.id == null ? undefined : String(currentTrade.id)}
      tabIndex={isInteractive ? 0 : undefined}
      onClick={isInteractive ? activate : undefined}
      onKeyDown={isInteractive ? handleKeyDown : undefined}
    >
      <span role="cell" aria-label="Trade reference">{displayValue(currentTrade.tradeRef)}</span>
      <span role="cell" aria-label="Instrument symbol">{displayValue(currentTrade.instrumentSymbol)}</span>
      <span role="cell" aria-label="Quantity">{displayValue(currentTrade.quantity)}</span>
      <span role="cell" aria-label="Price">{displayValue(currentTrade.price)}</span>
      <span role="cell" aria-label="Status">
        <span className={`status-pill ${statusClass}`} role="status">{status}</span>
      </span>
    </div>
  );
}

function areEqual(previousProps, nextProps) {
  const previousTrade = previousProps.trade ?? {};
  const nextTrade = nextProps.trade ?? {};

  return previousTrade.id === nextTrade.id
    && previousTrade.tradeRef === nextTrade.tradeRef
    && previousTrade.instrumentSymbol === nextTrade.instrumentSymbol
    && previousTrade.quantity === nextTrade.quantity
    && previousTrade.price === nextTrade.price
    && previousTrade.status === nextTrade.status
    && previousProps.onClick === nextProps.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
TradeRow.dataTableRow = true;

export default TradeRow;
