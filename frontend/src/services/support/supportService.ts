import { fetchWithSession } from '../auth/authService';
import type { CustomerProfile, Subscription, Invoice, UsageRecord, Notification, CreditNote, CancellationRequest } from '../customer/customerService';

const API_BASE = '/support';

// ==================== TYPES ====================

export interface CustomerSearchResponse {
  customerId: number;
  fullName: string;
  email: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export interface CustomerDetailResponse {
  customerProfile: CustomerProfile;
  subscriptions: Subscription[];
  invoices: Invoice[];
  usageRecords: UsageRecord[];
  notifications: Notification[];
  creditNotes: CreditNote[];
}

export interface AuditLog {
  id: number;
  actor: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId: number;
  oldValue: string;
  newValue: string;
  requestId: string;
  ip: string;
  createdAt: string;
}

export interface BillingJob {
  id: number;
  jobType: string;
  status: string;
  triggeredBy: string;
  startedAt: string;
  completedAt: string;
  totalRecords: number;
  successCount: number;
  failureCount: number;
  errorSummary: string;
  createdAt: string;
}

export interface DunningRetryLog {
  id: number;
  invoiceId: number;
  paymentId: number;
  attemptNo: number;
  scheduledAt: string;
  attemptedAt: string;
  status: string;
  failureReason: string;
}

// ==================== SERVICES ====================

export const searchCustomers = async (query: string): Promise<CustomerSearchResponse[]> => {
  const response = await fetchWithSession(`${API_BASE}/customers?query=${encodeURIComponent(query)}`);
  if (!response.ok) throw new Error('Failed to search customers');
  return response.json();
};

export const getCustomerDetails = async (id: number): Promise<CustomerDetailResponse> => {
  const response = await fetchWithSession(`${API_BASE}/customers/${id}`);
  if (!response.ok) throw new Error('Failed to fetch customer details');
  return response.json();
};

export const getRecentAuditLogs = async (): Promise<AuditLog[]> => {
  const response = await fetchWithSession(`${API_BASE}/audit-logs`);
  if (!response.ok) throw new Error('Failed to fetch audit logs');
  return response.json();
};

export const getBillingJobs = async (): Promise<BillingJob[]> => {
  const response = await fetchWithSession(`${API_BASE}/billing-jobs`);
  if (!response.ok) throw new Error('Failed to fetch billing jobs');
  return response.json();
};

export const getDunningLogs = async (): Promise<DunningRetryLog[]> => {
  const response = await fetchWithSession(`${API_BASE}/dunning-logs`);
  if (!response.ok) throw new Error('Failed to fetch dunning logs');
  return response.json();
};

export const getPastDueSubscriptions = async (): Promise<Subscription[]> => {
  const response = await fetchWithSession(`${API_BASE}/subscriptions/past-due`);
  if (!response.ok) throw new Error('Failed to fetch past due subscriptions');
  return response.json();
};

export const getPendingCancellationRequests = async (): Promise<CancellationRequest[]> => {
  const response = await fetchWithSession(`${API_BASE}/cancellation-requests`);
  if (!response.ok) throw new Error('Failed to fetch pending cancellation requests');
  return response.json();
};

export const approveCancellationRequest = async (requestId: number, agentNotes: string): Promise<any> => {
  const response = await fetchWithSession(`${API_BASE}/cancellation-requests/${requestId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ agentNotes }),
  });
  if (!response.ok) throw new Error('Failed to approve cancellation request');
  return response.json();
};

export const rejectCancellationRequest = async (requestId: number, agentNotes: string): Promise<void> => {
  const response = await fetchWithSession(`${API_BASE}/cancellation-requests/${requestId}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ agentNotes }),
  });
  if (!response.ok) throw new Error('Failed to reject cancellation request');
};
