import React from 'react';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message = 'We encountered an error while loading details. Please try again.',
  onRetry
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900 rounded-xl" data-testid="error-state">
      <span className="material-symbols-outlined text-[48px] text-error mb-4">
        warning
      </span>
      <h3 className="text-lg font-headline-md text-error mb-2">{title}</h3>
      <p className="text-sm text-on-surface-variant max-w-sm mb-6">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="h-10 px-6 bg-error text-on-error rounded-lg hover:opacity-90 transition-all text-sm font-semibold shadow-sm"
        >
          Try Again
        </button>
      )}
    </div>
  );
};
