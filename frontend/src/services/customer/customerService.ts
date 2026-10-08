import { fetchWithSession } from '../auth/authService';
// Relative URL - proxied through CRA dev server
const API_BASE = '/customer';
// ==================== TYPES ====================
export interface CustomerProfile {
  customerId: number;
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  currency: string;
  country: string;
  state: string | null;
  city: string | null;
  addressLine1: string | null;
  postalCode: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  createdAt: string;
}
export interface Plan {
  planId: number;
  name: string;
  billingPeriod: 'MONTHLY' | 'YEARLY';
  defaultPriceMinor: number;
  defaultCurrency: string;
  trialDays: number;
  setupFeeMinor: number;
  taxMode: 'INCLUSIVE' | 'EXCLUSIVE';
  effectiveFrom: string;
  effectiveTo: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  productName: string;
  features?: string[];
}
export interface AddOn {
  addOnId: number;
  name: string;
  priceMinor: number;
  currency: string;
  billingPeriod: 'MONTHLY' | 'YEARLY';
  status: 'ACTIVE' | 'INACTIVE';
}
export interface MeteredComponent {
  componentId: number;
  planId: number;
  name: string;
  unitName: string;
  pricePerUnitMinor: number;
  freeTierQuantity: number;
  status: 'ACTIVE' | 'INACTIVE';
}
export interface Subscription {
  subscriptionId: number;
  customerId: number;
  planId: number;
  planName: string;
  planPriceMinor?: number;
  billingPeriod?: 'MONTHLY' | 'YEARLY';
  status: 'DRAFT' | 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'PAUSED' | 'CANCELED' | 'ON_HOLD';
  startDate: string;
  trialEndDate: string | null;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  canceledAt: string | null;
  pausedFrom: string | null;
  pausedTo: string | null;
  currency: string;
  addOns: SubscriptionAddOn[];
  meteredUsage: MeteredUsage[];
  discountMinor?: number;
  totalDueMinor?: number;
  creditBalanceMinor?: number;
}
export interface SubscriptionAddOn {
  itemId: number;
  addonId: number;
  addonName: string;
  unitPriceMinor: number;
  quantity: number;
}
export interface MeteredUsage {
  componentId: number;
  componentName: string;
  unitName: string;
  quantityUsed: number;
  freeTierQuantity: number;
  pricePerUnitMinor: number;
  costMinor: number;
}
export interface Invoice {
  invoiceId: number;
  invoiceNumber: string;
  subscriptionId: number;
  status: 'DRAFT' | 'OPEN' | 'PAID' | 'VOID' | 'UNCOLLECTIBLE';
  billingReason: string;
  issueDate: string;
  dueDate: string | null;
  subtotalMinor: number;
  taxMinor: number;
  discountMinor: number;
  totalMinor: number;
  balanceMinor: number;
  currency: string;
  lineItems: InvoiceLineItem[];
}
export interface InvoiceLineItem {
  lineItemId: number;
  description: string;
  lineType: 'PLAN' | 'ADDON' | 'METERED' | 'PRORATION' | 'DISCOUNT' | 'TAX';
  quantity: number;
  unitPriceMinor: number;
  amountMinor: number;
  periodStart: string | null;
  periodEnd: string | null;
}
export interface Payment {
  paymentId: number;
  invoiceId: number;
  invoiceNumber: string;
  amountMinor: number;
  currency: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | 'PARTIALLY_REFUNDED';
  attemptNo: number;
  failureReason: string | null;
  createdAt: string;
}
export interface PaymentMethod {
  paymentMethodId: number;
  paymentType: 'CARD' | 'UPI';
  cardLast4: string | null;
  cardBrand: string | null;
  upiId: string | null;
  isDefault: boolean;
  expiryMonth: number | null;
  expiryYear: number | null;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
}
export interface Notification {
  id: number;
  type: string;
  subject: string;
  body: string;
  channel: 'EMAIL' | 'SMS' | 'IN_APP';
  status: 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED' | 'READ';
  createdAt: string;
}
export interface Coupon {
  couponId: number;
  code: string;
  name: string;
  type: 'PERCENT' | 'FIXED';
  amount: number;
  currency: string | null;
  duration: 'ONCE' | 'REPEATING' | 'FOREVER';
  durationInMonths: number | null;
  validFrom: string;
  validTo: string | null;
  status: 'ACTIVE' | 'EXPIRED' | 'DISABLED';
}
export interface UsageRecord {
  usageId: number;
  componentId: number;
  componentName: string;
  quantity: number;
  unitName: string;
  recordedAt: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
}
export interface CreditNote {
  creditNoteId: number;
  creditNoteNumber: string;
  invoiceId: number;
  invoiceNumber: string;
  reason: string;
  amountMinor: number;
  status: 'DRAFT' | 'ISSUED' | 'APPLIED' | 'VOIDED';
  createdAt: string;
}
export interface CancellationResponse {
  refundIssued: boolean;
  refundAmountMinor: number;
  currency: string;
  refundGatewayRef: string | null;
  creditNoteNumber: string | null;
  message: string;
}
export interface CancellationRequest {
  requestId: number;
  subscriptionId: number;
  planName: string;
  customerEmail: string;
  customerName: string;
  reason: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';
  atPeriodEnd: boolean;
  createdAt: string;
  processedByEmail: string | null;
  processedAt: string | null;
  agentNotes: string | null;
}
// ==================== PROFILE ====================
export const getCustomerProfile = async (): Promise<CustomerProfile> => {
  const response = await fetchWithSession(`${API_BASE}/me`);
  if (!response.ok) throw new Error('Failed to fetch profile');
  return response.json();
};
export const updateCustomerProfile = async (data: Partial<CustomerProfile>): Promise<CustomerProfile> => {
  const response = await fetchWithSession(`${API_BASE}/me`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error('Failed to update profile');
  return response.json();
};
// ==================== PLANS ====================
export const getAvailablePlans = async (region?: string): Promise<Plan[]> => {
  const query = region ? `?region=${region}` : '';
  const response = await fetchWithSession(`${API_BASE}/plans${query}`);
  if (!response.ok) throw new Error('Failed to fetch plans');
  return response.json();
};
export const getAvailableAddOns = async (): Promise<AddOn[]> => {
  const response = await fetchWithSession(`${API_BASE}/addons`);
  if (!response.ok) throw new Error('Failed to fetch add-ons');
  return response.json();
};
// ==================== SUBSCRIPTION ====================
export const getCurrentSubscription = async (): Promise<Subscription | null> => {
  try {
    const response = await fetchWithSession(`${API_BASE}/subscription`);
    return response.json();
  } catch (error: any) {
    if (error.status === 404) return null;
    throw error;
  }
};
export const createSubscription = async (data: {
  planId: number;
  paymentMethodId: number;
  couponCode?: string;
}): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/subscription`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to create subscription');
  }
  return response.json();
};
export const upgradeSubscription = async (data: {
  planId: number;
  proration: boolean;
}): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/upgrade`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error('Failed to upgrade subscription');
  return response.json();
};
export const cancelSubscription = async (params?: { atPeriodEnd?: boolean }): Promise<CancellationResponse> => {
  const query = params?.atPeriodEnd !== undefined ? `?atPeriodEnd=${params.atPeriodEnd}` : '';
  const response = await fetchWithSession(`${API_BASE}/subscription${query}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to cancel subscription');
  return response.json();
};
export const submitCancellationRequest = async (reason: string, atPeriodEnd: boolean): Promise<CancellationRequest> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/cancel-request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason, atPeriodEnd }),
  });
  if (!response.ok) throw new Error('Failed to submit cancellation request');
  return response.json();
};
export const withdrawCancellationRequest = async (): Promise<CancellationRequest> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/cancel-request/withdraw`, {
    method: 'POST',
  });
  if (!response.ok) throw new Error('Failed to withdraw cancellation request');
  return response.json();
};
export const getPendingCancellationRequest = async (): Promise<CancellationRequest | null> => {
  try {
    const response = await fetchWithSession(`${API_BASE}/subscription/cancel-request`);
    if (response.status === 204 || response.status === 404) return null;
    const text = await response.text();
    if (!text) return null;
    return JSON.parse(text);
  } catch (error: any) {
    if (error.status === 404 || error.status === 204) return null;
    throw error;
  }
};
export const pauseSubscription = async (data: { pausedTo: string }): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/pause`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error('Failed to pause subscription');
  return response.json();
};
export const resumeSubscription = async (): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/resume`, {
    method: 'PUT',
  });
  if (!response.ok) throw new Error('Failed to resume subscription');
  return response.json();
};
// ==================== ADD-ONS ====================
export const addAddOn = async (addonId: number): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/addons/${addonId}`, {
    method: 'POST',
  });
  if (!response.ok) throw new Error('Failed to add add-on');
  return response.json();
};
export const removeAddOn = async (addonId: number): Promise<Subscription> => {
  const response = await fetchWithSession(`${API_BASE}/addons/${addonId}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to remove add-on');
  return response.json();
};
// ==================== METERED USAGE ====================
export const getMeteredUsage = async (params?: {
  startDate?: string;
  endDate?: string;
}): Promise<UsageRecord[]> => {
  const query = params ? `?${new URLSearchParams(params as Record<string, string>)}` : '';
  const response = await fetchWithSession(`${API_BASE}/usage${query}`);
  if (!response.ok) throw new Error('Failed to fetch usage');
  return response.json();
};
// ==================== BILLING ====================
export const getInvoices = async (params?: {
  status?: string;
  from?: string;
  to?: string;
}): Promise<Invoice[]> => {
  const query = params ? `?${new URLSearchParams(params as Record<string, string>)}` : '';
  const response = await fetchWithSession(`${API_BASE}/invoices${query}`);
  if (!response.ok) throw new Error('Failed to fetch invoices');
  return response.json();
};
export const getInvoiceDetail = async (invoiceId: number): Promise<Invoice> => {
  const response = await fetchWithSession(`${API_BASE}/invoices/${invoiceId}`);
  if (!response.ok) throw new Error('Failed to fetch invoice');
  return response.json();
};
export const downloadInvoice = async (invoiceId: number): Promise<Blob> => {
  const response = await fetchWithSession(`${API_BASE}/invoices/${invoiceId}/download`);
  if (!response.ok) throw new Error('Failed to download invoice');
  return response.blob();
};
export const getPayments = async (): Promise<Payment[]> => {
  const response = await fetchWithSession(`${API_BASE}/payments`);
  if (!response.ok) throw new Error('Failed to fetch payments');
  return response.json();
};
export const getCreditNotes = async (): Promise<CreditNote[]> => {
  const response = await fetchWithSession(`${API_BASE}/credit-notes`);
  if (!response.ok) throw new Error('Failed to fetch credit notes');
  return response.json();
};
// ==================== COUPONS ====================
export const applyCoupon = async (code: string): Promise<Coupon> => {
  const response = await fetchWithSession(`${API_BASE}/coupons/apply`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  });
  if (!response.ok) throw new Error('Invalid or expired coupon');
  return response.json();
};
export const validateCoupon = async (code: string): Promise<Coupon> => {
  const response = await fetchWithSession(`${API_BASE}/coupons/validate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  });
  if (!response.ok) throw new Error('Invalid or expired coupon');
  return response.json();
};
export const getAvailableCoupons = async (): Promise<Coupon[]> => {
  const response = await fetchWithSession(`${API_BASE}/coupons`);
  if (!response.ok) throw new Error('Failed to fetch coupons');
  return response.json();
};
// ==================== PAYMENT METHODS ====================
export const getPaymentMethods = async (): Promise<PaymentMethod[]> => {
  const response = await fetchWithSession(`${API_BASE}/payment-methods`);
  if (!response.ok) throw new Error('Failed to fetch payment methods');
  return response.json();
};
export const addPaymentMethod = async (data: {
  paymentType: 'CARD' | 'UPI';
  cardNumber?: string;
  expiryMonth?: number;
  expiryYear?: number;
  cvv?: string;
  upiId?: string;
  isDefault?: boolean;
}): Promise<PaymentMethod> => {
  const response = await fetchWithSession(`${API_BASE}/payment-methods`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error('Failed to add payment method');
  return response.json();
};
export const setDefaultPaymentMethod = async (paymentMethodId: number): Promise<void> => {
  const response = await fetchWithSession(`${API_BASE}/payment-methods/${paymentMethodId}/default`, {
    method: 'PUT',
  });
  if (!response.ok) throw new Error('Failed to set default payment method');
};
export const deletePaymentMethod = async (paymentMethodId: number): Promise<void> => {
  const response = await fetchWithSession(`${API_BASE}/payment-methods/${paymentMethodId}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error('Failed to delete payment method');
};
// ==================== SUPPORT ====================
export const sendSupportMessage = async (data: {
  subject: string;
  message: string;
  category?: string;
}): Promise<void> => {
  const response = await fetchWithSession(`${API_BASE}/support/contact`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new Error('Failed to send message');
};
export const getFAQs = async (): Promise<{ question: string; answer: string; category: string }[]> => {
  const response = await fetchWithSession(`${API_BASE}/support/faqs`);
  if (!response.ok) throw new Error('Failed to fetch FAQs');
  return response.json();
};
// ==================== SUBSCRIPTION FLOW ====================
export interface CustomerStatusResponse {
  isCustomer: boolean;
  hasActiveSubscription: boolean;
  message: string;
}
export interface CustomerRegistrationRequest {
  phone: string;
  country: string;
  state: string;
  city: string;
  addressLine1: string;
  postalCode: string;
  currency: string;
}
export interface PaymentMethodRequest {
  paymentType: 'CARD' | 'UPI';
  cardNumber?: string;
  cardholderName?: string;
  expiryMonth?: string;
  expiryYear?: string;
  cvv?: string;
  upiId?: string;
}
export interface SubscriptionCompletionRequest {
  planId: number;
  paymentMethodId?: number;
  paymentMethod?: PaymentMethodRequest;
  billingPeriod: 'MONTHLY' | 'YEARLY';
  couponCode?: string;
}
export interface CustomerStatusResponse {
  isCustomer: boolean;
  hasActiveSubscription: boolean;
  hasDraftSubscription: boolean;
  message: string;
  trialEndDate?: string;
}
export interface SubscriptionResponse {
  subscriptionId: number;
  invoiceId: number;
  invoiceNumber: string;
  status: string;
  message: string;
  trialEndDate?: string;
}
// Check customer status
export const getCustomerStatus = async (): Promise<CustomerStatusResponse> => {
  const response = await fetchWithSession(`${API_BASE}/status`);
  if (!response.ok) throw new Error('Failed to check customer status');
  return response.json();
};
// Get featured plans (2 plans for landing page)
export const getFeaturedPlans = async (region?: string): Promise<Plan[]> => {
  const query = region ? `?region=${region}` : '';
  const response = await fetchWithSession(`${API_BASE}/plans/featured${query}`);
  if (!response.ok) throw new Error('Failed to fetch featured plans');
  return response.json();
};
// Get all active plans
export const getAllPlans = async (region?: string): Promise<Plan[]> => {
  const query = region ? `?region=${region}` : '';
  const response = await fetchWithSession(`${API_BASE}/plans/all${query}`);
  if (!response.ok) throw new Error('Failed to fetch all plans');
  return response.json();
};
// Step 1: Register customer details
export const registerCustomerDetails = async (data: CustomerRegistrationRequest): Promise<{ customerId: number; message: string }> => {
  const response = await fetchWithSession(`${API_BASE}/register-details`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to register customer details');
  }
  return response.json();
};
// Step 2: Create payment method
export const createPaymentMethod = async (customerId: number, data: PaymentMethodRequest): Promise<{ paymentMethodId: number; last4: string; brand: string; message: string }> => {
  const response = await fetchWithSession(`${API_BASE}/payment-method?customerId=${customerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to create payment method');
  }
  return response.json();
};
// Step 3: Complete subscription
export const completeSubscription = async (customerId: number, data: SubscriptionCompletionRequest): Promise<SubscriptionResponse> => {
  const response = await fetchWithSession(`${API_BASE}/subscription/complete?customerId=${customerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Failed to complete subscription');
  }
  return response.json();
};
// ==================== NOTIFICATIONS ====================
export const getUnreadNotifications = async (): Promise<Notification[]> => {
  const response = await fetchWithSession('/notifications/me');
  if (!response.ok) throw new Error('Failed to fetch notifications');
  const data = await response.json();
  return data.map((item: any) => ({
    id: item.notificationId,
    type: item.type,
    subject: item.subject,
    body: item.body,
    channel: item.channel || 'IN_APP',
    status: item.status,
    createdAt: item.createdAt,
  }));
};
export const markNotificationAsRead = async (id: number): Promise<void> => {
  const response = await fetchWithSession(`/notifications/${id}/read`, {
    method: 'PUT',
  });
  if (!response.ok) throw new Error('Failed to mark notification as read');
};
export const markAllNotificationsAsRead = async (): Promise<void> => {
  const response = await fetchWithSession(`/notifications/read-all`, {
    method: 'PUT',
  });
  if (!response.ok) throw new Error('Failed to mark all notifications as read');
};
