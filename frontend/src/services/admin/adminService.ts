import type {
  DashboardStats,
  Product,
  PlanResponse,
  PriceBookResponse,
  AddOnResponse,
  MeteredComponentResponse,
  TaxRate,
  Coupon,
  CustomerResponse,
  StaffResponse,
  SubscriptionResponse,
} from './adminTypes';
// API base URL — proxied to backend by both CRA (setupProxy.js) and Vite (vite.config.ts)
const API_BASE = '/api/admin';
// Generic fetch wrapper with session cookie support
async function adminFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  const res = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });
  if (!res.ok) {
    const errorText = await res.text();
    console.error(`Admin API error (${res.status}):`, errorText);
    let errorMessage = `Request failed: ${res.status}`;
    try {
      const errorJson = JSON.parse(errorText);
      errorMessage = errorJson.message || errorMessage;
    } catch {
      errorMessage = errorText || errorMessage;
    }
    throw new Error(errorMessage);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : ({} as T);
}
// ==================== DASHBOARD ====================
export const getDashboardStats = () => adminFetch<DashboardStats>('/dashboard/stats');
// ==================== PRODUCTS ====================
export const getAllProducts = () => adminFetch<Product[]>('/products');
export const updateProduct = (id: number, data: Partial<Product>) =>
  adminFetch<Product>(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const toggleProductStatus = (id: number) =>
  adminFetch<void>(`/products/${id}/toggle-status`, { method: 'PATCH' });
// ==================== PLANS ====================
export const getAllPlans = () => adminFetch<PlanResponse[]>('/plans');
export const createPlan = (data: any) =>
  adminFetch<PlanResponse>('/plans', { method: 'POST', body: JSON.stringify(data) });
export const deletePlan = (id: number) =>
  adminFetch<void>(`/plans/${id}`, { method: 'DELETE' });
// ==================== PRICE BOOKS ====================
export const getPriceBooks = () => adminFetch<PriceBookResponse[]>('/pricebooks');
export const createPriceBook = (data: any) =>
  adminFetch<PriceBookResponse>('/pricebooks', { method: 'POST', body: JSON.stringify(data) });
export const updatePriceBook = (id: number, data: any) =>
  adminFetch<PriceBookResponse>(`/pricebooks/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const archivePriceBook = (id: number) =>
  adminFetch<void>(`/pricebooks/${id}/archive`, { method: 'PUT' });
// ==================== ADD-ONS ====================
export const getAddOns = () => adminFetch<AddOnResponse[]>('/addons');
export const createAddOn = (data: any) =>
  adminFetch<AddOnResponse>('/addons', { method: 'POST', body: JSON.stringify(data) });
export const updateAddOn = (id: number, data: any) =>
  adminFetch<AddOnResponse>(`/addons/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const toggleAddOnStatus = (id: number) =>
  adminFetch<void>(`/addons/${id}/toggle-status`, { method: 'PATCH' });
// ==================== METERED COMPONENTS ====================
export const getMeteredComponents = () => adminFetch<MeteredComponentResponse[]>('/metered');
export const createMetered = (data: any) =>
  adminFetch<MeteredComponentResponse>('/metered', { method: 'POST', body: JSON.stringify(data) });
export const updateMetered = (id: number, data: any) =>
  adminFetch<MeteredComponentResponse>(`/metered/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const toggleMeteredStatus = (id: number) =>
  adminFetch<void>(`/metered/${id}/toggle-status`, { method: 'PATCH' });
// ==================== TAX RATES ====================
export const getTaxRates = () => adminFetch<TaxRate[]>('/taxrates');
export const createTaxRate = (data: any) =>
  adminFetch<TaxRate>('/taxrates', { method: 'POST', body: JSON.stringify(data) });
export const updateTaxRate = (id: number, data: any) =>
  adminFetch<TaxRate>(`/taxrates/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const deleteTaxRate = (id: number) =>
  adminFetch<void>(`/taxrates/${id}`, { method: 'DELETE' });
// ==================== COUPONS ====================
export const getCoupons = () => adminFetch<Coupon[]>('/coupons');
export const createCoupon = (data: any) =>
  adminFetch<Coupon>('/coupons', { method: 'POST', body: JSON.stringify(data) });
export const updateCoupon = (id: number, data: any) =>
  adminFetch<Coupon>(`/coupons/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const toggleCouponStatus = (id: number) =>
  adminFetch<void>(`/coupons/${id}/toggle-status`, { method: 'PATCH' });
// ==================== CUSTOMERS ====================
export const getCustomers = () => adminFetch<CustomerResponse[]>('/customers');
// ==================== STAFF ====================
export const getStaff = () => adminFetch<StaffResponse[]>('/staff');
export const createStaff = (data: any) =>
  adminFetch<StaffResponse>('/staff', { method: 'POST', body: JSON.stringify(data) });
export const deleteStaff = (id: number) =>
  adminFetch<void>(`/staff/${id}`, { method: 'DELETE' });
// ==================== SUBSCRIPTIONS ====================
export const getSubscriptions = () => adminFetch<SubscriptionResponse[]>('/subscriptions');
