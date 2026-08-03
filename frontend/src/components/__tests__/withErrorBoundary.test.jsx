import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, vi } from 'vitest';
import { withErrorBoundary } from '../withErrorBoundary.jsx';

describe('withErrorBoundary', () => {
  it('renders the wrapped component normally', () => {
    function Ready() {
      return <p>Ready to reconcile</p>;
    }

    const WrappedReady = withErrorBoundary(Ready);

    render(<WrappedReady />);

    expect(screen.getByText('Ready to reconcile')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows an accessible fallback and reports the render failure', () => {
    const error = new Error('First failure');
    const reporter = vi.fn();

    function Broken() {
      throw error;
    }

    const WrappedBroken = withErrorBoundary(Broken, reporter);

    render(<WrappedBroken />);

    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong');
    expect(screen.getByRole('alert')).toHaveTextContent('First failure');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    expect(screen.queryByText('broken content')).not.toBeInTheDocument();

    expect(reporter).toHaveBeenCalledTimes(1);
    expect(reporter).toHaveBeenCalledWith(
      error,
      expect.objectContaining({ componentStack: expect.any(String) }),
    );
  });

  it('recovers after reset and catches a later failure again', async () => {
    const firstError = new Error('First failure');
    const secondError = new Error('Second failure');
    const reporter = vi.fn();

    function Toggleable({ shouldThrow }) {
      if (shouldThrow) {
        throw shouldThrow === 'first' ? firstError : secondError;
      }

      return <p>Recovered content</p>;
    }

    const WrappedToggleable = withErrorBoundary(Toggleable, reporter);
    const user = userEvent.setup();
    const { rerender } = render(<WrappedToggleable shouldThrow="first" />);

    expect(screen.getByRole('alert')).toHaveTextContent('First failure');
    expect(reporter).toHaveBeenCalledTimes(1);

    rerender(<WrappedToggleable shouldThrow={false} />);
    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(screen.getByText('Recovered content')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    rerender(<WrappedToggleable shouldThrow="second" />);

    expect(screen.getByRole('alert')).toHaveTextContent('Second failure');
    expect(reporter).toHaveBeenCalledTimes(2);
    expect(reporter).toHaveBeenLastCalledWith(
      secondError,
      expect.objectContaining({ componentStack: expect.any(String) }),
    );
  });

  it('forwards props and exposes a useful display name', () => {
    function PropEcho({ value }) {
      return <p>{value}</p>;
    }

    const WrappedPropEcho = withErrorBoundary(PropEcho);

    render(<WrappedPropEcho value="Forwarded value" />);

    expect(screen.getByText('Forwarded value')).toBeInTheDocument();
    expect(WrappedPropEcho.displayName).toBe('withErrorBoundary(PropEcho)');
  });
});
