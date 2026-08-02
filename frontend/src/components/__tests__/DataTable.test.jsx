// TICKET-ADV114 — RTL coverage for the compound DataTable contract.
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import DataTable from '../DataTable.jsx';
import { TradeRow } from '../TradeRow.jsx';

const columns = [
  { key: 'name', label: 'Name' },
  { key: 'score', label: 'Score', type: 'number' },
];

function renderRows(data, pageSize = 10, includePagination = false) {
  return render(
    <DataTable data={data} pageSize={pageSize}>
      <DataTable.Header columns={columns} />
      <DataTable.Body renderRow={(row) => (
        <div role="row">
          <span role="cell" data-testid={`row-${row.id}`}>{row.name}</span>
        </div>
      )} />
      {includePagination && <DataTable.Pagination />}
    </DataTable>
  );
}

function visibleRowIds() {
  return within(screen.getByRole('rowgroup'))
    .getAllByRole('row')
    .map((row) => row.querySelector('[data-testid]')?.getAttribute('data-testid'));
}

describe('<DataTable>', () => {
  it('supports dot-syntax composition and renders the current page through the callback', () => {
    renderRows([
      { id: 1, name: 'Alpha', score: 1 },
      { id: 2, name: 'Beta', score: 2 },
    ]);

    expect(screen.getByRole('columnheader', { name: 'Name' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Score' })).toBeInTheDocument();
    expect(screen.getByTestId('row-1')).toHaveTextContent('Alpha');
    expect(screen.getByTestId('row-2')).toHaveTextContent('Beta');
  });

  it('fails clearly when any subcomponent is rendered outside its provider', () => {
    const subcomponents = [
      <DataTable.Header key="header" columns={columns} />,
      <DataTable.Body key="body" renderRow={() => null} />,
      <DataTable.Pagination key="pagination" />,
    ];

    subcomponents.forEach((subcomponent) => {
      expect(() => render(subcomponent)).toThrow(/inside <DataTable>/i);
    });
  });

  function expectInvalidRow(renderRow, error = /closed ARIA div row contract/i) {
    expect(() => render(
      <DataTable data={[{ id: 1 }]}>
        <DataTable.Body renderRow={renderRow} />
      </DataTable>
    )).toThrow(error);
  }

  it('rejects direct native tr output', () => {
    expectInvalidRow(() => <tr><td>native row</td></tr>, /native <tr>.*closed ARIA div row contract/i);
  });

  it('rejects fragment-wrapped native tr output', () => {
    expectInvalidRow(() => <><tr><td>native row</td></tr></>, /direct <div role="row">/i);
  });

  it('rejects an unmarked component that returns a native tr', () => {
    function UnmarkedRow() {
      return <tr><td>native row</td></tr>;
    }

    expectInvalidRow(() => <UnmarkedRow />, /direct <div role="row">/i);
  });

  it('rejects arbitrary non-row content', () => {
    expectInvalidRow(() => <span>arbitrary content</span>, /direct <div role="row">/i);
  });

  it('rejects a row whose direct children are not ARIA cells', () => {
    expectInvalidRow(
      () => <div role="row"><span>missing cell role</span></div>,
      /direct host children role="cell"/i
    );
  });

  it('accepts a direct ARIA row with direct host cell children', () => {
    render(
      <DataTable data={[{ id: 1 }]}>
        <DataTable.Body renderRow={() => (
          <div role="row">
            <span role="cell" data-testid="valid-cell">valid row</span>
          </div>
        )} />
      </DataTable>
    );

    expect(screen.getByTestId('valid-cell')).toHaveTextContent('valid row');
  });

  it('accepts the trusted TradeRow with direct ARIA cell children', () => {
    const trade = {
      id: 7,
      tradeRef: 'TRD-7',
      instrumentSymbol: 'SAP.DE',
      quantity: 10,
      price: 125.5,
      status: 'MATCHED',
    };

    render(
      <DataTable data={[trade]}>
        <DataTable.Body renderRow={(row) => <TradeRow trade={row} />} />
      </DataTable>
    );

    expect(screen.getByRole('cell', { name: 'Trade reference' })).toHaveTextContent('TRD-7');
    expect(screen.getByRole('cell', { name: 'Instrument symbol' })).toHaveTextContent('SAP.DE');
  });

  it('toggles ascending and descending sort state with accessible aria-sort', async () => {
    const user = userEvent.setup();
    renderRows([
      { id: 1, name: 'Bravo', score: 2 },
      { id: 2, name: 'Alpha', score: 1 },
    ]);

    const nameHeader = screen.getByRole('columnheader', { name: 'Name' });
    const nameButton = within(nameHeader).getByRole('button', { name: 'Name' });
    expect(nameHeader).toHaveAttribute('aria-sort', 'none');

    await user.click(nameButton);
    expect(nameHeader).toHaveAttribute('aria-sort', 'ascending');
    expect(visibleRowIds()).toEqual(['row-2', 'row-1']);

    await user.click(nameButton);
    expect(nameHeader).toHaveAttribute('aria-sort', 'descending');
    expect(visibleRowIds()).toEqual(['row-1', 'row-2']);
  });

  it('sorts strings, numbers, dates, and null values without mutating the input', async () => {
    const user = userEvent.setup();
    const data = [
      { id: 'late', name: 'Zulu', score: 10, tradeDate: '2026-02-01' },
      { id: 'early', name: 'alpha', score: 2, tradeDate: '2026-01-01' },
      { id: 'middle', name: 'Mike', score: null, tradeDate: '2026-01-15' },
    ];

    render(
      <DataTable data={data} pageSize={10}>
        <DataTable.Header columns={[
          { key: 'name', label: 'Name', type: 'string' },
          { key: 'score', label: 'Score', type: 'number' },
          { key: 'tradeDate', label: 'Trade date', type: 'date' },
        ]} />
        <DataTable.Body renderRow={(row) => (
          <div role="row">
            <span role="cell" data-testid={`row-${row.id}`}>{row.id}</span>
          </div>
        )} />
      </DataTable>
    );

    await user.click(screen.getByRole('button', { name: 'Score' }));
    expect(visibleRowIds()).toEqual(['row-early', 'row-late', 'row-middle']);

    await user.click(screen.getByRole('button', { name: 'Trade date' }));
    expect(visibleRowIds()).toEqual(['row-early', 'row-middle', 'row-late']);

    await user.click(screen.getByRole('button', { name: 'Name' }));
    expect(visibleRowIds()).toEqual(['row-early', 'row-middle', 'row-late']);
    expect(data.map((row) => row.id)).toEqual(['late', 'early', 'middle']);
  });

  it('sorts before paginating and resets to the first page when sorting', async () => {
    const user = userEvent.setup();
    renderRows([
      { id: 'high', name: 'High', score: 3 },
      { id: 'low', name: 'Low', score: 1 },
      { id: 'mid', name: 'Mid', score: 2 },
    ], 2, true);

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Score' }));
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
    expect(visibleRowIds()).toEqual(['row-low', 'row-mid']);

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    expect(visibleRowIds()).toEqual(['row-high']);
  });

  it('enforces pagination boundaries and handles empty and single-page data', async () => {
    const user = userEvent.setup();
    const { unmount: unmountInitial } = renderRows([
      { id: 1, name: 'One', score: 1 },
      { id: 2, name: 'Two', score: 2 },
      { id: 3, name: 'Three', score: 3 },
    ], 2, true);

    const previous = screen.getByRole('button', { name: 'Previous page' });
    const next = screen.getByRole('button', { name: 'Next page' });
    expect(previous).toBeDisabled();
    expect(next).not.toBeDisabled();

    await user.click(next);
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
    expect(next).toBeDisabled();
    expect(previous).not.toBeDisabled();

    await user.click(next);
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();

    unmountInitial();
    const { unmount } = renderRows([], 2, true);
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
    expect(within(screen.getByRole('rowgroup')).queryAllByRole('row')).toHaveLength(0);
    unmount();

    renderRows([{ id: 1, name: 'Only', score: 1 }], 2, true);
    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  it('clamps the page when data or page size changes and keeps row keys stable', async () => {
    const user = userEvent.setup();
    const data = [
      { id: 1, name: 'Alpha', score: 1 },
      { id: 3, name: 'Gamma', score: 3 },
      { id: 2, name: 'Beta', score: 2 },
      { id: 4, name: 'Delta', score: 4 },
    ];
    const { rerender } = renderRows(data, 3, true);
    const betaRow = screen.getByTestId('row-2').parentElement;

    await user.click(screen.getByRole('button', { name: 'Score' }));
    expect(visibleRowIds()).toEqual(['row-1', 'row-2', 'row-3']);
    expect(screen.getByTestId('row-2').parentElement).toBe(betaRow);

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();

    rerender(
      <DataTable data={data.slice(0, 1)} pageSize={3}>
        <DataTable.Header columns={columns} />
        <DataTable.Body renderRow={(row) => (
          <div role="row">
            <span role="cell" data-testid={`row-${row.id}`}>{row.name}</span>
          </div>
        )} />
        <DataTable.Pagination />
      </DataTable>
    );
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
    expect(screen.getByTestId('row-1')).toBeInTheDocument();

    rerender(
      <DataTable data={data} pageSize={4}>
        <DataTable.Header columns={columns} />
        <DataTable.Body renderRow={(row) => (
          <div role="row">
            <span role="cell" data-testid={`row-${row.id}`}>{row.name}</span>
          </div>
        )} />
        <DataTable.Pagination />
      </DataTable>
    );
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
    expect(within(screen.getByRole('rowgroup')).getAllByRole('row')).toHaveLength(4);
  });
});
