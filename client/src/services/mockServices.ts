import type {
  AuthService,
  CustomerDashboardService,
  ProductCatalogService,
  OrderPreparationService,
  OrderService,
  CompanyService
} from './interfaces';
import type { User, Company, Product, Order, OrderStatus, OrderTimelineEvent } from '../models/types';

import authData from '../mocks/auth.json';
import companyData from '../mocks/company.json';
import productsData from '../mocks/products.json';
import initialOrders from '../mocks/orders.json';

// Initialize LocalStorage Database if not present
const DB_KEYS = {
  USER: 'arbitrier_user',
  COMPANY: 'arbitrier_company',
  PRODUCTS: 'arbitrier_products',
  ORDERS: 'arbitrier_orders'
};

export const initDB = () => {
  if (!localStorage.getItem(DB_KEYS.USER)) {
    localStorage.setItem(DB_KEYS.USER, JSON.stringify(authData.user));
  }
  if (!localStorage.getItem(DB_KEYS.COMPANY)) {
    localStorage.setItem(DB_KEYS.COMPANY, JSON.stringify(companyData));
  }
  if (!localStorage.getItem(DB_KEYS.PRODUCTS)) {
    localStorage.setItem(DB_KEYS.PRODUCTS, JSON.stringify(productsData));
  }
  if (!localStorage.getItem(DB_KEYS.ORDERS)) {
    localStorage.setItem(DB_KEYS.ORDERS, JSON.stringify(initialOrders));
  }
};

initDB();

const getStored = <T>(key: string): T => {
  return JSON.parse(localStorage.getItem(key) || 'null') as T;
};

const setStored = <T>(key: string, data: T): void => {
  localStorage.setItem(key, JSON.stringify(data));
};

export class MockAuthService implements AuthService {
  async login(email: string, _password?: string): Promise<User> {
    const user = getStored<User>(DB_KEYS.USER);
    // Overwrite user email to match login input for dynamic demonstration
    const updatedUser = { ...user, email };
    setStored(DB_KEYS.USER, updatedUser);
    return updatedUser;
  }

  async logout(): Promise<void> {
    // Keep user in DB but can clear session state if required
  }

  async getCurrentUser(): Promise<User | null> {
    return getStored<User>(DB_KEYS.USER);
  }
}

export class MockCustomerDashboardService implements CustomerDashboardService {
  async getAvailableCredit(companyId: string): Promise<{ available: number; limit: number }> {
    const company = getStored<Company>(DB_KEYS.COMPANY);
    if (company && company.id === companyId) {
      return { available: company.availableCredit, limit: company.creditLimit };
    }
    return { available: 0, limit: 0 };
  }

  async getActiveOrders(companyId: string): Promise<Order[]> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    return orders.filter(
      o => o.companyId === companyId &&
      o.status !== 'DRAFT' &&
      o.status !== 'CONFIRMED' &&
      o.status !== 'CANCELLED' &&
      o.status !== 'REJECTED' &&
      o.status !== 'PARTIALLY_CONFIRMED'
    );
  }

  async getRecentOrders(companyId: string): Promise<Order[]> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    return orders
      .filter(o => o.companyId === companyId && o.status !== 'DRAFT')
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);
  }

  async getAlerts(companyId: string): Promise<string[]> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    const alerts: string[] = [];

    // Credit alert
    const company = getStored<Company>(DB_KEYS.COMPANY);
    if (company && company.availableCredit < company.creditLimit * 0.15) {
      alerts.push('Critical: Available credit limit is below 15%.');
    }

    // Pending decisions alert
    const pendingCount = orders.filter(o => o.companyId === companyId && o.status === 'AWAITING_CUSTOMER_DECISION').length;
    if (pendingCount > 0) {
      alerts.push(`Attention: You have ${pendingCount} order(s) awaiting availability approval.`);
    }

    return alerts;
  }
}

