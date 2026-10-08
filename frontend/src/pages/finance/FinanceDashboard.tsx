import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  TrendingUp, TrendingDown, Users, DollarSign, Activity
} from 'lucide-react';
import { DonutChart } from '../../components/finance/charts/donut';
import { RevenueTrendChart } from '../../components/finance/charts/revenueTrendchart';
import { AnalyticsChartCard } from '../../components/finance/components/analyticsChartCards';
import { ExportButton } from '../../components/finance/components/exportButton';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { financeService } from '../../services/finance/financeService';
import type { DashboardOverview } from '../../types/financeTypes';
import { FinanceMetricCard } from '../../components/finance/components/financeMetricCards';
import { AiInsightsCard } from '../../components/finance/components/AiInsightsCard';
import { aiService } from '../../services/ai/aiService';
import type { Insights } from '../../services/ai/aiService';
import './FinanceDashboard.css';

export const FinanceDashboard: React.FC = () => {
  const [data, setData] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [insights, setInsights] = useState<Insights | null>(null);

  useEffect(() => {
    // Insights load separately so a slow AI briefing never holds up the KPIs
    aiService.getInsights().then(setInsights).catch((e) => console.error('Error fetching insights:', e));
  }, []);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await financeService.getDashboardOverview();
        setData(result);
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading || !data) {
    return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading finance data...</div>;
  }

  return (
    <>
      <PageHeader subtitle="Key financial metrics and revenue analytics." />

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
        <ExportButton type="Finance Overview" />
      </div>

      {insights && <AiInsightsCard insights={insights} onRefresh={setInsights} />}

      {/* Row 1: Core Revenue Metrics */}
      <div className="finance-stat-cards-row finance-core-metrics-row">
        <Link to="/finance/mrr" className="finance-stat-card-link">
          <FinanceMetricCard title="Monthly Recurring Revenue" value={data.mrr.value} icon={<TrendingUp />} prefix="₹" className="finance-metric-card" />
        </Link>
        <Link to="/finance/arr" className="finance-stat-card-link">
          <FinanceMetricCard title="Annual Recurring Revenue" value={data.arr.value} icon={<Activity />} prefix="₹" className="finance-metric-card" />
        </Link>
        <Link to="/finance/arpu" className="finance-stat-card-link">
          <FinanceMetricCard title="Avg Revenue Per User" value={data.arpu.value} icon={<DollarSign />} prefix="₹" className="finance-metric-card" />
        </Link>
        <Link to="/finance/arpu" className="finance-stat-card-link">
          <FinanceMetricCard title="Lifetime Value" value={data.ltv.value} icon={<TrendingUp />} prefix="₹" className="finance-metric-card" />
        </Link>
      </div>

      {/* Row 2: Health Metrics */}
      <div className="finance-stat-cards-row finance-health-metrics-row">
        <Link to="/finance/churn" className="finance-stat-card-link">
          <FinanceMetricCard title="Churn Rate" value={data.churnRate.value} icon={<TrendingDown />} suffix="%" className="finance-metric-card" />
        </Link>
        <FinanceMetricCard title="Active Customers" value={data.activeCustomers.value} icon={<Users />} className="finance-metric-card" />
        <Link to="/finance/payments" className="finance-stat-card-link">
          <FinanceMetricCard title="Failed Payments" value={data.failedPayments.value} icon={<TrendingDown />} className="finance-metric-card" />
        </Link>
        <Link to="/finance/credits" className="finance-stat-card-link">
          <FinanceMetricCard title="Total Refunds" value={data.refundAmount.value} icon={<DollarSign />} prefix="₹" className="finance-metric-card" />
        </Link>
      </div>

      {/* Charts Row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' }}>
        <AnalyticsChartCard title="Revenue by Plan">
          <DonutChart data={data.revenueByPlan} />
        </AnalyticsChartCard>

        <AnalyticsChartCard title="Revenue by Region">
          <DonutChart data={data.revenueByRegion} />
        </AnalyticsChartCard>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
        <AnalyticsChartCard title="Revenue Growth (dashed: 3-month forecast)">
          <RevenueTrendChart
            data={data.revenueTrend}
            forecast={insights?.mrrForecast.map((p) => ({ date: p.month, value: p.valueMinor / 100 }))}
          />
        </AnalyticsChartCard>
      </div>
    </>
  );
};
