import React, { useEffect, useState } from 'react';
import { DollarSign, UserCheck } from 'lucide-react';
import { RevenueTrendChart } from '../../components/finance/charts/revenueTrendchart';
import { AnalyticsChartCard } from '../../components/finance/components/analyticsChartCards';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { financeService } from '../../services/finance/financeService';
import type { ARPUAnalytics } from '../../types/financeTypes';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';

export const ARPUAnalyticsPage: React.FC = () => {
  const [data, setData] = useState<ARPUAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await financeService.getARPUAnalytics();
        setData(result);
      } catch (error) {
        console.error('Error fetching ARPU data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading || !data) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading ARPU data...</div>;

  return (
    <>
      <PageHeader subtitle="Average Revenue Per User and Lifetime Value insights." />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '32px' }}>
        <FinanceMetricCard title="ARPU" value={data.arpu} icon={<DollarSign />} prefix="₹" formatter={(v) => v.toFixed(2)} />
        <FinanceMetricCard title="LTV" value={data.ltv} icon={<UserCheck />} prefix="₹" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
        <AnalyticsChartCard title="ARPU Trend">
          <RevenueTrendChart
            data={data.history.map(d => ({ date: d.date, value: d.value }))}
            color="#f59e0b"
          />
        </AnalyticsChartCard>
      </div>
    </>
  );
};