export class MockProductCatalogService implements ProductCatalogService {
  async searchProducts(query: string): Promise<Product[]> {
    const products = getStored<Product[]>(DB_KEYS.PRODUCTS) || [];
    if (!query) return products;
    const lower = query.toLowerCase();
    return products.filter(
      p => p.name.toLowerCase().includes(lower) || p.sku.toLowerCase().includes(lower) || p.category.toLowerCase().includes(lower)
    );
  }

  async getAllProducts(): Promise<Product[]> {
    return getStored<Product[]>(DB_KEYS.PRODUCTS) || [];
  }
}

export class MockOrderPreparationService implements OrderPreparationService {
  async getDraftOrder(companyId: string, customerId: string): Promise<Order> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    const draft = orders.find(o => o.companyId === companyId && o.status === 'DRAFT');
    if (draft) return draft;

    // Create a new blank draft
    const company = getStored<Company>(DB_KEYS.COMPANY);
    const newDraft: Order = {
      id: `draft-${Math.random().toString(36).substr(2, 9)}`,
      referenceId: null,
      customerId,
      companyId,
      status: 'DRAFT',
      items: [],
      totalAmount: 0,
      shippingAddress: company.shippingAddresses[0],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      timeline: []
    };
    orders.push(newDraft);
    setStored(DB_KEYS.ORDERS, orders);
    return newDraft;
  }

  async saveDraftOrder(order: Order): Promise<Order> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    const index = orders.findIndex(o => o.id === order.id);
    const updated = {
      ...order,
      totalAmount: order.items.reduce((sum, item) => sum + item.price * item.requestedQuantity, 0),
      updatedAt: new Date().toISOString()
    };

    if (index !== -1) {
      orders[index] = updated;
    } else {
      orders.push(updated);
    }
    setStored(DB_KEYS.ORDERS, orders);
    return updated;
  }
}

export class MockOrderService implements OrderService {
  async getOrders(companyId: string, statusFilter?: OrderStatus | 'ALL', search?: string): Promise<Order[]> {
    let orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    orders = orders.filter(o => o.companyId === companyId);

    if (statusFilter && statusFilter !== 'ALL') {
      orders = orders.filter(o => o.status === statusFilter);
    }

    if (search) {
      const lower = search.toLowerCase();
      orders = orders.filter(
        o => (o.referenceId && o.referenceId.toLowerCase().includes(lower)) ||
             o.id.toLowerCase().includes(lower) ||
             o.items.some(i => i.productName.toLowerCase().includes(lower))
      );
    }

    return orders.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  async getOrderById(orderId: string): Promise<Order | null> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    return orders.find(o => o.id === orderId) || null;
  }

  async submitOrder(order: Order): Promise<Order> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    const company = getStored<Company>(DB_KEYS.COMPANY);

    const refId = `ARB-REF-${Math.floor(1000 + Math.random() * 9000)}`;
    const items = order.items.map(item => {
      // Simulate availability check rules
      let available = item.requestedQuantity;
      // If customer orders more than 5 pumps, make it partially available
      if (item.productId === 'prod-3' && item.requestedQuantity > 2) {
        available = 2;
      }
      // If fitting is ordered, make it fully available
      return {
        ...item,
        availableQuantity: available,
        acceptedQuantity: available
      };
    });

    const total = items.reduce((sum, i) => sum + i.price * i.requestedQuantity, 0);

    // Business Logic state decision
    let finalStatus: OrderStatus = 'CONFIRMED';
    const isPartial = items.some(i => i.availableQuantity < i.requestedQuantity);
    const isExceedingCredit = total > company.availableCredit;

    if (isExceedingCredit) {
      finalStatus = 'REJECTED';
    } else if (isPartial) {
      finalStatus = 'AWAITING_CUSTOMER_DECISION';
    }

