// TypeScript interfaces matching the backend DTOs

export interface DashboardStats {
  // Catalog Management Stats
  totalProducts: number;
  totalPlans: number;
  activeCoupons: number;
  totalAddOns: number;
  activeTaxRates: number;
  totalPriceBooks: number;
  // User Management Stats
  totalCustomers: number;
  totalStaff: number;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  plansCount: number;
}

export interface PlanResponse {
  id: number;
  name: string;
  billingPeriod: string;
  defaultPriceMinor: number;
  defaultCurrency: string;
  trialDays: number;
  setupFeeMinor: number;
  taxMode: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: string;
  productId: number;
  productName: string;
}

export interface PriceBookResponse {
  id: number;
  planId: number;
  planName: string;
  region: string;
  currency: string;
  priceMinor: number;
  effectiveFrom: string;
  effectiveTo: string | null;
}

export interface AddOnResponse {
  id: number;
  name: string;
  priceMinor: number;
  currency: string;
  billingPeriod: string;
  taxMode: string;
  status: string;
}

export interface MeteredComponentResponse {
  id: number;
  name: string;
  unitName: string;
  pricePerUnitMinor: number;
  freeTierQuantity: number;
  status: string;
  planId: number;
  planName: string;
}

export interface TaxRate {
  id: number;
  name: string;
  region: string;
  ratePercent: number;
  inclusive: boolean;
  effectiveFrom: string;
  effectiveTo: string | null;
}

export interface Coupon {
  id: number;
  code: string;
  name: string;
  type: string;
  amount: number;
  currency: string | null;
  duration: string;
  durationInMonths: number | null;
  maxRedemptions: number | null;
  redeemedCount: number;
  validFrom: string;
  validTo: string | null;
  status: string;
}

export interface CustomerResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  currency: string;
  country: string;
  status: string;
  createdAt: string;
}

export interface StaffResponse {
  id: number;
  fullName: string;
  email: string;
  role: string;
  status: string;
}

export interface SubscriptionResponse {
  id: number;
  customerId: number;
  customerName: string;
  planId: number;
  planName: string;
  status: string;
  startDate: string;
  currentPeriodEnd: string;
  currency: string;
}
