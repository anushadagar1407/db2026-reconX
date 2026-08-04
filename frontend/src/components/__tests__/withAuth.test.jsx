import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom';
import { AuthContext } from '@context/AuthContext.jsx';
import { withAuth } from '../withAuth.jsx';

function LoginDestination() {
  const location = useLocation();
  return <div data-testid="login-destination">{location.state?.from || 'no-attempted-path'}</div>;
}

function renderGuard(auth, path = '/protected') {
  function ProtectedPage({ label }) {
    return <div data-testid="protected-page">{label}</div>;
  }

  const Protected = withAuth(ProtectedPage);

  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<LoginDestination />} />
          <Route path="*" element={<Protected label="forwarded prop" />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe('withAuth', () => {
  it('redirects during render without rendering the protected page', () => {
    const ProtectedPage = vi.fn(() => <div data-testid="protected-page">secret</div>);
    const Protected = withAuth(ProtectedPage);

    render(
      <AuthContext.Provider value={{ user: null, isLoading: false }}>
        <MemoryRouter initialEntries={['/trades/42']}>
          <Routes>
            <Route path="/login" element={<LoginDestination />} />
            <Route path="*" element={<Protected />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.queryByTestId('protected-page')).not.toBeInTheDocument();
    expect(ProtectedPage).not.toHaveBeenCalled();
    expect(screen.getByTestId('login-destination')).toHaveTextContent('/trades/42');
  });

  it('shows loading and delays both redirect and protected rendering', () => {
    renderGuard({ user: null, isLoading: true });

    expect(screen.getByRole('status')).toHaveTextContent('Loading...');
    expect(screen.queryByTestId('protected-page')).not.toBeInTheDocument();
    expect(screen.queryByTestId('login-destination')).not.toBeInTheDocument();
  });

  it('preserves the attempted pathname in redirect state', () => {
    renderGuard({ user: null, isLoading: false }, '/audit/trades/ABC');

    expect(screen.getByTestId('login-destination')).toHaveTextContent('/audit/trades/ABC');
  });

  it('forwards props for authenticated users', () => {
    renderGuard({ user: { token: 'token' }, isLoading: false });

    expect(screen.getByTestId('protected-page')).toHaveTextContent('forwarded prop');
  });

  it('sets a useful display name', () => {
    function Dashboard() {
      return null;
    }

    expect(withAuth(Dashboard).displayName).toBe('withAuth(Dashboard)');
  });
});
