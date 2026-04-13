'use client';

import React, { Component, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-900 p-4">
          <div className="max-w-md w-full">
            <div className="glass-card p-8 text-center">
              <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
              
              <h1 className="text-xl font-bold text-white mb-2">
                Something went wrong
              </h1>
              
              <p className="text-gray-400 mb-6">
                {this.state.error?.message || 'An unexpected error occurred'}
              </p>
              
              <Button
                onClick={() => window.location.reload()}
                className="w-full"
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Reload Page
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
