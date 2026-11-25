import React from 'react';

interface State {
  hasError: boolean;
  message: string;
}

export class ErrorBoundary extends React.Component<React.PropsWithChildren, State> {
  constructor(props: React.PropsWithChildren) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#f7f5f0] flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-white border border-[#e2ddd2] rounded-xl p-6 space-y-4">
            <h2 className="text-lg font-semibold text-zinc-900">Something went wrong</h2>
            <p className="text-sm text-zinc-600 break-words">{this.state.message}</p>
            <button
              onClick={() => window.location.reload()}
              className="px-4 py-2 text-sm font-medium bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12] transition-colors"
            >
              Reload Workspace
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
