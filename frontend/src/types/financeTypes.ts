export interface RevenueMetric {
  value: number;
  trend: number;
  trendDirection: 'up' | 'down';
}

export interface MRRAnalytics {
  total: number;
  expansion: number;
  contraction: number;
  newMRR: number;
  reactivation: number;
  history: Array<{ date: string; value: number }>;
}

export interface ARRAnalytics {
  total: number;
  history: Array<{ date: string; value: number }>;
  byRegion: Array<{ region: string; value: number }>;
  byPlan: Array<{ plan: string; value: number }>;
}

export interface ChurnMetric {
  customerChurnRate: number;
  revenueChurnRate: number;
  churnedRevenue: number;
  history: Array<{ date: string; value: number }>;
  reasons: Array<{ reason: string; count: number }>;
}

export interface ARPUAnalytics {
  arpu: number;
  ltv: number;
  cacLtvRatio: string;
  history: Array<{ date: string; value: number }>;
}

export interface InvoiceLineItem {
  lineItemId: string;
  description: string;
  lineType: 'PLAN' | 'ADDON' | 'METERED' | 'PRORATION' | 'DISCOUNT' | 'TAX' | 'CREDIT';
  quantity: number;
  unitPriceMinor: number;
  amountMinor: number;
  periodStart: string | null;
  periodEnd: string | null;
}

export interface Invoice {
  id: string;
  customerId: string;
  customerName: string;
  amount: number;
  status: 'Paid' | 'Pending' | 'Failed' | 'Overdue' | 'Draft';
  date: string;
  dueDate: string;
  lineItems?: InvoiceLineItem[];
}

export interface Payment {
  id: string;
  invoiceId: string;
  customerId: string;
  amount: number;
  status: 'Successful' | 'Failed';
  method: string;
  date: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  customerId: string;
  amount: number;
  status: 'Approved' | 'Pending' | 'Rejected' | 'Processing';
  reason: string;
  date: string;
}

export interface RevenueSnapshot {
  id: string;
  date: string;
  totalRevenue: number;
  mrr: number;
  arr: number;
  activeCustomers: number;
  newCustomers: number;
  netChurnPercent: number;
}

export interface ExportJob {
  id: string;
  type: string;
  status: 'Completed' | 'Processing' | 'Failed';
  format: 'CSV' | 'PDF';
  date: string;
  url?: string;
}

export interface CustomerSegment {
  id: string;
  name: string;
  customerCount: number;
  mrr: number;
  arpu: number;
}

export interface DashboardOverview {
  mrr: RevenueMetric;
  arr: RevenueMetric;
  arpu: RevenueMetric;
  ltv: RevenueMetric;
  churnRate: RevenueMetric;
  activeCustomers: RevenueMetric;
  failedPayments: RevenueMetric;
  refundAmount: RevenueMetric;
  revenueByPlan: Array<{ name: string; value: number }>;
  revenueByRegion: Array<{ name: string; value: number }>;
  recentActivity: Array<{ id: string; type: string; description: string; date: string; amount?: number }>;
  revenueTrend: Array<{ date: string; value: number }>;
}
