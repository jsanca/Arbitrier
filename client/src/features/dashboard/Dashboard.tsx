import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import type { Order, Company, User } from '../../models/types';
import { services } from '../../services/mockServices';
import { CreditSummary } from '../../components/CreditSummary';
import { StatusBadge } from '../../components/StatusBadge';
import { LoadingState } from '../../components/LoadingState';

export const Dashboard: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [activeOrders, setActiveOrders] = useState<Order[]>([]);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [draftOrders, setDraftOrders] = useState<Order[]>([]);
  const [alerts, setAlerts] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        const currentUser = await services.auth.getCurrentUser();
        if (!currentUser) {
          navigate('/login');
          return;
        }
        setUser(currentUser);

        const currentCompany = await services.company.getCompany(currentUser.companyId);
        setCompany(currentCompany);

        if (currentCompany) {
          const active = await services.dashboard.getActiveOrders(currentCompany.id);
          const recent = await services.dashboard.getRecentOrders(currentCompany.id);
          const alertItems = await services.dashboard.getAlerts(currentCompany.id);

          // Get drafts
          const allOrders = await services.order.getOrders(currentCompany.id);
          const drafts = allOrders.filter(o => o.status === 'DRAFT');

          setActiveOrders(active);
          setRecentOrders(recent.filter(o => o.status !== 'DRAFT'));
          setDraftOrders(drafts);
          setAlerts(alertItems);
        }
      } catch (err) {
        console.error('Failed to load dashboard', err);
      } finally {
        setLoading(false);
      }
    };

    loadDashboardData();
  }, [navigate]);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  if (loading) return <LoadingState message="Assembling your company dashboard..." />;
  if (!user || !company) return null;

  return (
    <div className="space-y-8" data-testid="dashboard-page">
      {/* Welcome Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Company Dashboard</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Overview of corporate account activity for <span className="font-semibold">{company.name}</span>.
          </p>
        </div>
        <div className="flex gap-3">
          <Link
            to="/orders/new"
            className="h-11 px-5 bg-primary text-on-primary hover:bg-primary-container rounded-xl flex items-center justify-center gap-2 text-sm font-semibold transition-all shadow-sm"
          >
            <span className="material-symbols-outlined text-[20px]">add_shopping_cart</span>
            <span>Create New Order</span>
          </Link>
        </div>
      </div>

      {/* Credit Summary Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <CreditSummary availableCredit={company.availableCredit} creditLimit={company.creditLimit} />
        </div>

        {/* Alerts & Attention Panel */}
        <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm flex flex-col">
          <h3 className="text-xs font-semibold text-secondary uppercase tracking-wider mb-4 flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[16px] text-primary">notifications</span>
            Action Items
          </h3>
          {alerts.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
              <span className="material-symbols-outlined text-[32px] text-emerald-500 mb-2">check_circle</span>
              <p className="text-xs font-semibold text-on-surface">All system operations nominal</p>
              <p className="text-[11px] text-on-surface-variant mt-0.5">No immediate items require your review.</p>
            </div>
          ) : (
            <div className="space-y-3 flex-1 overflow-y-auto max-h-48">
              {alerts.map((alert, idx) => {
                const isCritical = alert.includes('Critical') || alert.includes('exceeds');
                return (
                  <div
                    key={idx}
                    onClick={() => alert.includes('awaiting') && navigate('/orders')}
                    className={`p-3 rounded-lg border text-xs flex gap-2.5 transition-all ${
                      alert.includes('awaiting') ? 'cursor-pointer hover:bg-surface-container-low' : ''
                    } ${
                      isCritical
                        ? 'bg-error-container/20 border-error text-error'
                        : 'bg-amber-50 dark:bg-amber-950/20 border-amber-300 dark:border-amber-900 text-amber-800 dark:text-amber-200'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px] mt-0.5">
                      {isCritical ? 'report' : 'notification_important'}
                    </span>
                    <span>{alert}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Action Required: Awaiting Customer Decision Orders */}
      {activeOrders.some(o => o.status === 'AWAITING_CUSTOMER_DECISION') && (
        <div className="bg-amber-50 dark:bg-amber-950/10 border border-amber-300 dark:border-amber-900 p-6 rounded-xl shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <span className="material-symbols-outlined text-amber-600 text-[24px]">gavel</span>
            <h3 className="text-base font-bold text-amber-800 dark:text-amber-300">Orders Awaiting Stock Acceptance</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-xs font-semibold text-amber-700 dark:text-amber-400 uppercase tracking-wider border-b border-amber-200 dark:border-amber-900">
                  <th className="py-2.5">Order Ref</th>
                  <th className="py-2.5">Submittal Date</th>
                  <th className="py-2.5">Requested Value</th>
                  <th className="py-2.5">Items Fulfillable</th>
                  <th className="py-2.5 text-right">Review Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-amber-200/50 dark:divide-amber-950/30">
                {activeOrders
                  .filter(o => o.status === 'AWAITING_CUSTOMER_DECISION')
                  .map(order => {
                    const totalItems = order.items.length;
                    const fullStockItems = order.items.filter(i => i.availableQuantity >= i.requestedQuantity).length;

                    return (
                      <tr key={order.id} className="hover:bg-amber-100/30 dark:hover:bg-amber-950/20">
                        <td className="py-3 font-semibold text-amber-950 dark:text-amber-100">{order.referenceId}</td>
                        <td className="py-3 text-amber-900 dark:text-amber-200">{new Date(order.createdAt).toLocaleDateString()}</td>
                        <td className="py-3 text-amber-900 dark:text-amber-200">{formatCurrency(order.totalAmount)}</td>
                        <td className="py-3 text-amber-900 dark:text-amber-200">
                          {fullStockItems} of {totalItems} items fully in stock
                        </td>
                        <td className="py-3 text-right">
                          <Link
                            to={`/orders/review?orderId=${order.id}`}
                            className="inline-flex h-8 items-center px-4 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-bold transition-all shadow-sm"
                          >
                            Resolve Stock Proposal
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Main Grid: Active Orders & Drafts */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        {/* Active Orders List */}
        <div className="xl:col-span-2 space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold font-headline-md text-on-surface">Active Procurement Orders</h3>
            <Link to="/orders" className="text-xs font-semibold text-primary hover:underline">
              View All Orders
            </Link>
          </div>

          <div className="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
            {activeOrders.filter(o => o.status !== 'AWAITING_CUSTOMER_DECISION').length === 0 ? (
              <div className="p-8 text-center text-sm text-secondary">
                No active processing orders right now. Click "Create New Order" to start.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="bg-surface-container text-xs font-semibold text-secondary uppercase border-b border-outline-variant">
                      <th className="px-6 py-4">Order Ref</th>
                      <th className="px-6 py-4">Submitted Date</th>
                      <th className="px-6 py-4">Total Amount</th>
                      <th className="px-6 py-4">Processing Step</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant">
                    {activeOrders
                      .filter(o => o.status !== 'AWAITING_CUSTOMER_DECISION')
                      .map(order => (
                        <tr
                          key={order.id}
                          className="hover:bg-surface-container-low transition-colors cursor-pointer"
                          onClick={() => navigate(`/orders/${order.id}`)}
                        >
                          <td className="px-6 py-4 font-semibold text-primary">
                            {order.referenceId || 'Initiating...'}
                          </td>
                          <td className="px-6 py-4 text-on-surface-variant">
                            {new Date(order.createdAt).toLocaleDateString()}
                          </td>
                          <td className="px-6 py-4 font-medium">{formatCurrency(order.totalAmount)}</td>
                          <td className="px-6 py-4">
                            <StatusBadge status={order.status} />
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Drafts Side-Panel */}
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-bold font-headline-md text-on-surface">Saved Drafts</h3>
            {draftOrders.length > 0 && (
              <span className="text-xs bg-surface-container-high text-secondary px-2 py-0.5 rounded font-bold">
                {draftOrders.length}
              </span>
            )}
          </div>

          <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 shadow-sm space-y-3">
            {draftOrders.length === 0 ? (
              <div className="p-6 text-center text-xs text-secondary">
                No draft shopping carts saved.
              </div>
            ) : (
              draftOrders.map(draft => (
                <div
                  key={draft.id}
                  className="p-3.5 border border-outline-variant rounded-lg hover:border-primary transition-all flex flex-col justify-between cursor-pointer"
                  onClick={() => navigate('/orders/new')}
                >
                  <div className="flex justify-between items-start gap-2 mb-2">
                    <div>
                      <p className="text-xs font-semibold truncate max-w-[160px] text-on-surface">
                        {draft.items.map(i => i.productName).join(', ') || 'Empty Cart'}
                      </p>
                      <p className="text-[10px] text-on-surface-variant/70 mt-0.5">
                        Edited: {new Date(draft.updatedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <span className="text-xs font-bold text-primary">{formatCurrency(draft.totalAmount)}</span>
                  </div>
                  <div className="flex justify-between items-center mt-1 border-t border-outline-variant/30 pt-2 text-[11px] text-primary font-semibold">
                    <span>{draft.items.length} item(s)</span>
                    <span className="flex items-center gap-0.5">
                      Resume <span className="material-symbols-outlined text-[12px]">arrow_forward</span>
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Recent Activity Table */}
      <div className="space-y-4">
        <h3 className="text-lg font-bold font-headline-md text-on-surface">Recent Order Outcomes</h3>
        <div className="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
          {recentOrders.length === 0 ? (
            <div className="p-8 text-center text-sm text-secondary">
              No historical orders to display.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-surface-container text-xs font-semibold text-secondary uppercase border-b border-outline-variant">
                    <th className="px-6 py-4">Order Ref</th>
                    <th className="px-6 py-4">Submission Date</th>
                    <th className="px-6 py-4">Final Value</th>
                    <th className="px-6 py-4">Outcome</th>
                    <th className="px-6 py-4 text-right">Invoice</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant">
                  {recentOrders.map(order => (
                    <tr
                      key={order.id}
                      className="hover:bg-surface-container-low transition-colors cursor-pointer animate-fade-in"
                      onClick={() => navigate(`/orders/${order.id}`)}
                    >
                      <td className="px-6 py-4 font-semibold text-primary">{order.referenceId}</td>
                      <td className="px-6 py-4 text-on-surface-variant">
                        {new Date(order.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 font-medium">{formatCurrency(order.totalAmount)}</td>
                      <td className="px-6 py-4">
                        <StatusBadge status={order.status} />
                      </td>
                      <td className="px-6 py-4 text-right" onClick={e => e.stopPropagation()}>
                        {order.status === 'CONFIRMED' || order.status === 'PARTIALLY_CONFIRMED' ? (
                          <button
                            onClick={() => alert('Feature mock: Downloading institutional Invoice PDF...')}
                            className="text-xs text-primary hover:underline font-semibold flex items-center justify-end gap-1 ml-auto"
                          >
                            <span className="material-symbols-outlined text-[14px]">download</span>
                            <span>Invoice</span>
                          </button>
                        ) : (
                          <span className="text-[11px] text-on-surface-variant/40">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
