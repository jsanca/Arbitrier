import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Order, OrderStatus } from '../../models/types';
import { services } from '../../services/mockServices';
import { StatusBadge } from '../../components/StatusBadge';
import { LoadingState } from '../../components/LoadingState';
import { EmptyState } from '../../components/EmptyState';

export const OrdersList: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchOrdersData = async () => {
      setLoading(true);
      try {
        const currentUser = await services.auth.getCurrentUser();
        if (!currentUser) {
          navigate('/login');
          return;
        }

        const data = await services.order.getOrders(currentUser.companyId, statusFilter, searchQuery);
        setOrders(data);
      } catch (err) {
        console.error('Failed to get orders', err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrdersData();
  }, [statusFilter, searchQuery, navigate]);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  const filterTabs: { value: OrderStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All Orders' },
    { value: 'DRAFT', label: 'Drafts' },
    { value: 'AWAITING_CUSTOMER_DECISION', label: 'Action Required' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'PARTIALLY_CONFIRMED', label: 'Partial Confirmed' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'REJECTED', label: 'Rejected' }
  ];

  return (
    <div className="space-y-6" data-testid="orders-list-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Procurement Orders</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Browse corporate purchase history, drafts, and active execution pipelines.
          </p>
        </div>
        <button
          onClick={() => navigate('/orders/new')}
          className="h-10 px-4 bg-primary text-on-primary hover:bg-primary-container rounded-lg flex items-center justify-center gap-2 text-xs font-semibold transition-all shadow-sm self-start sm:self-auto"
        >
          <span className="material-symbols-outlined text-[16px]">add_shopping_cart</span>
          <span>New Order</span>
        </button>
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
          {/* Search Input */}
          <div className="relative flex-1 max-w-md">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-secondary">
              search
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search by Reference ID, SKU, or product name..."
              className="w-full h-10 pl-10 pr-4 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl outline-none text-xs bg-surface-container-low"
            />
          </div>

          {/* Filter Status select for smaller layouts */}
          <div className="block md:hidden">
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value as OrderStatus | 'ALL')}
              className="w-full h-10 px-3 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl text-xs bg-surface-container-low outline-none"
            >
              {filterTabs.map(tab => (
                <option key={tab.value} value={tab.value}>
                  {tab.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Tab row for desktop */}
        <div className="hidden md:flex flex-wrap border-b border-outline-variant pb-1 gap-1">
          {filterTabs.map(tab => {
            const isActive = statusFilter === tab.value;
            return (
              <button
                key={tab.value}
                onClick={() => setStatusFilter(tab.value)}
                className={`px-4 py-2 text-xs font-semibold rounded-t-lg transition-all border-b-2 ${
                  isActive
                    ? 'border-primary text-primary bg-primary-fixed/20'
                    : 'border-transparent text-secondary hover:text-primary hover:bg-surface-container-low/40'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Orders Table Area */}
      {loading ? (
        <LoadingState message="Loading procurement records..." />
      ) : orders.length === 0 ? (
        <EmptyState
          title="No orders match filters"
          description="We couldn't find any orders in your database matching the selected filters or search queries."
          actionText="Create New Order"
          onAction={() => navigate('/orders/new')}
        />
      ) : (
        <div className="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-surface-container text-xs font-semibold text-secondary uppercase border-b border-outline-variant">
                  <th className="px-6 py-4">Reference ID</th>
                  <th className="px-6 py-4">Submitted Date</th>
                  <th className="px-6 py-4">Item Details</th>
                  <th className="px-6 py-4 text-right">Order Value</th>
                  <th className="px-6 py-4">Execution Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {orders.map(order => (
                  <tr
                    key={order.id}
                    className="hover:bg-surface-container-low transition-colors cursor-pointer"
                    onClick={() => {
                      if (order.status === 'DRAFT') {
                        navigate('/orders/new');
                      } else {
                        navigate(`/orders/${order.id}`);
                      }
                    }}
                  >
                    <td className="px-6 py-4 font-semibold text-primary">
                      {order.status === 'DRAFT' ? (
                        <span className="text-secondary/70 italic">[Draft Cart]</span>
                      ) : (
                        order.referenceId || 'Pending Check'
                      )}
                    </td>
                    <td className="px-6 py-4 text-on-surface-variant">
                      {new Date(order.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4">
                      <p className="font-semibold text-xs text-on-surface truncate max-w-xs">
                        {order.items.map(i => i.productName).join(', ') || 'No items selected'}
                      </p>
                      <p className="text-[10px] text-secondary mt-0.5">
                        {order.items.length} line items total
                      </p>
                    </td>
                    <td className="px-6 py-4 text-right font-semibold text-on-surface">
                      {formatCurrency(order.totalAmount)}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={order.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
