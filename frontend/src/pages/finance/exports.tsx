import React, { useEffect, useState } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { Download } from 'lucide-react';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { type Column, FinanceDataTable } from '../../components/finance/components/finanaceDataTable';
import { financeService } from '../../services/finance/financeService';
import type { ExportJob } from '../../types/financeTypes';

export const ExportsPage: React.FC = () => {
  const [data, setData] = useState<ExportJob[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const result = await financeService.getExports();
        setData(result);
      } catch (error) {
        console.error('Error fetching exports:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const columns: Column<ExportJob>[] = [
    { header: 'Job ID', accessor: (row) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{row.id}</span> },
    { header: 'Report Type', accessor: 'type' },
    { header: 'Format', accessor: 'format' },
    { header: 'Date Requested', accessor: (row) => new Date(row.date).toLocaleString() },
    { header: 'Status', accessor: (row) => <StatusBadge status={row.status} /> },
    {
      header: 'Actions',
      accessor: (row) => (
        row.status === 'Completed' ? (
          <div className="table-actions">
            <button className="btn-admin-secondary" style={{ padding: '6px 12px', fontSize: '12px', gap: '4px' }}>
              <Download size={14} /> Download
            </button>
          </div>
        ) : (
          <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>-</span>
        )
      )
    },
  ];

  return (
    <>
      <PageHeader subtitle="History of generated financial reports." />

      {loading ? (
        <div>Loading exports...</div>
      ) : (
        <FinanceDataTable columns={columns} data={data} keyExtractor={(row) => row.id} />
      )}
    </>
  );
};
