import React from 'react';

interface LoadingStateProps {
  message?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({ message = 'Loading details...' }) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center" data-testid="loading-state">
      <div className="relative w-12 h-12 mb-4">
        <div className="absolute inset-0 rounded-full border-4 border-surface-container-high"></div>
        <div className="absolute inset-0 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
      </div>
      <p className="text-secondary text-sm font-medium animate-pulse">{message}</p>
    </div>
  );
};
