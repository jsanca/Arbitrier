import React from 'react';
import type { OrderTimelineEvent } from '../models/types';

interface BusinessTimelineProps {
  timeline: OrderTimelineEvent[];
}

export const BusinessTimeline: React.FC<BusinessTimelineProps> = ({ timeline }) => {
  if (!timeline || timeline.length === 0) {
    return (
      <div className="text-sm text-secondary p-4 bg-surface-container-low rounded-lg text-center">
        No timeline events generated yet.
      </div>
    );
  }

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  return (
    <div className="relative border-l border-outline-variant ml-3 pl-8 space-y-6 py-2" data-testid="business-timeline">
      {timeline.map((event, idx) => {
        let dotColor = 'bg-surface-container-high border-outline-variant';
        let iconName = 'radio_button_unchecked';
        let titleColor = 'text-secondary font-medium';

        if (event.completed) {
          dotColor = 'bg-emerald-500 border-emerald-500 text-white';
          iconName = 'check';
          titleColor = 'text-on-surface font-semibold';
        } else if (event.failed) {
          dotColor = 'bg-error border-error text-white';
          iconName = 'error';
          titleColor = 'text-error font-semibold';
        } else if (event.timestamp) {
          // In progress (timestamp present but not completed/failed)
          dotColor = 'bg-primary border-primary text-white animate-pulse';
          iconName = 'sync';
          titleColor = 'text-primary font-semibold';
        }

        return (
          <div key={idx} className="relative group">
            {/* Timeline Dot */}
            <span
              className={`absolute -left-11 top-1.5 w-6 h-6 rounded-full border-2 flex items-center justify-center text-[14px] ${dotColor}`}
            >
              <span className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: "'wght' 700" }}>
                {iconName}
              </span>
            </span>

            {/* Event Info */}
            <div>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
                <h4 className={`text-sm ${titleColor}`}>{event.status}</h4>
                {event.timestamp && (
                  <span className="text-xs text-secondary font-mono">
                    {formatDate(event.timestamp)}
                  </span>
                )}
              </div>
              {event.note && (
                <p className={`text-xs mt-1 border-l-2 p-2 rounded-r-md ${event.failed ? 'bg-error-container/20 border-error text-error' : 'bg-surface-container border-primary text-on-surface-variant'}`}>
                  {event.note}
                </p>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
