import React, { useEffect, useState } from 'react';
import { TrendingUp } from 'lucide-react';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';
import { RevenueTrendChart } from '../../components/finance/charts/revenueTrendchart';
import { AnalyticsChartCard } from '../../components/finance/components/analyticsChartCards';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { financeService } from '../../services/finance/financeService';
import type { MRRAnalytics } from '../../types/financeTypes';

export const MRRAnalyticsPage: React.FC = () => {
  const [data, setData] = useState<MRRAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await financeService.getMRRAnalytics();
        setData(result);
      } catch (error) {
        console.error('Error fetching MRR data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading || !data) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading MRR data...</div>;

  return (
    <>
      <PageHeader subtitle="Monthly Recurring Revenue breakdown and trends." />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '20px', marginBottom: '32px' }}>
        <FinanceMetricCard title="Total MRR" value={data.total} icon={<TrendingUp />} prefix="₹" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
        <AnalyticsChartCard title="MRR Trend Over Time">
          <RevenueTrendChart data={data.history} color="#10b981" />
        </AnalyticsChartCard>
      </div>
    </>
  );
};
