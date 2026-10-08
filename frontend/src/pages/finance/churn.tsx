import React, { useEffect, useState } from 'react';
import { TrendingDown, Users, DollarSign, ShieldCheck } from 'lucide-react';
import { RevenueTrendChart } from '../../components/finance/charts/revenueTrendchart';
import { AnalyticsChartCard } from '../../components/finance/components/analyticsChartCards';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { financeService } from '../../services/finance/financeService';
import type { ChurnMetric } from '../../types/financeTypes';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';

export const ChurnAnalyticsPage: React.FC = () => {
  const [data, setData] = useState<ChurnMetric | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await financeService.getChurnAnalytics();
        setData(result);
      } catch (error) {
        console.error('Error fetching Churn data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading || !data) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading Churn data...</div>;

  return (
    <>
      <PageHeader subtitle="Analyze customer and revenue retention." />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '32px' }}>
        <FinanceMetricCard title="Customer Churn Rate" value={data.customerChurnRate} icon={<Users />} suffix="%" />
        <FinanceMetricCard title="Revenue Churn Rate" value={data.revenueChurnRate} icon={<TrendingDown />} suffix="%" />
        <FinanceMetricCard title="Churned Revenue" value={data.churnedRevenue} icon={<DollarSign />} prefix="₹" />
        <FinanceMetricCard title="Net Revenue Retention" value={Math.max(0, 100 - data.revenueChurnRate)} icon={<ShieldCheck />} suffix="%" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
        <AnalyticsChartCard title="Churn Rate Trend">
          <RevenueTrendChart
            data={data.history.map(d => ({ date: d.date, value: d.value }))}
            color="#ef4444"
            valuePrefix=""
          />
        </AnalyticsChartCard>
      </div>
    </>
  );
};
