import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext.jsx';

function AuthProbe({ onLoadingRender }) {
  const { user, isLoading, login, logout } = useAuth();
  onLoadingRender?.(isLoading);

  return (
    <div>
      <div role="status" aria-label="Auth loading">{String(isLoading)}</div>
      <div aria-label="Current user">
        {user ? `${user.token}|${user.role ?? 'no-role'}` : 'signed-out'}
      </div>
      <button type="button" onClick={() => login('fresh-token', 'TRADER')}>
        Login with role
      </button>
      <button type="button" onClick={() => login('fresh-token')}>
        Login without role
      </button>
      <button type="button" onClick={logout}>Log out</button>
    </div>
  );
}

function renderAuthProbe(onLoadingRender) {
  return render(
    <AuthProvider>
      <AuthProbe onLoadingRender={onLoadingRender} />
    </AuthProvider>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('starts loading, then completes with an empty session', async () => {
    const loadingStates = [];
    renderAuthProbe((isLoading) => loadingStates.push(isLoading));

    expect(loadingStates[0]).toBe(true);
    expect(screen.getByLabelText('Current user')).toHaveTextContent('signed-out');

    await waitFor(() => {
      expect(screen.getByRole('status', { name: 'Auth loading' })).toHaveTextContent('false');
    });
  });

  it('restores a token and role from sessionStorage', async () => {
    sessionStorage.setItem('reconx-token', 'restored-token');
    sessionStorage.setItem('reconx-role', 'VIEWER');
    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByLabelText('Current user')).toHaveTextContent('restored-token|VIEWER');
    });
    expect(screen.getByRole('status', { name: 'Auth loading' })).toHaveTextContent('false');
  });

  it('persists login data and removes a stale role when omitted', async () => {
    const user = userEvent.setup();
    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByRole('status', { name: 'Auth loading' })).toHaveTextContent('false');
    });
    await user.click(screen.getByRole('button', { name: 'Login with role' }));

    expect(sessionStorage.getItem('reconx-token')).toBe('fresh-token');
    expect(sessionStorage.getItem('reconx-role')).toBe('TRADER');
    expect(screen.getByLabelText('Current user')).toHaveTextContent('fresh-token|TRADER');

    await user.click(screen.getByRole('button', { name: 'Login without role' }));

    expect(sessionStorage.getItem('reconx-token')).toBe('fresh-token');
    expect(sessionStorage.getItem('reconx-role')).toBeNull();
    expect(screen.getByLabelText('Current user')).toHaveTextContent('fresh-token|no-role');
  });

  it('clears both session keys and user state on logout', async () => {
    const user = userEvent.setup();
    sessionStorage.setItem('reconx-token', 'restored-token');
    sessionStorage.setItem('reconx-role', 'ADMIN');
    renderAuthProbe();

    await waitFor(() => {
      expect(screen.getByLabelText('Current user')).toHaveTextContent('restored-token|ADMIN');
    });
    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(sessionStorage.getItem('reconx-token')).toBeNull();
    expect(sessionStorage.getItem('reconx-role')).toBeNull();
    expect(screen.getByLabelText('Current user')).toHaveTextContent('signed-out');
  });
});
