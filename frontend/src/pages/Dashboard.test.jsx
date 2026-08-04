import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '@context/AuthContext.jsx';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import Dashboard from './Dashboard.jsx';

const trades = vi.hoisted(() => [
  { id: 1, tradeRef: 'TRD-2026-0001', instrument: 'SAP.DE', quantity: 100, price: 250, status: 'MATCHED' },
  { id: 2, tradeRef: 'TRD-2026-0002', instrument: 'SAP.DE', quantity: 50, price: 251, status: 'UNMATCHED' },
]);

vi.mock('@hooks/useTradeStream.js', () => ({
  useTradeStream: () => ({ trades, isConnected: false }),
}));

function renderWithProviders(ui) {
  const user = { email: 'trader@db.com', role: 'TRADER' };

  return render(
    <AuthContext.Provider value={{ user, isLoading: false }}>
      <ThemeProvider>
        <MemoryRouter>{ui}</MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('<Dashboard />', () => {
  it('shows summary cards', () => {
    renderWithProviders(<Dashboard />);

    expect(screen.getByRole('heading', { name: /portfolio value/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /^matched$/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /^unmatched$/i })).toBeInTheDocument();
    expect(screen.getByText(/37,550/)).toBeInTheDocument();
  });
});
