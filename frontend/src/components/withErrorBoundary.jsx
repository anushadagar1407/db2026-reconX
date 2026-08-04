// TICKET-ADV113 — withErrorBoundary HOC: wraps a component in an error boundary.
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    if (typeof this.props.onError === 'function') {
      this.props.onError(error, info);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  }

  render() {
    if (this.state.hasError) {
      const errorMessage = this.state.error instanceof Error
        ? this.state.error.message
        : String(this.state.error);

      return (
        <div role="alert" className="error-fallback">
          <h2>Something went wrong</h2>
          <p>{errorMessage || 'An unexpected error occurred.'}</p>
          <button type="button" onClick={this.handleReset}>Try again</button>
        </div>
      );
    }

    return this.props.children;
  }
}

export function withErrorBoundary(Component, onError) {
  function WithErrorBoundary(props) {
    return (
      <ErrorBoundary onError={onError}>
        <Component {...props} />
      </ErrorBoundary>
    );
  }
  WithErrorBoundary.displayName = `withErrorBoundary(${Component.displayName || Component.name || 'Component'})`;
  return WithErrorBoundary;
}
