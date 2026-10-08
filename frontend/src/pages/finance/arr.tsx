import React, { useEffect, useState } from 'react';
import { Activity, Calendar } from 'lucide-react';
import { DonutChart } from '../../components/finance/charts/donut';
import { RevenueTrendChart } from '../../components/finance/charts/revenueTrendchart';
import { AnalyticsChartCard } from '../../components/finance/components/analyticsChartCards';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { financeService } from '../../services/finance/financeService';
import type { ARRAnalytics } from '../../types/financeTypes';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';

export const ARRAnalyticsPage: React.FC = () => {
  const [data, setData] = useState<ARRAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await financeService.getARRAnalytics();
        setData(result);
      } catch (error) {
        console.error('Error fetching ARR data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading || !data) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading ARR data...</div>;

  return (
    <>
      <PageHeader subtitle="Annual Recurring Revenue and yearly forecasts." />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '32px' }}>
        <FinanceMetricCard title="Total ARR" value={data.total} icon={<Activity />} prefix="₹" />
        <FinanceMetricCard title="Monthly Equivalent" value={data.total / 12} icon={<Calendar />} prefix="₹" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px', marginBottom: '24px' }}>
        <AnalyticsChartCard title="ARR Growth History">
          <RevenueTrendChart data={data.history.map(d => ({ date: d.date, value: d.value }))} color="#0f766e" />
        </AnalyticsChartCard>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        <AnalyticsChartCard title="ARR by Region">
          <DonutChart data={data.byRegion.map(d => ({ name: d.region, value: d.value }))} />
        </AnalyticsChartCard>

        <AnalyticsChartCard title="ARR by Plan Tier">
          <DonutChart data={data.byPlan.map(d => ({ name: d.plan, value: d.value }))} />
        </AnalyticsChartCard>
      </div>
    </>
  );
};
