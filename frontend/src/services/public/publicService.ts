// API base URL — proxied to backend by both CRA (setupProxy.js) and Vite (vite.config.ts)
const API_BASE_URL = '/api';
export interface PlanInfo {
  planId: number;
  name: string;
  billingPeriod: 'MONTHLY' | 'YEARLY';
  defaultPriceMinor: number;
  defaultCurrency: string;
  trialDays: number;
  setupFeeMinor: number;
  taxMode: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: string;
  productName: string;
}
async function publicFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || 'Request failed');
  }
  const text = await response.text();
  return text ? JSON.parse(text) : ({} as T);
}
export const publicService = {
  // Fetch all active plans for public display
  getPublicPlans: async (region?: string): Promise<PlanInfo[]> => {
    try {
      const query = region ? `?region=${region}` : '';
      return await publicFetch<PlanInfo[]>(`/customer/plans/all${query}`);
    } catch (error) {
      console.error('Failed to fetch plans:', error);
      // Return fallback plans if API fails
      return [];
    }
  },
};
