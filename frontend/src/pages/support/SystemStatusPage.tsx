import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { DataTable } from '../../components/admin/shared/DataTable';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { getBillingJobs, getDunningLogs, type BillingJob, type DunningRetryLog } from '../../services/support/supportService';

export const SystemStatusPage: React.FC = () => {
  const [jobs, setJobs] = useState<BillingJob[]>([]);
  const [logs, setLogs] = useState<DunningRetryLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('jobs');

  useEffect(() => {
    Promise.all([getBillingJobs(), getDunningLogs()])
      .then(([jobsData, logsData]) => {
        setJobs(jobsData);
        setLogs(logsData);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const jobColumns = [
    { key: 'createdAt', header: 'Triggered', render: (r: BillingJob) => new Date(r.createdAt).toLocaleString() },
    { key: 'jobType', header: 'Type' },
    { key: 'status', header: 'Status', render: (r: BillingJob) => <StatusBadge status={r.status} /> },
    { key: 'totalRecords', header: 'Total' },
    { key: 'successCount', header: 'Success' },
    { key: 'failureCount', header: 'Failure' },
    { 
      key: 'successRate', 
      header: 'Success Rate', 
      render: (r: BillingJob) => {
        if (r.totalRecords === 0) return 'N/A';
        const rate = Math.round((r.successCount / r.totalRecords) * 100);
        return <span style={{ color: rate === 100 ? '#10b981' : rate > 80 ? '#f59e0b' : '#ef4444', fontWeight: 600 }}>{rate}%</span>;
      } 
    },
  ];

  const dunningColumns = [
    { key: 'scheduledAt', header: 'Scheduled', render: (r: DunningRetryLog) => new Date(r.scheduledAt).toLocaleString() },
    { key: 'invoiceId', header: 'Invoice ID' },
    { key: 'attemptNo', header: 'Attempt' },
    { key: 'status', header: 'Status', render: (r: DunningRetryLog) => <StatusBadge status={r.status} /> },
    { key: 'failureReason', header: 'Reason' },
  ];

  return (
    <>
      <PageHeader subtitle="Monitor automated billing jobs and payment retries." />
      
      <div style={{ padding: '0 40px 40px 40px' }}>
        {/* Tabs */}
        <div style={{ display: 'flex', borderBottom: '1px solid #e2e8f0', marginBottom: '20px' }}>
          {['jobs', 'dunning'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                padding: '10px 20px',
                background: 'transparent',
                border: 'none',
                borderBottom: activeTab === tab ? '2px solid #0f766e' : '2px solid transparent',
                color: activeTab === tab ? '#0f766e' : '#64748b',
                fontWeight: 600,
                cursor: 'pointer',
                textTransform: 'capitalize',
              }}
            >
              {tab === 'jobs' ? 'Billing Jobs' : 'Dunning Retries'}
            </button>
          ))}
        </div>

        <div className="data-panel" style={{ padding: '24px' }}>
          {loading ? (
            <div style={{ color: '#9ca3af', textAlign: 'center' }}>Loading data...</div>
          ) : activeTab === 'jobs' ? (
            <DataTable columns={jobColumns} data={jobs} emptyMessage="No billing jobs found." />
          ) : (
            <DataTable columns={dunningColumns} data={logs} emptyMessage="No dunning logs found." />
          )}
        </div>
      </div>
    </>
  );
};
