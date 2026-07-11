import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Order, OrderItem, Company, User } from '../../models/types';
import { services } from '../../services/mockServices';
import { LoadingState } from '../../components/LoadingState';

export const AvailabilityReview: React.FC = () => {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId') || '';

  const [user, setUser] = useState<User | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [itemsState, setItemsState] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const loadOrderData = async () => {
      if (!orderId) {
        navigate('/orders/new');
        return;
      }
      try {
        const currentUser = await services.auth.getCurrentUser();
        if (!currentUser) {
          navigate('/login');
          return;
        }
        setUser(currentUser);

        const currentCompany = await services.company.getCompany(currentUser.companyId);
        setCompany(currentCompany);

        const currentOrder = await services.order.getOrderById(orderId);
        if (!currentOrder) {
          navigate('/dashboard');
          return;
        }

        setOrder(currentOrder);

        // Pre-saga stock check mockup simulation:
        // Inject mock stock logic: If customer orders product-3 (Pump) in quantity > 2, it is partially available (only 2 in stock).
        const simulatedItems = currentOrder.items.map(item => {
          let available = item.requestedQuantity;
          if (item.productId === 'prod-3' && item.requestedQuantity > 2) {
            available = 2; // only 2 available
          } else if (item.productId === 'prod-5' && item.requestedQuantity > 10) {
            available = 0; // completely unavailable
          }
          return {
            ...item,
            availableQuantity: available,
            acceptedQuantity: item.requestedQuantity // Default to requested quantity so customer decides how to resolve partials
          };
        });

        setItemsState(simulatedItems);
      } catch (err) {
        console.error('Failed to load availability review', err);
      } finally {
        setLoading(false);
      }
    };

    loadOrderData();
  }, [orderId, navigate]);

  const handleAcceptAvailable = (productId: string) => {
    setItemsState(prev =>
      prev.map(item => {
        if (item.productId === productId) {
          return { ...item, acceptedQuantity: item.availableQuantity };
        }
        return item;
      })
    );
  };

  const handleUpdateQuantity = (productId: string, newQty: number) => {
    if (newQty < 0) return;
    setItemsState(prev =>
      prev.map(item => {
        if (item.productId === productId) {
          return { ...item, acceptedQuantity: newQty };
        }
        return item;
      })
    );
  };

  const handleRemoveItem = (productId: string) => {
    setItemsState(prev => prev.filter(item => item.productId !== productId));
  };

  const handleConfirmSubmission = async () => {
    if (!order || itemsState.length === 0) return;

    // Save final accepted items and values back to the order
    const updatedOrder = {
      ...order,
      items: itemsState,
      totalAmount: itemsState.reduce((sum, item) => sum + item.price * item.acceptedQuantity, 0)
    };

    // Save before submitting
    await services.preparation.saveDraftOrder(updatedOrder);

    // Redirect to the simulated submitting order loader page
    navigate(`/orders/submitting?orderId=${updatedOrder.id}`);
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  if (loading) return <LoadingState message="Checking global warehouse availability matrices..." />;
  if (!user || !company || !order) return null;

  const totalAcceptedAmount = itemsState.reduce((sum, item) => sum + item.price * item.acceptedQuantity, 0);

  return (
    <div className="space-y-6" data-testid="availability-review-page">
      <div>
        <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Availability Review</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          Review stock status of your requested items. Resolving availability warnings now speeds up delivery processing.
        </p>
      </div>

      {/* Advisory Warning */}
      <div className="p-4 bg-amber-50 dark:bg-amber-950/20 border border-amber-300 dark:border-amber-900 rounded-xl flex gap-3 text-xs text-amber-800 dark:text-amber-300">
        <span className="material-symbols-outlined text-[20px] shrink-0">info</span>
        <div>
          <span className="font-bold">Advisory Notice:</span> Stock allocations are estimated in real-time and remain provisional. Stock is only reserved once order submittal completes successfully.
        </div>
      </div>

      {/* Table grid */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-surface-container text-xs font-semibold text-secondary uppercase border-b border-outline-variant">
                <th className="px-6 py-4">Product</th>
                <th className="px-6 py-4 text-center">Requested</th>
                <th className="px-6 py-4 text-center">Available Now</th>
                <th className="px-6 py-4">Stock Status</th>
                <th className="px-6 py-4 text-right">Actions / Quantity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {itemsState.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-6 text-center text-secondary">
                    All items removed from review.
                  </td>
                </tr>
              ) : (
                itemsState.map(item => {
                  const isFullyAvailable = item.availableQuantity >= item.requestedQuantity;
                  const isPartiallyAvailable = item.availableQuantity > 0 && item.availableQuantity < item.requestedQuantity;
                  const isUnavailable = item.availableQuantity === 0;

                  return (
                    <tr key={item.productId} className="hover:bg-surface-container-low/30 transition-colors">
                      <td className="px-6 py-4">
                        <p className="font-medium text-on-surface">{item.productName}</p>
                        <p className="text-[10px] text-secondary mt-0.5">SKU: {item.sku}</p>
                      </td>
                      <td className="px-6 py-4 text-center font-medium">{item.requestedQuantity}</td>
                      <td className="px-6 py-4 text-center font-bold">
                        {isUnavailable ? (
                          <span className="text-error">0</span>
                        ) : (
                          <span className={isFullyAvailable ? 'text-emerald-600' : 'text-amber-600'}>
                            {item.availableQuantity}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {isFullyAvailable && (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 bg-emerald-50 dark:bg-emerald-950/20 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Fully Available
                          </span>
                        )}
                        {isPartiallyAvailable && (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-amber-700 bg-amber-50 dark:bg-amber-950/20 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span> Partially Available
                          </span>
                        )}
                        {isUnavailable && (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-error bg-red-50 dark:bg-red-950/20 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-error"></span> Out of Stock
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-right">
                        {isFullyAvailable ? (
                          <span className="text-xs text-secondary font-medium">Auto-confirmed</span>
                        ) : (
                          <div className="flex items-center justify-end gap-2">
                            {/* Accept Available */}
                            {item.availableQuantity > 0 && item.acceptedQuantity !== item.availableQuantity && (
                              <button
                                onClick={() => handleAcceptAvailable(item.productId)}
                                className="h-8 px-2.5 bg-amber-50 dark:bg-amber-950/30 hover:bg-amber-100 border border-amber-300 dark:border-amber-900 rounded-lg text-[11px] font-semibold text-amber-800 dark:text-amber-200 transition-all"
                              >
                                Accept Available ({item.availableQuantity})
                              </button>
                            )}

                            {/* Direct accepted quantity modification */}
                            <div className="flex items-center border border-outline-variant rounded-lg overflow-hidden bg-surface-container-low h-8">
                              <button
                                onClick={() => handleUpdateQuantity(item.productId, item.acceptedQuantity - 1)}
                                className="w-7 h-full flex items-center justify-center hover:bg-surface-container-high text-secondary"
                              >
                                -
                              </button>
                              <input
                                type="number"
                                value={item.acceptedQuantity}
                                onChange={e => handleUpdateQuantity(item.productId, parseInt(e.target.value) || 0)}
                                className="w-9 h-full bg-transparent text-center font-semibold text-xs text-on-surface focus:outline-none border-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                              />
                              <button
                                onClick={() => handleUpdateQuantity(item.productId, item.acceptedQuantity + 1)}
                                className="w-7 h-full flex items-center justify-center hover:bg-surface-container-high text-secondary"
                              >
                                +
                              </button>
                            </div>

                            {/* Remove Line */}
                            <button
                              onClick={() => handleRemoveItem(item.productId)}
                              className="h-8 w-8 text-on-surface-variant hover:text-error hover:bg-red-50 dark:hover:bg-red-950/20 rounded-lg flex items-center justify-center border border-outline-variant transition-all"
                              aria-label="Remove item"
                            >
                              <span className="material-symbols-outlined text-[16px]">close</span>
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Confirmation Actions */}
      <div className="flex flex-col sm:flex-row sm:justify-between items-center gap-4 bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm">
        <div>
          <p className="text-xs text-on-surface-variant">Adjusted Cart Estimate</p>
          <h3 className="text-xl font-bold text-primary mt-0.5">{formatCurrency(totalAcceptedAmount)}</h3>
        </div>
        <div className="flex gap-3 w-full sm:w-auto">
          <button
            onClick={() => navigate('/orders/new')}
            className="flex-1 sm:flex-initial h-11 px-5 border border-outline-variant hover:bg-surface-container-low text-on-surface-variant font-semibold rounded-xl transition-all text-sm"
          >
            Back to Edit Cart
          </button>
          <button
            onClick={handleConfirmSubmission}
            disabled={itemsState.length === 0}
            className="flex-1 sm:flex-initial h-11 px-6 bg-primary text-on-primary hover:opacity-95 font-semibold rounded-xl transition-all text-sm shadow-sm disabled:opacity-40"
          >
            Confirm & Submit Purchase
          </button>
        </div>
      </div>
    </div>
  );
};
