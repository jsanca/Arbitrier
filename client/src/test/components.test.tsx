import { describe, test, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingState } from '../components/LoadingState';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { CreditSummary } from '../components/CreditSummary';
import { OrderLineTable } from '../components/OrderLineTable';
import { BusinessTimeline } from '../components/BusinessTimeline';
import type { OrderItem, OrderTimelineEvent } from '../models/types';

describe('StatusBadge Component', () => {
  test('renders correct text and classes for CONFIRMED status', () => {
    render(<StatusBadge status="CONFIRMED" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('Confirmed');
    expect(badge).toHaveClass('text-emerald-800');
  });

  test('renders correct text and classes for DRAFT status', () => {
    render(<StatusBadge status="DRAFT" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge).toHaveTextContent('Draft');
    expect(badge).toHaveClass('text-on-surface-variant');
  });
});

describe('LoadingState Component', () => {
  test('renders with default message', () => {
    render(<LoadingState />);
    expect(screen.getByTestId('loading-state')).toBeInTheDocument();
    expect(screen.getByText('Loading details...')).toBeInTheDocument();
  });

  test('renders with custom message', () => {
    render(<LoadingState message="Fetching catalog..." />);
    expect(screen.getByText('Fetching catalog...')).toBeInTheDocument();
  });
});

describe('EmptyState Component', () => {
  test('renders labels and handles action click', () => {
    const handleAction = vi.fn();
    render(
      <EmptyState
        title="Empty Cart"
        description="No items added."
        actionText="Shop Now"
        onAction={handleAction}
      />
    );
    expect(screen.getByText('Empty Cart')).toBeInTheDocument();
    expect(screen.getByText('No items added.')).toBeInTheDocument();
    
    const btn = screen.getByRole('button', { name: 'Shop Now' });
    fireEvent.click(btn);
    expect(handleAction).toHaveBeenCalledTimes(1);
  });
});

describe('ErrorState Component', () => {
  test('renders warning title and triggers retry action', () => {
    const handleRetry = vi.fn();
    render(<ErrorState title="API Fail" message="Failed to fetch orders" onRetry={handleRetry} />);
    expect(screen.getByText('API Fail')).toBeInTheDocument();
    
    const btn = screen.getByRole('button', { name: 'Try Again' });
    fireEvent.click(btn);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });
});

describe('CreditSummary Component', () => {
  test('renders available vs total limits', () => {
    render(<CreditSummary availableCredit={50000} creditLimit={150000} />);
    expect(screen.getByTestId('credit-summary')).toBeInTheDocument();
    expect(screen.getByText('$50,000.00')).toBeInTheDocument();
    expect(screen.getByText('$150,000.00')).toBeInTheDocument();
  });

  test('renders critical warning when credit is below 15%', () => {
    render(<CreditSummary availableCredit={10000} creditLimit={100000} />);
    expect(screen.getByText(/Critical: Available credit limit is below 15%/i)).toBeInTheDocument();
  });
});

describe('OrderLineTable Component', () => {
  const mockItems: OrderItem[] = [
    {
      productId: 'p-1',
      productName: 'Hydraulic Hose',
      sku: 'SKU-001',
      price: 100,
      requestedQuantity: 5,
      availableQuantity: 5,
      acceptedQuantity: 5
    }
  ];

  test('renders row item details and requested total subtotal value', () => {
    render(<OrderLineTable items={mockItems} status="CONFIRMED" />);
    expect(screen.getByTestId('order-line-table')).toBeInTheDocument();
    expect(screen.getByText('Hydraulic Hose')).toBeInTheDocument();
    expect(screen.getByText('SKU-001')).toBeInTheDocument();
    expect(screen.getByText('$500.00')).toBeInTheDocument();
  });
});

describe('BusinessTimeline Component', () => {
  test('renders business steps progression', () => {
    const mockTimeline: OrderTimelineEvent[] = [
      { status: 'Order received', timestamp: '2026-07-10T12:00:00Z', completed: true },
      { status: 'Availability confirmed', timestamp: '2026-07-10T12:01:00Z', completed: true }
    ];
    render(<BusinessTimeline timeline={mockTimeline} />);
    expect(screen.getByTestId('business-timeline')).toBeInTheDocument();
    expect(screen.getByText('Order received')).toBeInTheDocument();
    expect(screen.getByText('Availability confirmed')).toBeInTheDocument();
  });
});
