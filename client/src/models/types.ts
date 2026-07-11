export interface User {
  id: string;
  name: string;
  email: string;
  role: string;
  companyId: string;
}

export interface BillingContact {
  name: string;
  email: string;
  phone: string;
}

export interface ShippingAddress {
  id: string;
  name: string;
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
}

export interface Company {
  id: string;
  name: string;
  creditLimit: number;
  availableCredit: number;
  billingContact: BillingContact;
  shippingAddresses: ShippingAddress[];
  paymentTerms: string;
  authorizedBuyers: string[];
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  price: number;
  category: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  price: number;
  requestedQuantity: number;
  availableQuantity: number;
  acceptedQuantity: number;
}

export interface OrderTimelineEvent {
  status: string;
  timestamp: string | null;
  completed: boolean;
  failed?: boolean;
  note?: string;
}

export type OrderStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'CREDIT_RESERVATION_REQUESTED'
  | 'CREDIT_RESERVED'
  | 'INVENTORY_RESERVATION_REQUESTED'
  | 'INVENTORY_FULLY_RESERVED'
  | 'AWAITING_CUSTOMER_DECISION'
  | 'CONFIRMED'
  | 'PARTIALLY_CONFIRMED'
  | 'CANCELLED'
  | 'REJECTED';

export interface Order {
  id: string;
  referenceId: string | null;
  customerId: string;
  companyId: string;
  status: OrderStatus;
  items: OrderItem[];
  totalAmount: number;
  shippingAddress: ShippingAddress;
  createdAt: string;
  updatedAt: string;
  timeline: OrderTimelineEvent[];
}

export interface AvailabilityReviewItem {
  productId: string;
  productName: string;
  requestedQuantity: number;
  availableQuantity: number;
  status: 'fully_available' | 'partially_available' | 'unavailable';
}
