import React from 'react';

interface EmptyStateProps {
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  icon?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No items found',
  description = 'There is no data to display right now.',
  actionText,
  onAction,
  icon = 'inbox'
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-surface-container-lowest border border-outline-variant rounded-xl" data-testid="empty-state">
      <span className="material-symbols-outlined text-[48px] text-secondary mb-4 opacity-40">
        {icon}
      </span>
      <h3 className="text-lg font-headline-md text-on-surface mb-2">{title}</h3>
      <p className="text-sm text-on-surface-variant max-w-sm mb-6">{description}</p>
      {actionText && onAction && (
        <button
          onClick={onAction}
          className="h-10 px-6 bg-primary text-on-primary rounded-lg hover:bg-primary-container transition-all text-sm font-semibold shadow-sm"
        >
          {actionText}
        </button>
      )}
    </div>
  );
};
