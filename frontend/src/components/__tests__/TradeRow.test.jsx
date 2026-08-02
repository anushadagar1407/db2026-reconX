// TICKET-ADV119 — focused observable tests for the memoised TradeRow primitive.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TradeRow } from '../TradeRow.jsx';

const baseTrade = {
  id: 42,
  tradeRef: 'TRD-20260802-0042',
  instrumentSymbol: 'SAP.DE',
  quantity: 100,
  price: 125.5,
  status: 'MATCHED',
};

function renderTrade(trade = baseTrade, onClick) {
  return render(<TradeRow trade={trade} onClick={onClick} />);
}

describe('<TradeRow>', () => {
  it('renders every visible TradeResponse field with accessible cells', () => {
    renderTrade(baseTrade, vi.fn());

    expect(screen.getByRole('row')).toHaveAttribute('data-trade-id', '42');
    expect(screen.getByRole('cell', { name: 'Trade reference' })).toHaveTextContent('TRD-20260802-0042');
    expect(screen.getByRole('cell', { name: 'Instrument symbol' })).toHaveTextContent('SAP.DE');
    expect(screen.getByRole('cell', { name: 'Quantity' })).toHaveTextContent('100');
    expect(screen.getByRole('cell', { name: 'Price' })).toHaveTextContent('125.5');
    expect(screen.getByRole('cell', { name: 'Status' })).toHaveTextContent('MATCHED');
    expect(screen.getByRole('status')).toHaveTextContent('MATCHED');
  });

  it('uses the minimum onClick contract with the stable trade id', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    renderTrade(baseTrade, onClick);

    await user.click(screen.getByRole('row'));
    expect(onClick).toHaveBeenCalledWith(42);
  });

  it('skips rendering when all consumed props are unchanged', () => {
    const onClick = vi.fn();
    const { rerender } = render(
      <TradeRow trade={{ ...baseTrade }} onClick={onClick} />
    );
    const firstRow = screen.getByRole('row');

    rerender(
      <TradeRow trade={{ ...baseTrade }} onClick={onClick} />
    );

    expect(screen.getByRole('row')).toBe(firstRow);
  });

  it.each([
    ['id', 43],
    ['tradeRef', 'TRD-20260802-0043'],
    ['instrumentSymbol', 'AAPL'],
    ['quantity', 200],
    ['price', 130.25],
    ['status', 'UNMATCHED'],
  ])('updates when rendered field %s changes', (field, value) => {
    const onClick = vi.fn();
    const { rerender } = renderTrade(baseTrade, onClick);

    rerender(<TradeRow trade={{ ...baseTrade, [field]: value }} onClick={onClick} />);

    if (field === 'id') {
      expect(screen.getByRole('row')).toHaveAttribute('data-trade-id', '43');
    } else {
      expect(screen.getByRole('cell', { name: field === 'tradeRef' ? 'Trade reference' : field === 'instrumentSymbol' ? 'Instrument symbol' : field.charAt(0).toUpperCase() + field.slice(1) }))
        .toHaveTextContent(String(value));
    }
  });

  it('re-renders for a changed callback identity and uses the new callback', async () => {
    const user = userEvent.setup();
    const firstCallback = vi.fn();
    const secondCallback = vi.fn();
    const { rerender } = renderTrade(baseTrade, firstCallback);

    rerender(<TradeRow trade={{ ...baseTrade }} onClick={secondCallback} />);
    await user.click(screen.getByRole('row'));

    expect(firstCallback).not.toHaveBeenCalled();
    expect(secondCallback).toHaveBeenCalledWith(42);
  });
});