    const timeline: OrderTimelineEvent[] = [
      { "status": "Order received", "timestamp": new Date().toISOString(), "completed": true },
      {
        "status": "Availability confirmed",
        "timestamp": new Date().toISOString(),
        "completed": true,
        "note": isPartial ? "Partial inventory available. Awaiting customer confirmation." : "All requested items are available."
      },
      {
        "status": "Company credit approved",
        "timestamp": finalStatus !== 'REJECTED' ? new Date().toISOString() : null,
        "completed": finalStatus !== 'REJECTED',
        "failed": isExceedingCredit,
        "note": isExceedingCredit ? "Credit check failed: Order exceeds available company credit limit." : "Credit check passed."
      },
      {
        "status": "Order confirmed",
        "timestamp": finalStatus === 'CONFIRMED' ? new Date().toISOString() : null,
        "completed": finalStatus === 'CONFIRMED',
        "failed": finalStatus === 'REJECTED',
        "note": finalStatus === 'REJECTED' ? "Order rejected." : undefined
      },
      {
        "status": "Preparing delivery",
        "timestamp": null,
        "completed": false
      }
    ];

    const submittedOrder: Order = {
      ...order,
      id: order.id.startsWith('draft-') ? `ord-${Math.floor(100 + Math.random() * 900)}` : order.id,
      referenceId: refId,
      status: finalStatus,
      items,
      totalAmount: total,
      updatedAt: new Date().toISOString(),
      timeline
    };

    // Remove old draft
    const filteredOrders = orders.filter(o => o.id !== order.id);
    filteredOrders.push(submittedOrder);
    setStored(DB_KEYS.ORDERS, filteredOrders);

    // Deduct credit if confirmed
    if (finalStatus === 'CONFIRMED') {
      company.availableCredit = Math.max(0, company.availableCredit - total);
      setStored(DB_KEYS.COMPANY, company);
    }

    return submittedOrder;
  }

  async respondToPartialProposal(orderId: string, accept: boolean, updatedItems?: { productId: string; quantity: number }[]): Promise<Order> {
    const orders = getStored<Order[]>(DB_KEYS.ORDERS) || [];
    const company = getStored<Company>(DB_KEYS.COMPANY);
    const orderIndex = orders.findIndex(o => o.id === orderId);

    if (orderIndex === -1) throw new Error('Order not found');
    const order = orders[orderIndex];

    let newStatus: OrderStatus = accept ? 'PARTIALLY_CONFIRMED' : 'CANCELLED';
    const items = order.items.map(item => {
      const update = updatedItems?.find(u => u.productId === item.productId);
      const acceptedQty = accept ? (update ? update.quantity : item.availableQuantity) : 0;
      return {
        ...item,
        acceptedQuantity: acceptedQty
      };
    });

    const newTotal = items.reduce((sum, i) => sum + i.price * i.acceptedQuantity, 0);

    const timeline = [...order.timeline];
    // Update availability note
    const availIndex = timeline.findIndex(t => t.status === 'Availability confirmed');
    if (availIndex !== -1) {
      timeline[availIndex] = {
        ...timeline[availIndex],
        note: accept ? `Customer accepted partial proposal.` : `Customer rejected partial proposal.`
      };
    }

    // Add decision milestones
    timeline.push({
      status: accept ? "Order confirmed (Partial)" : "Order cancelled",
      timestamp: new Date().toISOString(),
      completed: true,
      failed: !accept
    });

    if (accept) {
      timeline.push({
        status: "Preparing delivery",
        timestamp: new Date().toISOString(),
        completed: true
      });
    }

    const updatedOrder: Order = {
      ...order,
      status: newStatus,
      items,
      totalAmount: newTotal,
      updatedAt: new Date().toISOString(),
      timeline
    };

    orders[orderIndex] = updatedOrder;
    setStored(DB_KEYS.ORDERS, orders);

    if (accept) {
      company.availableCredit = Math.max(0, company.availableCredit - newTotal);
      setStored(DB_KEYS.COMPANY, company);
    }

    return updatedOrder;
  }
}

export class MockCompanyService implements CompanyService {
  async getCompany(companyId: string): Promise<Company | null> {
    const company = getStored<Company>(DB_KEYS.COMPANY);
    if (company && company.id === companyId) return company;
    return null;
  }
}

export const services = {
  auth: new MockAuthService(),
  dashboard: new MockCustomerDashboardService(),
  products: new MockProductCatalogService(),
  preparation: new MockOrderPreparationService(),
  order: new MockOrderService(),
  company: new MockCompanyService()
};
