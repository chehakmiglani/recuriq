import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { getCustomers } from '../../../services/admin/adminService';
import type { CustomerResponse } from '../../../services/admin/adminTypes';

export const CustomersPage: React.FC = () => {
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => { getCustomers().then(setCustomers).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);

  const columns = [
    { key: 'fullName', header: 'Customer', render: (r: CustomerResponse) => (
      <div>
        <div style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.fullName}</div>
        <div style={{ fontSize: '13px', color: '#6b7280' }}>{r.email}</div>
      </div>
    )},
    { key: 'country', header: 'Country' },
    { key: 'currency', header: 'Currency' },
    { key: 'createdAt', header: 'Joined', render: (r: CustomerResponse) => r.createdAt?.split('T')[0] ?? '-' },
    { key: 'status', header: 'Status', render: (r: CustomerResponse) => <StatusBadge status={r.status} /> },
  ];

  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading customers...</div>;

  return (
    <>
      <PageHeader subtitle="View customer accounts (read-only)." />
      <div className="data-panel">
        <DataTable columns={columns} data={customers} emptyMessage="No customers registered yet." />
      </div>
    </>
  );
};
