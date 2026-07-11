import React from 'react';
import type { OrderItem, OrderStatus } from '../models/types';

interface OrderLineTableProps {
  items: OrderItem[];
  status: OrderStatus;
}

export const OrderLineTable: React.FC<OrderLineTableProps> = ({ items, status }) => {
  const showAccepted = ['PARTIALLY_CONFIRMED', 'AWAITING_CUSTOMER_DECISION', 'CANCELLED', 'CONFIRMED'].includes(status);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  return (
    <div className="overflow-x-auto border border-outline-variant rounded-xl" data-testid="order-line-table">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="bg-surface-container text-xs font-semibold text-secondary uppercase tracking-wider border-b border-outline-variant">
            <th className="px-6 py-4">Product</th>
            <th className="px-6 py-4">SKU</th>
            <th className="px-6 py-4 text-right">Price</th>
            <th className="px-6 py-4 text-center">Requested</th>
            {showAccepted && (
              <>
                <th className="px-6 py-4 text-center">Available</th>
                <th className="px-6 py-4 text-center">Accepted</th>
              </>
            )}
            <th className="px-6 py-4 text-right">Total</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-outline-variant bg-surface-container-lowest text-sm">
          {items.map((item, idx) => {
            const qty = showAccepted ? item.acceptedQuantity : item.requestedQuantity;
            const subtotal = item.price * qty;

            const isPartial = item.availableQuantity < item.requestedQuantity;

            return (
              <tr key={idx} className="hover:bg-surface-container-low transition-colors">
                <td className="px-6 py-4 font-medium text-on-surface">
                  {item.productName}
                  {showAccepted && isPartial && (
                    <span className="block text-[11px] text-amber-600 font-semibold mt-0.5">
                      Partial stock availability ({item.availableQuantity} of {item.requestedQuantity} in stock)
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 text-secondary font-mono text-xs">{item.sku}</td>
                <td className="px-6 py-4 text-right font-medium">{formatCurrency(item.price)}</td>
                <td className="px-6 py-4 text-center font-medium">{item.requestedQuantity}</td>
                {showAccepted && (
                  <>
                    <td className={`px-6 py-4 text-center font-medium ${isPartial ? 'text-amber-600' : ''}`}>
                      {item.availableQuantity}
                    </td>
                    <td className="px-6 py-4 text-center font-medium bg-surface-container-low">
                      {item.acceptedQuantity}
                    </td>
                  </>
                )}
                <td className="px-6 py-4 text-right font-semibold text-on-surface">
                  {formatCurrency(subtotal)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
