import type { User, Company, Product, Order, OrderStatus } from '../models/types';

export interface AuthService {
  login(email: string, password?: string): Promise<User>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<User | null>;
}

export interface CustomerDashboardService {
  getAvailableCredit(companyId: string): Promise<{ available: number; limit: number }>;
  getActiveOrders(companyId: string): Promise<Order[]>;
  getRecentOrders(companyId: string): Promise<Order[]>;
  getAlerts(companyId: string): Promise<string[]>;
}

export interface ProductCatalogService {
  searchProducts(query: string): Promise<Product[]>;
  getAllProducts(): Promise<Product[]>;
}

export interface OrderPreparationService {
  getDraftOrder(companyId: string, customerId: string): Promise<Order>;
  saveDraftOrder(order: Order): Promise<Order>;
}

export interface OrderService {
  getOrders(companyId: string, statusFilter?: OrderStatus | 'ALL', search?: string): Promise<Order[]>;
  getOrderById(orderId: string): Promise<Order | null>;
  submitOrder(order: Order): Promise<Order>;
  respondToPartialProposal(orderId: string, accept: boolean, updatedItems?: { productId: string; quantity: number }[]): Promise<Order>;
}

export interface CompanyService {
  getCompany(companyId: string): Promise<Company | null>;
}
