import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { DataTable } from '../../components/admin/shared/DataTable';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { getPastDueSubscriptions } from '../../services/support/supportService';
import type { Subscription } from '../../services/customer/customerService';

export const PastDueSubscriptionsPage: React.FC = () => {
  const [subs, setSubs] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getPastDueSubscriptions()
      .then(setSubs)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { key: 'subscriptionId', header: 'Subscription ID' },
    { key: 'customerId', header: 'Customer ID' },
    { key: 'planName', header: 'Plan' },
    { key: 'status', header: 'Status', render: (r: Subscription) => <StatusBadge status={r.status} /> },
    { key: 'currentPeriodEnd', header: 'Period End', render: (r: Subscription) => r.currentPeriodEnd ? new Date(r.currentPeriodEnd).toLocaleDateString() : '-' },
  ];

  return (
    <>
      <PageHeader subtitle="List of customers with payment issues or held subscriptions." />
      <div className="data-panel" style={{ padding: '40px' }}>
        {loading ? (
          <div style={{ color: '#9ca3af', textAlign: 'center' }}>Loading subscriptions...</div>
        ) : (
          <DataTable columns={columns} data={subs} emptyMessage="No past due subscriptions found." />
        )}
      </div>
    </>
  );
};
