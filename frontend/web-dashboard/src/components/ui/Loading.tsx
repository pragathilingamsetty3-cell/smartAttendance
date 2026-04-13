'use client';

import React from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/cn';

interface LoadingProps {
  size?: 'sm' | 'md' | 'lg';
  text?: string;
  className?: string;
}

export const Loading: React.FC<LoadingProps> = ({
  size = 'md',
  text,
  className
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8'
  };

  return (
    <div className={cn('flex items-center justify-center', className)}>
      <div className="flex flex-col items-center space-y-2">
        <Loader2 className={cn('animate-spin text-blue-500', sizeClasses[size])} />
        {text && (
          <p className="text-sm text-gray-400 loading-dots">{text}</p>
        )}
      </div>
    </div>
  );
};

export const LoadingSpinner: React.FC<LoadingProps> = ({
  size = 'md',
  className
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8'
  };

  return (
    <Loader2 className={cn('animate-spin text-blue-500', sizeClasses[size], className)} />
  );
};

export const PageLoading: React.FC<{ text?: string }> = ({ text = 'Loading...' }) => {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <Loading size="lg" text={text} />
    </div>
  );
};

export default Loading;
