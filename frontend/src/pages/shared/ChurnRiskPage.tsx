import React, { useEffect, useState } from 'react';
import { ShieldAlert, AlertTriangle, ShieldCheck, IndianRupee } from 'lucide-react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';
import { aiService } from '../../services/ai/aiService';
import type { ChurnRiskSummary, RiskLevel } from '../../services/ai/aiService';

const LEVEL_STYLES: Record<RiskLevel, { color: string; background: string }> = {
  HIGH: { color: '#b91c1c', background: '#fee2e2' },
  MEDIUM: { color: '#b45309', background: '#fef3c7' },
  LOW: { color: '#15803d', background: '#dcfce7' },
};

const FILTERS: { label: string; value?: RiskLevel }[] = [
  { label: 'All' },
  { label: 'High', value: 'HIGH' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Low', value: 'LOW' },
];

const formatMoney = (minor: number, currency: string | null) =>
  `${currency && currency !== 'INR' ? currency + ' ' : '₹'}${(minor / 100).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;

export const ChurnRiskPage: React.FC = () => {
  const [data, setData] = useState<ChurnRiskSummary | null>(null);
  const [level, setLevel] = useState<RiskLevel | undefined>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    aiService.getChurnRisk(level)
      .then((result) => { setData(result); setError(null); })
      .catch((e: Error) => setError(e.message));
  }, [level]);

  if (error) return <div style={{ color: '#ef4444', padding: '40px', textAlign: 'center' }}>{error}</div>;
  if (!data) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Scoring customers...</div>;

  return (
    <>
      <PageHeader subtitle="Customers most likely to cancel, scored from payment, invoice, usage and cancellation signals." />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '32px' }}>
        <FinanceMetricCard title="High Risk" value={data.highRisk} icon={<ShieldAlert />} />
        <FinanceMetricCard title="Medium Risk" value={data.mediumRisk} icon={<AlertTriangle />} />
        <FinanceMetricCard title="Low Risk" value={data.lowRisk} icon={<ShieldCheck />} />
        <FinanceMetricCard title="Monthly Revenue at Risk" value={data.monthlyRevenueAtRiskMinor / 100} icon={<IndianRupee />} prefix="₹" />
      </div>

      <div className="data-panel">
        <div className="data-panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <h3 className="data-panel-title">Customers by risk</h3>
          <div style={{ display: 'flex', gap: '8px' }}>
            {FILTERS.map((f) => (
              <button
                key={f.label}
                onClick={() => setLevel(f.value)}
                style={{
                  padding: '6px 14px', borderRadius: '999px', fontSize: '13px', cursor: 'pointer',
                  border: '1px solid #e5e7eb',
                  background: level === f.value ? '#111827' : '#ffffff',
                  color: level === f.value ? '#ffffff' : '#374151',
                }}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table className="table" style={{ width: '100%', marginBottom: 0 }}>
            <thead>
              <tr>
                <th>Customer</th>
                <th>Plan</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Monthly value</th>
                <th>Risk</th>
                <th>Why</th>
              </tr>
            </thead>
            <tbody>
              {data.customers.length === 0 && (
                <tr><td colSpan={6} style={{ textAlign: 'center', color: '#9ca3af', padding: '24px' }}>No customers at this level.</td></tr>
              )}
              {data.customers.map((c) => (
                <tr key={`${c.customerId}-${c.planName}`}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{c.customerName ?? `Customer #${c.customerId}`}</div>
                    <div style={{ fontSize: '12px', color: '#6b7280' }}>{c.email}</div>
                  </td>
                  <td>{c.planName}</td>
                  <td>{c.subscriptionStatus.replace('_', ' ')}</td>
                  <td style={{ textAlign: 'right' }}>{formatMoney(c.monthlyValueMinor, c.currency)}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{
                        ...LEVEL_STYLES[c.level], padding: '2px 10px', borderRadius: '999px',
                        fontSize: '12px', fontWeight: 600,
                      }}>
                        {c.level}
                      </span>
                      <span style={{ fontVariantNumeric: 'tabular-nums', color: '#374151' }}>{c.score}</span>
                    </div>
                  </td>
                  <td>
                    <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '13px', color: '#4b5563' }}>
                      {c.reasons.map((r) => <li key={r}>{r}</li>)}
                    </ul>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
};
