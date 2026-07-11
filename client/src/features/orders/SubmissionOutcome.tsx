import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import type { Order, Company } from '../../models/types';
import { services } from '../../services/mockServices';
import { OrderLineTable } from '../../components/OrderLineTable';
import { LoadingState } from '../../components/LoadingState';

export const SubmissionOutcome: React.FC = () => {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId') || '';
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadOrderOutcome = async () => {
      if (!orderId) {
        navigate('/dashboard');
        return;
      }
      try {
        const item = await services.order.getOrderById(orderId);
        if (!item) {
          navigate('/dashboard');
          return;
        }
        setOrder(item);

        const currentCompany = await services.company.getCompany(item.companyId);
        setCompany(currentCompany);
      } catch (err) {
        console.error('Failed to load outcome', err);
      } finally {
        setLoading(false);
      }
    };

    loadOrderOutcome();
  }, [orderId, navigate]);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  if (loading) return <LoadingState message="Fetching final execution summaries..." />;
  if (!order || !company) return null;

  const isConfirmed = order.status === 'CONFIRMED' || order.status === 'PARTIALLY_CONFIRMED';
  const isAwaitingStockDecision = order.status === 'AWAITING_CUSTOMER_DECISION';
  const isRejected = order.status === 'REJECTED';
  const isCancelled = order.status === 'CANCELLED';

  return (
    <div className="max-w-3xl mx-auto space-y-8" data-testid="submission-outcome-page">
      {/* 1. Final Outcome Header Cards */}

      {isConfirmed && (
        <div className="bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-300 dark:border-emerald-800 p-8 rounded-2xl shadow-sm text-center space-y-4">
          <span className="material-symbols-outlined text-[56px] text-emerald-500">check_circle</span>
          <div>
            <h1 className="text-2xl font-bold text-emerald-900 dark:text-emerald-300">Procurement Order Confirmed</h1>
            <p className="text-xs text-emerald-700 dark:text-emerald-400 mt-1.5">
              Order Reference ID: <span className="font-mono font-bold bg-white/60 dark:bg-black/20 px-2 py-0.5 rounded">{order.referenceId}</span>
            </p>
          </div>
          <p className="text-sm text-on-surface-variant max-w-lg mx-auto">
            Your bulk order has been successfully validated, credit reservation approved, and passed to logistics for packaging.
          </p>
          <div className="flex gap-3 justify-center pt-2">
            <Link
              to={`/orders/${order.id}`}
              className="h-10 px-5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[16px]">visibility</span>
              <span>View Order Status</span>
            </Link>
            <Link
              to="/dashboard"
              className="h-10 px-5 border border-emerald-300 dark:border-emerald-800 text-emerald-800 dark:text-emerald-300 text-xs font-semibold rounded-lg flex items-center justify-center hover:bg-emerald-100/30 transition-all"
            >
              Go to Dashboard
            </Link>
          </div>
        </div>
      )}

      {isAwaitingStockDecision && (
        <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-300 dark:border-amber-800 p-8 rounded-2xl shadow-sm text-center space-y-4">
          <span className="material-symbols-outlined text-[56px] text-amber-600 animate-pulse">report_problem</span>
          <div>
            <h1 className="text-2xl font-bold text-amber-900 dark:text-amber-300">Order Submitted: Action Required</h1>
            <p className="text-xs text-amber-700 dark:text-amber-400 mt-1.5">
              Order Reference ID: <span className="font-mono font-bold bg-white/60 dark:bg-black/20 px-2 py-0.5 rounded">{order.referenceId}</span>
            </p>
          </div>
          <p className="text-sm text-on-surface-variant max-w-lg mx-auto">
            Your order has been received, but pre-saga checks found partial item availability. Please review the inventory allocations to proceed with confirmation.
          </p>
          <div className="flex gap-3 justify-center pt-2">
            <Link
              to={`/orders/review?orderId=${order.id}`}
              className="h-10 px-5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[16px]">gavel</span>
              <span>Resolve Stock Allocation Now</span>
            </Link>
            <Link
              to="/dashboard"
              className="h-10 px-5 border border-amber-300 dark:border-amber-800 text-amber-800 dark:text-amber-300 text-xs font-semibold rounded-lg flex items-center justify-center hover:bg-amber-100/30 transition-all"
            >
              Back to Dashboard
            </Link>
          </div>
        </div>
      )}

      {isRejected && (
        <div className="bg-red-50 dark:bg-red-950/20 border border-red-300 dark:border-red-800 p-8 rounded-2xl shadow-sm text-center space-y-4">
          <span className="material-symbols-outlined text-[56px] text-error">cancel</span>
          <div>
            <h1 className="text-2xl font-bold text-red-900 dark:text-red-300">Procurement Order Rejected</h1>
            <p className="text-xs text-red-700 dark:text-red-400 mt-1.5">
              Order Reference ID: <span className="font-mono font-bold bg-white/60 dark:bg-black/20 px-2 py-0.5 rounded">{order.referenceId}</span>
            </p>
          </div>
          <p className="text-sm text-on-surface-variant max-w-lg mx-auto">
            Your bulk order was rejected because the order amount exceeds your company's available credit limit of{' '}
            <span className="font-bold">{formatCurrency(company.availableCredit)}</span>.
          </p>
          <div className="flex gap-3 justify-center pt-2">
            <button
              onClick={() => navigate('/orders/new')}
              className="h-10 px-5 bg-error hover:opacity-90 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[16px]">edit</span>
              <span>Modify Cart / Adjust Quantity</span>
            </button>
            <Link
              to="/dashboard"
              className="h-10 px-5 border border-red-300 dark:border-red-800 text-red-800 dark:text-red-300 text-xs font-semibold rounded-lg flex items-center justify-center hover:bg-red-100/30 transition-all"
            >
              Back to Dashboard
            </Link>
          </div>
        </div>
      )}

      {isCancelled && (
        <div className="bg-surface-container border border-outline-variant p-8 rounded-2xl shadow-sm text-center space-y-4">
          <span className="material-symbols-outlined text-[56px] text-secondary">block</span>
          <div>
            <h1 className="text-2xl font-bold text-on-surface">Order Cancelled</h1>
            <p className="text-xs text-on-surface-variant mt-1.5">
              Order Reference ID: <span className="font-mono font-bold bg-surface-container-high px-2 py-0.5 rounded">{order.referenceId}</span>
            </p>
          </div>
          <p className="text-sm text-on-surface-variant max-w-lg mx-auto">
            You rejected the proposed stock allocations, and all credit/inventory requests have been cancelled.
          </p>
          <div className="flex gap-3 justify-center pt-2">
            <Link
              to="/orders/new"
              className="h-10 px-5 bg-primary text-on-primary hover:opacity-90 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-all shadow-sm"
            >
              Create New Order
            </Link>
            <Link
              to="/dashboard"
              className="h-10 px-5 border border-outline-variant hover:bg-surface-container-low text-xs font-semibold rounded-lg flex items-center justify-center transition-all"
            >
              Go to Dashboard
            </Link>
          </div>
        </div>
      )}

      {/* 2. Order Details Section */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 shadow-sm space-y-6">
        <h2 className="text-base font-bold text-on-surface border-b border-outline-variant/50 pb-2">Order Line Details</h2>

        <OrderLineTable items={order.items} status={order.status} />

        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 pt-4 border-t border-outline-variant/30 text-xs">
          {/* Dest Address */}
          <div>
            <p className="font-semibold text-secondary uppercase tracking-wider mb-1">Shipping Destination</p>
            <p className="font-bold text-on-surface">{order.shippingAddress.name}</p>
            <p className="text-on-surface-variant mt-0.5">
              {order.shippingAddress.street}, {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.zip}
            </p>
          </div>

          {/* Pricing summary */}
          <div className="w-full sm:w-64 space-y-2 text-right">
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Items Subtotal:</span>
              <span className="font-medium">{formatCurrency(order.totalAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Delivery:</span>
              <span className="text-emerald-500 font-semibold">Free Cargo</span>
            </div>
            <div className="flex justify-between border-t border-outline-variant/30 pt-2 font-bold text-sm text-on-surface">
              <span>Total Price:</span>
              <span>{formatCurrency(order.totalAmount)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
