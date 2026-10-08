import type { DashboardOverview, MRRAnalytics, ARRAnalytics, ChurnMetric, ARPUAnalytics, Refund, RevenueSnapshot, Invoice, Payment } from "../../types/financeTypes";

const API_BASE = '/api/finance';

async function financeFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
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
    console.error(`Finance API error (${res.status}):`, errorText);
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

export const financeService = {
  getDashboardOverview: async (): Promise<DashboardOverview> => {
    const data = await financeFetch<any>('/dashboard');
    return {
      mrr: { value: (data.mrrMinor || 0) / 100, trend: 0, trendDirection: 'up' },
      arr: { value: (data.arrMinor || 0) / 100, trend: 0, trendDirection: 'up' },
      arpu: { value: (data.arpuMinor || 0) / 100, trend: 0, trendDirection: 'up' },
      ltv: { value: (data.ltvMinor || 0) / 100, trend: 0, trendDirection: 'up' },
      churnRate: { value: data.netChurnPercent || 0, trend: 0, trendDirection: 'down' },
      activeCustomers: { value: data.activeCustomers || 0, trend: 0, trendDirection: 'up' },
      failedPayments: { value: data.failedPaymentsCount || 0, trend: 0, trendDirection: 'down' },
      refundAmount: { value: (data.refundAmountMinor || 0) / 100, trend: 0, trendDirection: 'up' },
      revenueByPlan: data.revenueByPlan?.map((p: any) => ({
        name: p.planName,
        value: (p.revenueMinor || 0) / 100
      })) || [],
      revenueByRegion: data.revenueByRegion?.map((r: any) => ({
        name: r.region,
        value: (r.revenueMinor || 0) / 100
      })) || [],
      recentActivity: [],
      revenueTrend: data.revenueTrend?.map((t: any) => ({
        date: t.month,
        value: (t.valueMinor || 0) / 100
      })) || []
    };
  },

  getMRRAnalytics: async (): Promise<MRRAnalytics> => {
    const data = await financeFetch<any>('/reports/mrr');
    return {
      total: (data.mrrMinor || 0) / 100,
      expansion: (data.expansionMinor || 0) / 100,
      contraction: (data.contractionMinor || 0) / 100,
      newMRR: (data.newMrrMinor || 0) / 100,
      reactivation: (data.reactivationMinor || 0) / 100,
      history: data.revenueTrend?.map((t: any) => ({
        date: t.month,
        value: (t.valueMinor || 0) / 100
      })) || []
    };
  },

  getARRAnalytics: async (): Promise<ARRAnalytics> => {
    const data = await financeFetch<any>('/reports/arr');
    return {
      total: (data.arrMinor || 0) / 100,
      history: data.arrTrend?.map((t: any) => ({
        date: t.month,
        value: (t.valueMinor || 0) / 100
      })) || [],
      byRegion: data.revenueByRegion?.map((r: any) => ({
        region: r.region,
        value: (r.revenueMinor || 0) / 100
      })) || [],
      byPlan: data.revenueByPlan?.map((p: any) => ({
        plan: p.planName,
        value: (p.revenueMinor || 0) / 100
      })) || []
    };
  },

  getChurnAnalytics: async (): Promise<ChurnMetric> => {
    const data = await financeFetch<any>('/reports/churn');
    return {
      customerChurnRate: data.netChurnPercent || 0,
      revenueChurnRate: data.revenueChurnPercent || 0,
      churnedRevenue: (data.churnedRevenueMinor || 0) / 100,
      history: data.churnTrend?.map((t: any) => ({
        date: t.month,
        value: t.netChurnPercent || 0
      })) || [],
      reasons: data.reasons?.map((r: any) => ({
        reason: r.reason,
        count: r.count
      })) || []
    };
  },

  getARPUAnalytics: async (): Promise<ARPUAnalytics> => {
    const data = await financeFetch<any>('/reports/arpu-ltv');
    return {
      arpu: (data.arpuMinor || 0) / 100,
      ltv: (data.ltvMinor || 0) / 100,
      cacLtvRatio: data.cacLtvRatio || 'N/A',
      history: data.arpuTrend?.map((t: any) => ({
        date: t.month,
        value: (t.valueMinor || 0) / 100
      })) || []
    };
  },

  getInvoices: async (page = 1, limit = 10): Promise<{ data: Invoice[]; total: number }> => {
    const response = await financeFetch<any>(`/invoices?page=${page - 1}&size=${limit}`);
    return {
      data: response.content?.map((i: any) => ({
        id: i.invoiceNumber,
        customerId: String(i.customerId),
        customerName: `Customer ${i.customerId}`,
        amount: i.amount, // Backend already returns decimal rupees
        status: i.status,
        date: i.date,
        dueDate: i.dueDate
      })) || [],
      total: response.totalElements || 0
    };
  },

  getInvoiceDetail: async (invoiceId: string): Promise<Invoice> => {
    // Extract numeric ID from invoice number (e.g., "INV-2026-1001" -> "1001")
    const numericId = invoiceId.split('-').pop() || invoiceId;
    const response = await financeFetch<any>(`/invoices/${numericId}`);
    return {
      id: response.invoiceNumber,
      customerId: String(response.customerId),
      customerName: `Customer ${response.customerId}`,
      amount: response.amount,
      status: response.status === 'PAID' ? 'Paid' : response.status === 'DRAFT' ? 'Draft' : 'Pending',
      date: response.date,
      dueDate: response.dueDate,
      lineItems: response.lineItems?.map((item: any) => ({
        lineItemId: String(item.lineItemId),
        description: item.description,
        lineType: item.lineType,
        quantity: item.quantity,
        unitPriceMinor: item.unitPriceMinor,
        amountMinor: item.amountMinor,
        periodStart: item.periodStart,
        periodEnd: item.periodEnd
      })) || []
    };
  },

  getPayments: async (page = 1, limit = 10): Promise<{ data: Payment[]; total: number }> => {
    const response = await financeFetch<any>(`/payments?page=${page - 1}&size=${limit}`);
    return {
      data: response.content?.map((p: any) => ({
        id: String(p.paymentId),
        invoiceId: p.invoiceNumber,
        customerId: 'N/A',
        amount: p.amount, // Backend returns decimal rupees
        status: p.status,
        method: p.paymentMethod,
        date: p.date
      })) || [],
      total: response.totalElements || 0
    };
  },

  getCredits: async (page = 1, limit = 10): Promise<{ data: Refund[]; total: number }> => {
    const response = await financeFetch<any>(`/refunds?page=${page - 1}&size=${limit}`);
    return {
      data: response.content?.map((r: any) => ({
        id: r.refundId,
        paymentId: String(r.paymentId),
        customerId: 'N/A',
        amount: r.amount, // Backend returns decimal rupees
        status: r.status ,
        reason: r.reason || 'No reason provided',
        date: r.date
      })) || [],
      total: response.totalElements || 0
    };
  },

  getRevenueSnapshots: async (page = 1, limit = 10): Promise<{ data: RevenueSnapshot[]; total: number }> => {
    const response = await financeFetch<any>(`/snapshots?page=${page - 1}&size=${limit}`);
    return {
      data: response.content?.map((s: any, index: number) => ({
        id: `snap-${index}`,
        date: s.date,
        totalRevenue: (s.totalRevenueMinor || 0) / 100,
        mrr: (s.mrrMinor || 0) / 100,
        arr: (s.arrMinor || 0) / 100,
        activeCustomers: s.activeCustomers || 0,
        newCustomers: s.newCustomers || 0,
        netChurnPercent:Math.round(( s.netChurnPercent || 0)*100)/100,
      })) || [],
      total: response.totalElements || 0
    };
  },

  exportFinanceReport: async (type: string, format: 'CSV' | 'PDF'): Promise<Blob> => {
    const response = await fetch(`${API_BASE}/reports/export?type=${encodeURIComponent(type)}&format=${format.toLowerCase()}`, {
      method: 'GET',
      headers: {
        'Accept': format === 'PDF' ? 'application/pdf' : 'text/csv',
        'Authorization': `Bearer ${localStorage.getItem('token')}` // Ensure auth if needed
      },
    });
    if (!response.ok) {
      throw new Error('Export failed');
    }
    return await response.blob();
  },

  getExports: async () => {
    return []; // Optional: Create an endpoint for this if needed, returning empty for now
  }
};
