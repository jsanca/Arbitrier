import React from 'react';
import type { OrderStatus } from '../models/types';

interface StatusBadgeProps {
  status: OrderStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const config: Record<OrderStatus, { bg: string; text: string; label: string; icon?: string }> = {
    DRAFT: {
      bg: 'bg-surface-container-high',
      text: 'text-on-surface-variant',
      label: 'Draft',
      icon: 'draft'
    },
    SUBMITTED: {
      bg: 'bg-primary-fixed',
      text: 'text-on-primary-fixed-variant',
      label: 'Submitted',
      icon: 'pending'
    },
    CREDIT_RESERVATION_REQUESTED: {
      bg: 'bg-secondary-fixed',
      text: 'text-on-secondary-fixed-variant',
      label: 'Checking Credit',
      icon: 'sync'
    },
    CREDIT_RESERVED: {
      bg: 'bg-secondary-container',
      text: 'text-on-secondary-container',
      label: 'Credit Approved',
      icon: 'lock'
    },
    INVENTORY_RESERVATION_REQUESTED: {
      bg: 'bg-secondary-fixed-dim',
      text: 'text-on-secondary-fixed',
      label: 'Reserving Inventory',
      icon: 'sync'
    },
    INVENTORY_FULLY_RESERVED: {
      bg: 'bg-primary-fixed-dim',
      text: 'text-on-primary-fixed',
      label: 'Inventory Confirmed',
      icon: 'done_all'
    },
    AWAITING_CUSTOMER_DECISION: {
      bg: 'bg-amber-100 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800',
      text: 'text-amber-800 dark:text-amber-300',
      label: 'Action Required: Availability Review',
      icon: 'warning'
    },
    CONFIRMED: {
      bg: 'bg-emerald-100 dark:bg-emerald-950/40 border border-emerald-300 dark:border-emerald-800',
      text: 'text-emerald-800 dark:text-emerald-300',
      label: 'Confirmed',
      icon: 'check_circle'
    },
    PARTIALLY_CONFIRMED: {
      bg: 'bg-teal-100 dark:bg-teal-950/40 border border-teal-300 dark:border-teal-800',
      text: 'text-teal-800 dark:text-teal-300',
      label: 'Partially Confirmed',
      icon: 'done'
    },
    CANCELLED: {
      bg: 'bg-rose-100 dark:bg-rose-950/40 border border-rose-300 dark:border-rose-800',
      text: 'text-rose-800 dark:text-rose-300',
      label: 'Cancelled',
      icon: 'cancel'
    },
    REJECTED: {
      bg: 'bg-red-100 dark:bg-red-950/40 border border-red-300 dark:border-red-800',
      text: 'text-red-800 dark:text-red-300',
      label: 'Rejected',
      icon: 'error'
    }
  };

  const current = config[status] || { bg: 'bg-surface-container', text: 'text-on-surface-variant', label: status };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 text-xs font-semibold rounded-full ${current.bg} ${current.text}`}
      data-testid="status-badge"
    >
      {current.icon && (
        <span className="material-symbols-outlined text-[14px]" aria-hidden="true">
          {current.icon}
        </span>
      )}
      {current.label}
    </span>
  );
};
