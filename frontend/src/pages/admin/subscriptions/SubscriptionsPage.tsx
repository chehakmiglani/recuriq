import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { getSubscriptions } from '../../../services/admin/adminService';
import type { SubscriptionResponse } from '../../../services/admin/adminTypes';

const columns = [
  { key: 'customerName', header: 'Customer', render: (r: SubscriptionResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.customerName}</span> },
  { key: 'planName', header: 'Plan' },
  { key: 'startDate', header: 'Start Date' },
  { key: 'currentPeriodEnd', header: 'Period End' },
  { key: 'currency', header: 'Currency' },
  { key: 'status', header: 'Status', render: (r: SubscriptionResponse) => <StatusBadge status={r.status} /> },
];

export const SubscriptionsPage: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<SubscriptionResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSubscriptions()
      .then(setSubscriptions)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading subscriptions...</div>;

  return (
    <>
      <PageHeader subtitle="View customer subscriptions (read-only)." />
      <div className="data-panel">
        <DataTable columns={columns} data={subscriptions} emptyMessage="No subscriptions yet." />
      </div>
    </>
  );
};
