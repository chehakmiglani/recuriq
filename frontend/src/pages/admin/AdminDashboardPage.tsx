import React, { useState, useEffect } from 'react';
import { Package, Layers, TicketPercent, Puzzle, Percent, BookOpen, Users, UserCog } from 'lucide-react';
import { Link } from 'react-router-dom';
import { StatsCard } from '../../components/admin/dashboard/StatsCard';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { getDashboardStats, getAllProducts, getAllPlans } from '../../services/admin/adminService';
import type { DashboardStats, Product, PlanResponse } from '../../services/admin/adminTypes';

export const AdminDashboardPage: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        const [statsData, productsData, plansData] = await Promise.all([
          getDashboardStats(),
          getAllProducts(),
          getAllPlans()
        ]);
        setStats(statsData);
        setProducts(productsData.slice(0, 5)); // Top 5 products
        setPlans(plansData.slice(0, 4)); // Top 4 plans
      } catch (error) {
        console.error('Failed to load dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };
    loadDashboardData();
  }, []);

  if (loading) {
    return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading dashboard...</div>;
  }

  const formatPrice = (minor: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(minor / 100);
  };

  return (
    <>
      {/* Header */}
      <PageHeader 
        subtitle="Overview of your platform catalog and users" 
      />

      {/* Stats Grid - Row 1: Catalog Management */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '20px' }}>
        <StatsCard
          label="Total Products"
          value={String(stats?.totalProducts ?? 0)}
          icon={<Package size={20} />}
          to="/admin/product"
        />
        <StatsCard
          label="Total Plans"
          value={String(stats?.totalPlans ?? 0)}
          icon={<Layers size={20} />}
          to="/admin/plans"
        />
        <StatsCard
          label="Active Coupons"
          value={String(stats?.activeCoupons ?? 0)}
          icon={<TicketPercent size={20} />}
          to="/admin/coupons"
        />
        <StatsCard
          label="Total Add-ons"
          value={String(stats?.totalAddOns ?? 0)}
          icon={<Puzzle size={20} />}
          to="/admin/addons"
        />
      </div>

      {/* Stats Grid - Row 2: Tax, Price Books, Users */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginBottom: '32px' }}>
        <StatsCard
          label="Active Tax Rates"
          value={String(stats?.activeTaxRates ?? 0)}
          icon={<Percent size={20} />}
          to="/admin/taxrates"
        />
        <StatsCard
          label="Total Price Books"
          value={String(stats?.totalPriceBooks ?? 0)}
          icon={<BookOpen size={20} />}
          to="/admin/pricebooks"
        />
        <StatsCard
          label="Total Customers"
          value={String(stats?.totalCustomers ?? 0)}
          icon={<Users size={20} />}
          to="/admin/customers"
        />
        <StatsCard
          label="Total Staff"
          value={String(stats?.totalStaff ?? 0)}
          icon={<UserCog size={20} />}
          to="/admin/staff"
        />
      </div>

      {/* Main Content Grid */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {/* Current Products Section */}
        <div className="data-panel" style={{ border: 'none', boxShadow: 'none', background: 'transparent' }}>
          <div className="data-panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', border: 'none', background: 'transparent' }}>
            <h2 style={{ fontFamily: "'Outfit', sans-serif", fontSize: '20px', fontWeight: 700, color: '#0f172a', margin: 0 }}>Current Products</h2>
            <Link to="/admin/product" style={{ fontSize: '14px', fontWeight: 600, color: '#0f766e', textDecoration: 'none' }}>
              View all products
            </Link>
          </div>

          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Product Name</th>
                  <th style={{ textAlign: 'center' }}>Status</th>
                  <th style={{ textAlign: 'center' }}>Plans Count</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id}>
                    <td style={{ fontWeight: 600, color: '#1e293b' }}>
                      {product.name}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <StatusBadge status={product.status} />
                    </td>
                    <td style={{ textAlign: 'center', color: '#64748b', fontSize: '13px' }}>
                      {product.plansCount}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Link to="/admin/product" className="btn-admin-sm btn-toggle-inactive" style={{ textDecoration: 'none' }}>
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        {/* Featured Plans Section */}
        <div className="data-panel" style={{ border: 'none', boxShadow: 'none', background: 'transparent' }}>
          <div className="data-panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', border: 'none', background: 'transparent' }}>
            <h2 style={{ fontFamily: "'Outfit', sans-serif", fontSize: '20px', fontWeight: 700, color: '#0f172a', margin: 0 }}>Featured Plans</h2>
            <Link to="/admin/plans" style={{ fontSize: '14px', fontWeight: 600, color: '#0f766e', textDecoration: 'none' }}>
              View all {stats?.totalPlans ?? 0} plans
            </Link>
          </div>
          <div style={{ padding: '12px 0', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
            {plans.map((plan) => (
              <div key={plan.id} style={{
                background: '#ffffff',
                border: '1px solid #e5e7eb',
                borderRadius: '12px',
                padding: '20px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between'
              }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                    <h3 style={{ fontFamily: 'Inter, sans-serif', fontSize: '16px', fontWeight: 600, color: '#1f2937', margin: 0 }}>
                      {plan.name}
                    </h3>
                    <span style={{
                      fontSize: '10px',
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      padding: '4px 8px',
                      borderRadius: '6px',
                      backgroundColor: 'var(--info-bg)',
                      color: 'var(--info-text)',
                      fontFamily: 'Inter, sans-serif'
                    }}>
                      {plan.billingPeriod}
                    </span>
                  </div>
                  <p style={{
                    fontFamily: 'Inter, sans-serif',
                    fontSize: '14px',
                    color: '#6b7280',
                    margin: '0 0 16px 0'
                  }}>
                    Billed {plan.billingPeriod.toLowerCase()}
                  </p>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <span style={{
                      fontFamily: 'Inter, sans-serif',
                      fontSize: '28px',
                      fontWeight: 700,
                      color: '#1f2937'
                    }}>
                      {formatPrice(plan.defaultPriceMinor, plan.defaultCurrency)}
                    </span>
                    <StatusBadge status={plan.status} />
                  </div>
                </div>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr',
                  gap: '12px',
                  paddingTop: '16px',
                  borderTop: '1px solid #e5e7eb'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <p style={{
                      fontFamily: 'Inter, sans-serif',
                      fontSize: '12px',
                      color: '#6b7280',
                      margin: 0,
                      fontWeight: 500
                    }}>Trial Period</p>
                    <p style={{
                      fontFamily: 'Inter, sans-serif',
                      fontSize: '14px',
                      fontWeight: 600,
                      color: '#374151',
                      margin: 0
                    }}>{plan.trialDays} Days</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
};
