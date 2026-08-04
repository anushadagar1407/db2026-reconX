import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useNavigationType,
} from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';
import { api } from '../services/apiService.js';
import Login from './Login.jsx';

vi.mock('../services/apiService.js', () => ({
  api: {
    login: vi.fn(),
  },
}));

function Destination() {
  const location = useLocation();
  const navigationType = useNavigationType();

  return (
    <div>
      <div aria-label="Destination pathname">{location.pathname}</div>
      <div aria-label="Navigation type">{navigationType}</div>
    </div>
  );
}

function renderLogin(initialEntry = '/login') {
  const authLogin = vi.fn();

  render(
    <AuthContext.Provider value={{ user: null, isLoading: false, login: authLogin, logout: vi.fn() }}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="*" element={<Destination />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );

  return authLogin;
}

describe('<Login>', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.login.mockReset();
  });

  it('logs in and replaces the route with the attempted path', async () => {
    api.login.mockResolvedValue({ token: 'jwt-token', role: 'TRADER' });
    const authLogin = renderLogin({ pathname: '/login', state: { from: '/trades' } });
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Destination pathname')).toHaveTextContent('/trades');
    });
    expect(api.login).toHaveBeenCalledWith('admin@db.com', 'admin123');
    expect(authLogin).toHaveBeenCalledWith('jwt-token', 'TRADER');
    expect(screen.getByLabelText('Navigation type')).toHaveTextContent('REPLACE');
  });

  it('uses the root route when no attempted path exists', async () => {
    api.login.mockResolvedValue({ token: 'jwt-token', role: 'ADMIN' });
    const authLogin = renderLogin();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Destination pathname')).toHaveTextContent('/');
    });
    expect(authLogin).toHaveBeenCalledWith('jwt-token', 'ADMIN');
  });

  it('shows API failures and clears stale errors on a successful retry', async () => {
    api.login
      .mockRejectedValueOnce(new Error('HTTP 401: Invalid credentials'))
      .mockResolvedValueOnce({ token: 'jwt-token', role: 'VIEWER' });
    const authLogin = renderLogin();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 401: Invalid credentials');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Destination pathname')).toHaveTextContent('/');
    });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(authLogin).toHaveBeenCalledWith('jwt-token', 'VIEWER');
  });
});
