const API_BASE = '/api/ai';

export interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
}

export interface AssistantResponse {
  answer: string;
  toolsUsed: string[];
}

export type RiskLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface ChurnRisk {
  customerId: number;
  customerName: string | null;
  email: string | null;
  country: string | null;
  planName: string;
  subscriptionStatus: string;
  monthlyValueMinor: number;
  currency: string | null;
  score: number;
  level: RiskLevel;
  reasons: string[];
}

export interface ChurnRiskSummary {
  highRisk: number;
  mediumRisk: number;
  lowRisk: number;
  monthlyRevenueAtRiskMinor: number;
  customers: ChurnRisk[];
}

export interface ForecastPoint {
  month: string;
  valueMinor: number;
}

export interface Insights {
  mrrForecast: ForecastPoint[];
  currentMrrMinor: number;
  projectedGrowthPercent: number | null;
  highRisk: number;
  mediumRisk: number;
  monthlyRevenueAtRiskMinor: number;
  topRisks: ChurnRisk[];
  aiBriefing: string | null;
  aiBriefingGeneratedAt: string | null;
}

async function aiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  });

  if (!res.ok) {
    const errorText = await res.text();
    let errorMessage = `Request failed: ${res.status}`;
    try {
      errorMessage = JSON.parse(errorText).message || errorMessage;
    } catch {
      errorMessage = errorText || errorMessage;
    }
    throw new Error(errorMessage);
  }
  return res.json();
}

export const aiService = {
  getStatus: () => aiFetch<{ assistantEnabled: boolean }>('/status'),

  ask: (question: string, history: ChatTurn[]) =>
    aiFetch<AssistantResponse>('/ask', {
      method: 'POST',
      body: JSON.stringify({ question, history }),
    }),

  getInsights: (refresh = false) =>
    aiFetch<Insights>(`/insights${refresh ? '?refresh=true' : ''}`),

  getChurnRisk: (level?: RiskLevel) =>
    aiFetch<ChurnRiskSummary>(`/churn-risk${level ? `?level=${level}` : ''}`),
};
