import React, { useEffect, useState } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { type Column, FinanceDataTable } from '../../components/finance/components/finanaceDataTable';
import { financeService } from '../../services/finance/financeService';
import type { Refund } from '../../types/financeTypes';
import { StatusBadge } from '../../components/shared/StatusBadge';

export const CreditsPage: React.FC = () => {
  const [data, setData] = useState<Refund[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const result = await financeService.getCredits(page, 10);
        setData(result.data);
        setTotal(result.total);
      } catch (error) {
        console.error('Error fetching credits:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [page]);

  const columns: Column<Refund>[] = [
    { header: 'Refund ID', accessor: (row) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{row.id}</span> },
    { header: 'Payment ID', accessor: 'paymentId' },
    { header: 'Amount', accessor: (row) => `₹${row.amount.toFixed(2)}` },
    { header: 'Reason', accessor: 'reason' },
    { header: 'Date', accessor: (row) => new Date(row.date).toLocaleDateString() },
    { header: 'Status', accessor: (row) => <StatusBadge status={row.status} /> },
  ];

  return (
    <>
      <PageHeader subtitle="Manage customer refunds and credit notes." />

      {loading ? (
        <div>Loading credits...</div>
      ) : (
        <>
          <FinanceDataTable columns={columns} data={data} keyExtractor={(row) => row.id} />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
              Showing {(page - 1) * 10 + 1} to {Math.min(page * 10, total)} of {total} entries
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
                className="btn-admin-sm"
                style={{ background: '#fff', border: '1px solid #e5e7eb', color: page === 1 ? '#9ca3af' : '#1f2937' }}
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={page * 10 >= total}
                className="btn-admin-sm"
                style={{ background: '#fff', border: '1px solid #e5e7eb', color: page * 10 >= total ? '#9ca3af' : '#1f2937' }}
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </>
  );
};
