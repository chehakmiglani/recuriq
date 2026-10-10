// Shapes of the /api/finance responses, as sent by the backend DTOs.
// Monetary fields ending in "Minor" are paise; "amount" fields are already rupees.

import type { InvoiceLineItem } from '../../types/financeTypes';

export interface Page<T> {
  content?: T[];
  totalElements?: number;
}

export interface MonthlyTrendResponse {
  month: string;
  valueMinor?: number;
}

export interface PlanRevenueResponse {
  planName: string;
  revenueMinor?: number;
}

export interface RegionRevenueResponse {
  region: string;
  revenueMinor?: number;
}

export interface DashboardResponse {
  mrrMinor?: number;
  arrMinor?: number;
  arpuMinor?: number;
  ltvMinor?: number;
  netChurnPercent?: number;
  activeCustomers?: number;
  failedPaymentsCount?: number;
  refundAmountMinor?: number;
  revenueByPlan?: PlanRevenueResponse[];
  revenueByRegion?: RegionRevenueResponse[];
  revenueTrend?: MonthlyTrendResponse[];
}

export interface MrrReportResponse {
  mrrMinor?: number;
  expansionMinor?: number;
  contractionMinor?: number;
  newMrrMinor?: number;
  reactivationMinor?: number;
  revenueTrend?: MonthlyTrendResponse[];
}

export interface ArrReportResponse {
  arrMinor?: number;
  arrTrend?: MonthlyTrendResponse[];
  revenueByRegion?: RegionRevenueResponse[];
  revenueByPlan?: PlanRevenueResponse[];
}

export interface ChurnReportResponse {
  netChurnPercent?: number;
  revenueChurnPercent?: number;
  churnedRevenueMinor?: number;
  churnTrend?: Array<{ month: string; netChurnPercent?: number }>;
  reasons?: Array<{ reason: string; count: number }>;
}

export interface ArpuLtvReportResponse {
  arpuMinor?: number;
  ltvMinor?: number;
  cacLtvRatio?: string;
  arpuTrend?: MonthlyTrendResponse[];
}

export interface InvoiceRecordResponse {
  invoiceNumber: string;
  customerId: number;
  amount: number;
  date: string;
  dueDate: string;
  status: string;
}

export interface InvoiceLineItemResponse {
  lineItemId: number;
  description: string;
  lineType: InvoiceLineItem['lineType'];
  quantity: number;
  unitPriceMinor: number;
  amountMinor: number;
  periodStart: string | null;
  periodEnd: string | null;
}

export interface InvoiceDetailResponse extends InvoiceRecordResponse {
  lineItems?: InvoiceLineItemResponse[];
}

export interface PaymentRecordResponse {
  paymentId: number;
  invoiceNumber: string;
  amount: number;
  paymentMethod: string;
  date: string;
  status: string;
}

export interface RefundCreditResponse {
  refundId: string;
  paymentId: number;
  amount: number;
  reason?: string | null;
  date: string;
  status: string;
}

export interface RevenueSnapshotResponse {
  date: string;
  totalRevenueMinor?: number;
  mrrMinor?: number;
  arrMinor?: number;
  activeCustomers?: number;
  newCustomers?: number;
  netChurnPercent?: number;
}
