import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Product, Order, OrderItem, Company, User } from '../../models/types';
import { services } from '../../services/mockServices';
import { LoadingState } from '../../components/LoadingState';

export const NewOrder: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [draftOrder, setDraftOrder] = useState<Order | null>(null);
  const [selectedAddressId, setSelectedAddressId] = useState('');
  const [deliveryOption, setDeliveryOption] = useState('STANDARD');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const initializePage = async () => {
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
          // Get products
          const items = await services.products.getAllProducts();
          setProducts(items);

          // Get or create draft
          const draft = await services.preparation.getDraftOrder(currentCompany.id, currentUser.id);
          setDraftOrder(draft);

          if (draft.shippingAddress) {
            setSelectedAddressId(draft.shippingAddress.id);
          } else if (currentCompany.shippingAddresses.length > 0) {
            setSelectedAddressId(currentCompany.shippingAddresses[0].id);
          }
        }
      } catch (err) {
        console.error('Failed to initialize new order page', err);
      } finally {
        setLoading(false);
      }
    };

    initializePage();
  }, [navigate]);

  const handleProductSearch = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setSearchQuery(val);
    if (company) {
      const filtered = await services.products.searchProducts(val);
      setProducts(filtered);
    }
  };

  const handleAddToCart = async (product: Product) => {
    if (!draftOrder || !company) return;

    const existingItemIdx = draftOrder.items.findIndex(item => item.productId === product.id);
    let updatedItems = [...draftOrder.items];

    if (existingItemIdx !== -1) {
      updatedItems[existingItemIdx].requestedQuantity += 1;
    } else {
      updatedItems.push({
        productId: product.id,
        productName: product.name,
        sku: product.sku,
        price: product.price,
        requestedQuantity: 1,
        availableQuantity: 1,
        acceptedQuantity: 1
      });
    }

    const updatedDraft = {
      ...draftOrder,
      items: updatedItems,
      totalAmount: updatedItems.reduce((sum, item) => sum + item.price * item.requestedQuantity, 0)
    };

    setDraftOrder(updatedDraft);
    await services.preparation.saveDraftOrder(updatedDraft);
  };

  const handleQuantityChange = async (productId: string, change: number) => {
    if (!draftOrder) return;

    const updatedItems = draftOrder.items
      .map(item => {
        if (item.productId === productId) {
          const newQty = item.requestedQuantity + change;
          return newQty > 0 ? { ...item, requestedQuantity: newQty } : null;
        }
        return item;
      })
      .filter((item): item is OrderItem => item !== null);

    const updatedDraft = {
      ...draftOrder,
      items: updatedItems,
      totalAmount: updatedItems.reduce((sum, item) => sum + item.price * item.requestedQuantity, 0)
    };

    setDraftOrder(updatedDraft);
    await services.preparation.saveDraftOrder(updatedDraft);
  };

  const handleRemoveItem = async (productId: string) => {
    if (!draftOrder) return;

    const updatedItems = draftOrder.items.filter(item => item.productId !== productId);
    const updatedDraft = {
      ...draftOrder,
      items: updatedItems,
      totalAmount: updatedItems.reduce((sum, item) => sum + item.price * item.requestedQuantity, 0)
    };

    setDraftOrder(updatedDraft);
    await services.preparation.saveDraftOrder(updatedDraft);
  };

  const handleSaveDraft = async () => {
    if (!draftOrder || !company) return;

    const selectedAddr = company.shippingAddresses.find(a => a.id === selectedAddressId) || company.shippingAddresses[0];
    const updatedDraft = {
      ...draftOrder,
      shippingAddress: selectedAddr,
      updatedAt: new Date().toISOString()
    };

    await services.preparation.saveDraftOrder(updatedDraft);
    alert('Draft procurement order saved successfully!');
    navigate('/dashboard');
  };

  const handleReviewCheckout = async () => {
    if (!draftOrder || draftOrder.items.length === 0 || !company) return;

    const selectedAddr = company.shippingAddresses.find(a => a.id === selectedAddressId) || company.shippingAddresses[0];
    const finalDraft = {
      ...draftOrder,
      shippingAddress: selectedAddr
    };

    await services.preparation.saveDraftOrder(finalDraft);
    // Proceed directly to availability review screen
    navigate(`/orders/review?orderId=${finalDraft.id}&delivery=${deliveryOption}`);
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  };

  if (loading) return <LoadingState message="Opening ordering workspace..." />;
  if (!user || !company || !draftOrder) return null;

  return (
    <div className="space-y-8" data-testid="new-order-page">
      <div>
        <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Create New Bulk Purchase</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          Draft items from the product catalog and configure delivery details.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left/Middle: Catalog Search & Cart */}
        <div className="lg:col-span-2 space-y-6">
          {/* Catalog Search */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm">
            <h3 className="text-sm font-bold font-headline-md mb-3">Product Catalog</h3>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-secondary">
                search
              </span>
              <input
                type="text"
                value={searchQuery}
                onChange={handleProductSearch}
                placeholder="Search products by name, category, or SKU..."
                className="w-full h-11 pl-10 pr-4 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl outline-none text-sm bg-surface-container-low"
              />
            </div>

            {/* Catalog Items */}
            <div className="mt-4 divide-y divide-outline-variant max-h-96 overflow-y-auto">
              {products.length === 0 ? (
                <p className="p-4 text-center text-xs text-secondary">No matching products in catalog.</p>
              ) : (
                products.map(product => {
                  const existingItem = draftOrder.items.find(i => i.productId === product.id);
                  return (
                    <div key={product.id} className="py-3 flex justify-between items-center gap-4 hover:bg-surface-container-low/30 px-2 rounded-lg transition-colors">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-on-surface">{product.name}</p>
                        <p className="text-[11px] text-secondary mt-0.5">
                          SKU: <span className="font-mono">{product.sku}</span> • Category: {product.category}
                        </p>
                      </div>
                      <div className="flex items-center gap-4 shrink-0">
                        <span className="text-sm font-bold text-on-surface">{formatCurrency(product.price)}</span>
                        <button
                          onClick={() => handleAddToCart(product)}
                          className="h-8 px-3 bg-surface-container-high hover:bg-primary hover:text-on-primary rounded-lg text-xs font-bold transition-all flex items-center gap-1"
                        >
                          <span className="material-symbols-outlined text-[14px]">add</span>
                          <span>{existingItem ? `Add (${existingItem.requestedQuantity})` : 'Add'}</span>
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Cart / Draft Items List */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-sm font-bold font-headline-md">Draft Item List</h3>
              <span className="text-xs bg-surface-container-high px-2 py-0.5 rounded font-semibold text-secondary">
                {draftOrder.items.length} unique products
              </span>
            </div>

            {draftOrder.items.length === 0 ? (
              <div className="p-12 text-center border border-dashed border-outline-variant rounded-lg">
                <span className="material-symbols-outlined text-[36px] text-secondary opacity-40 mb-2">
                  shopping_cart
                </span>
                <p className="text-xs text-on-surface-variant font-medium">Your procurement cart is empty.</p>
                <p className="text-[11px] text-secondary mt-0.5">Use the product catalog search above to add items.</p>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="divide-y divide-outline-variant">
                  {draftOrder.items.map(item => (
                    <div key={item.productId} className="py-4 flex justify-between items-center gap-4">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-on-surface">{item.productName}</p>
                        <p className="text-[11px] text-secondary mt-0.5">
                          SKU: <span className="font-mono">{item.sku}</span> • {formatCurrency(item.price)} each
                        </p>
                      </div>
                      <div className="flex items-center gap-4 shrink-0">
                        {/* Quantity controls */}
                        <div className="flex items-center border border-outline-variant rounded-lg overflow-hidden bg-surface-container-low h-9">
                          <button
                            onClick={() => handleQuantityChange(item.productId, -1)}
                            className="w-8 h-full flex items-center justify-center hover:bg-surface-container-high transition-colors"
                            aria-label="Decrease quantity"
                          >
                            <span className="material-symbols-outlined text-[14px]">remove</span>
                          </button>
                          <span className="px-3 font-semibold text-xs text-on-surface w-10 text-center select-none">
                            {item.requestedQuantity}
                          </span>
                          <button
                            onClick={() => handleQuantityChange(item.productId, 1)}
                            className="w-8 h-full flex items-center justify-center hover:bg-surface-container-high transition-colors"
                            aria-label="Increase quantity"
                          >
                            <span className="material-symbols-outlined text-[14px]">add</span>
                          </button>
                        </div>

                        {/* Total per line */}
                        <span className="text-sm font-bold text-on-surface w-20 text-right">
                          {formatCurrency(item.price * item.requestedQuantity)}
                        </span>

                        {/* Remove button */}
                        <button
                          onClick={() => handleRemoveItem(item.productId)}
                          className="w-8 h-8 text-on-surface-variant hover:text-error hover:bg-red-50 dark:hover:bg-red-950/20 rounded-lg flex items-center justify-center transition-all"
                          aria-label="Remove item"
                        >
                          <span className="material-symbols-outlined text-[18px]">delete</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right side: Delivery details & Summary */}
        <div className="space-y-6">
          {/* Settings Panel */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2">Delivery Settings</h3>

            {/* Delivery address */}
            <div>
              <label className="block text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-1.5">
                Shipping Destination
              </label>
              <select
                value={selectedAddressId}
                onChange={e => setSelectedAddressId(e.target.value)}
                className="w-full h-11 px-3 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl text-xs bg-surface-container-low outline-none"
              >
                {company.shippingAddresses.map(addr => (
                  <option key={addr.id} value={addr.id}>
                    {addr.name} ({addr.city}, {addr.state})
                  </option>
                ))}
              </select>
              {selectedAddressId && (
                <div className="mt-2 p-2.5 bg-surface-container-low border border-outline-variant/50 rounded-lg text-[11px] text-on-surface-variant/90 space-y-0.5">
                  <p className="font-semibold text-on-surface">
                    {company.shippingAddresses.find(a => a.id === selectedAddressId)?.street}
                  </p>
                  <p>
                    {company.shippingAddresses.find(a => a.id === selectedAddressId)?.city},{' '}
                    {company.shippingAddresses.find(a => a.id === selectedAddressId)?.state}{' '}
                    {company.shippingAddresses.find(a => a.id === selectedAddressId)?.zip}
                  </p>
                </div>
              )}
            </div>

            {/* Shipping service level */}
            <div>
              <label className="block text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-1.5">
                Delivery Schedule
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setDeliveryOption('STANDARD')}
                  className={`py-2 px-3 border text-xs font-semibold rounded-lg flex flex-col items-center gap-0.5 transition-all ${
                    deliveryOption === 'STANDARD'
                      ? 'border-primary bg-primary-fixed text-on-primary-fixed'
                      : 'border-outline-variant hover:bg-surface-container-low text-on-surface-variant'
                  }`}
                >
                  <span>Standard Freight</span>
                  <span className="text-[10px] font-normal opacity-70">3-5 Business Days</span>
                </button>
                <button
                  type="button"
                  onClick={() => setDeliveryOption('EXPEDITED')}
                  className={`py-2 px-3 border text-xs font-semibold rounded-lg flex flex-col items-center gap-0.5 transition-all ${
                    deliveryOption === 'EXPEDITED'
                      ? 'border-primary bg-primary-fixed text-on-primary-fixed'
                      : 'border-outline-variant hover:bg-surface-container-low text-on-surface-variant'
                  }`}
                >
                  <span>Priority Expedited</span>
                  <span className="text-[10px] font-normal opacity-70">Next-Day Cargo</span>
                </button>
              </div>
            </div>
          </div>

          {/* Pricing summary */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2">Order Summary</h3>

            <div className="space-y-2 text-xs">
              <div className="flex justify-between text-on-surface-variant">
                <span>Items Subtotal</span>
                <span>{formatCurrency(draftOrder.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-on-surface-variant">
                <span>Estimated Freight Charges</span>
                <span className="text-emerald-500 font-semibold">Free B2B Freight</span>
              </div>
              <div className="flex justify-between text-on-surface-variant">
                <span>Taxes & Duties</span>
                <span>$0.00 (Exempt)</span>
              </div>
              <div className="border-t border-outline-variant/30 pt-2 flex justify-between font-bold text-sm text-on-surface">
                <span>Total Amount</span>
                <span>{formatCurrency(draftOrder.totalAmount)}</span>
              </div>
            </div>

            <div className="pt-2 grid grid-cols-1 gap-2">
              <button
                onClick={handleReviewCheckout}
                disabled={draftOrder.items.length === 0}
                className="w-full h-11 bg-primary text-on-primary font-semibold rounded-xl hover:opacity-95 transition-all flex items-center justify-center gap-1.5 shadow-sm disabled:opacity-40"
              >
                <span className="material-symbols-outlined text-[20px]">assignment_turned_in</span>
                <span>Review Order Availability</span>
              </button>
              <button
                onClick={handleSaveDraft}
                className="w-full h-11 border border-outline-variant hover:bg-surface-container-low text-on-surface-variant font-semibold rounded-xl transition-all flex items-center justify-center gap-1.5"
              >
                <span className="material-symbols-outlined text-[20px]">save</span>
                <span>Save Draft & Exit</span>
              </button>
            </div>

            {company.availableCredit < draftOrder.totalAmount && (
              <div className="p-3 bg-error-container/20 border border-error-container text-xs text-error rounded-lg flex gap-2">
                <span className="material-symbols-outlined text-[16px] shrink-0 mt-0.5">report</span>
                <span>
                  Warning: Order total exceeds available credit ({formatCurrency(company.availableCredit)}). 
                  Checking out may result in credit validation rejection.
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
