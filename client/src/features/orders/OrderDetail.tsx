import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import type { Order, Company } from '../../models/types';
import { services } from '../../services/mockServices';
import { OrderLineTable } from '../../components/OrderLineTable';
import { BusinessTimeline } from '../../components/BusinessTimeline';
import { LoadingState } from '../../components/LoadingState';
import { StatusBadge } from '../../components/StatusBadge';

export const OrderDetail: React.FC = () => {
  const { orderId = '' } = useParams<{ orderId: string }>();
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadOrderDetails = async () => {
      try {
        const item = await services.order.getOrderById(orderId);
        if (!item) {
          navigate('/orders');
          return;
        }
        setOrder(item);

        const currentCompany = await services.company.getCompany(item.companyId);
        setCompany(currentCompany);
      } catch (err) {
        console.error('Failed to load order details', err);
      } finally {
        setLoading(false);
      }
    };

    loadOrderDetails();
  }, [orderId, navigate]);

  const handleDownloadInvoice = () => {
    alert('Feature mock: Downloading institutional Invoice PDF for this order...');
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  if (loading) return <LoadingState message="Fetching order procurement tracking logs..." />;
  if (!order || !company) return null;

  const showInvoiceButton = ['CONFIRMED', 'PARTIALLY_CONFIRMED'].includes(order.status);
  const isAwaitingAction = order.status === 'AWAITING_CUSTOMER_DECISION';

  return (
    <div className="space-y-8" data-testid="order-detail-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-outline-variant/60 pb-6">
        <div>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Order {order.referenceId || 'Initiating'}</h1>
            <StatusBadge status={order.status} />
          </div>
          <p className="text-xs text-on-surface-variant mt-1.5">
            Internal ID: <span className="font-mono">{order.id}</span> • Submitted: {new Date(order.createdAt).toLocaleString()}
          </p>
        </div>

        <div className="flex gap-2">
          {isAwaitingAction && (
            <Link
              to={`/orders/review?orderId=${order.id}`}
              className="h-10 px-5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[16px]">gavel</span>
              <span>Resolve Stock Allocation</span>
            </Link>
          )}

          {showInvoiceButton && (
            <button
              onClick={handleDownloadInvoice}
              className="h-10 px-4 bg-primary text-on-primary hover:bg-primary-container rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px]">download</span>
              <span>Download Invoice</span>
            </button>
          )}

          <Link
            to="/orders"
            className="h-10 px-4 border border-outline-variant hover:bg-surface-container-low text-on-surface-variant text-xs font-semibold rounded-lg flex items-center justify-center transition-all"
          >
            Back to List
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left/Middle: Details & Timeline */}
        <div className="lg:col-span-2 space-y-6">
          {/* Timeline */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm">
            <h3 className="text-sm font-bold font-headline-md mb-6 border-b border-outline-variant/30 pb-3 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px] text-primary">analytics</span>
              Procurement Timeline
            </h3>
            <BusinessTimeline timeline={order.timeline} />
          </div>

          {/* Line items table */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant/30 pb-3">Itemized Breakdown</h3>
            <OrderLineTable items={order.items} status={order.status} />

            <div className="flex justify-between items-center pt-2 text-xs font-semibold">
              <span className="text-secondary">Summary Total:</span>
              <span className="text-base font-bold text-on-surface">{formatCurrency(order.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Right side: Destinations & Action Placeholders */}
        <div className="space-y-6">
          {/* Shipping/Billing addresses */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2">Logistics & Billing</h3>

            <div>
              <p className="text-[10px] font-semibold text-secondary uppercase tracking-wider mb-1">Shipping Destination</p>
              <p className="text-xs font-semibold text-on-surface">{order.shippingAddress.name}</p>
              <p className="text-[11px] text-on-surface-variant mt-0.5">
                {order.shippingAddress.street}, {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.zip}
              </p>
            </div>

            <div className="border-t border-outline-variant/30 pt-3">
              <p className="text-[10px] font-semibold text-secondary uppercase tracking-wider mb-1">Billing Account Profile</p>
              <p className="text-xs font-semibold text-on-surface">{company.billingContact.name}</p>
              <p className="text-[11px] text-on-surface-variant mt-0.5">
                Email: {company.billingContact.email}
              </p>
              <p className="text-[11px] text-on-surface-variant">
                Phone: {company.billingContact.phone}
              </p>
            </div>

            <div className="border-t border-outline-variant/30 pt-3">
              <p className="text-[10px] font-semibold text-secondary uppercase tracking-wider mb-1">Payment Terms</p>
              <p className="text-xs font-semibold text-on-surface">{company.paymentTerms}</p>
            </div>
          </div>

          {/* Customer support help */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md">Assistance & Support</h3>
            <p className="text-xs text-on-surface-variant">
              Need to modify or cancel this procurement order? Get in touch with our institutional support desk.
            </p>
            <button
              onClick={() => alert('Support tickets can be submitted to: support@arbitrier.com. Please mention your reference ID.')}
              className="w-full h-10 border border-primary hover:bg-primary-fixed text-primary font-semibold rounded-lg text-xs transition-all flex items-center justify-center gap-1.5"
            >
              <span className="material-symbols-outlined text-[16px]">support_agent</span>
              <span>Contact Support Desk</span>
            </button>
          </div>

          {/* Placeholders for future features */}
          <div className="p-4 bg-surface-container-low border border-outline-variant/50 rounded-xl space-y-2.5">
            <h4 className="text-[10px] font-bold text-secondary uppercase tracking-wider">Future Security Features</h4>
            <div className="space-y-1.5 text-[10px] text-on-surface-variant/80">
              <p className="flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">lock</span>
                <span>Manifest Verification (Future Integration)</span>
              </p>
              <p className="flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">verified</span>
                <span>Cryptographic Chain-of-Custody (TBD)</span>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
