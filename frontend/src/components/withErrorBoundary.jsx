// TICKET-ADV113 — withErrorBoundary HOC: wraps a component in an error boundary.
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      resetKey: 0,
    };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
	void error;
	void info;
  }

  handleReset = () => {
    this.setState((previousState) => ({
      error: null,
      resetKey: previousState.resetKey + 1,
    }));
  };

  render() {
    if (this.state.error) {
      return (
        <section className="error-boundary" role="alert">
          <h2>Something went wrong</h2>
          <p>{this.state.error.message}</p>

          <button type="button" onClick={this.handleReset}>
            Try again
          </button>
        </section>
      );
    }

    return (
      <React.Fragment key={this.state.resetKey}>
        {this.props.children}
      </React.Fragment>
    );
  }
}

export function withErrorBoundary(Component) {
  function WithErrorBoundary(props) {
    return (
      <ErrorBoundary>
        <Component {...props} />
      </ErrorBoundary>
    );
  }

  WithErrorBoundary.displayName =
    `withErrorBoundary(${Component.displayName || Component.name || 'Component'})`;

  return WithErrorBoundary;
}

export { ErrorBoundary };

export default withErrorBoundary;
