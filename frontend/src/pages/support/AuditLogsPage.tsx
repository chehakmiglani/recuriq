import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { DataTable } from '../../components/admin/shared/DataTable';
import { getRecentAuditLogs, type AuditLog } from '../../services/support/supportService';

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getRecentAuditLogs()
      .then(setLogs)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { key: 'createdAt', header: 'Time', render: (r: AuditLog) => new Date(r.createdAt).toLocaleString() },
    { key: 'actor', header: 'Actor' },
    { key: 'action', header: 'Action' },
    { key: 'entityType', header: 'Entity' },
    { key: 'entityId', header: 'Entity ID' },
  ];

  return (
    <>
      <PageHeader subtitle="View recent system activities and changes." />
      <div className="data-panel" style={{ padding: '40px' }}>
        {loading ? (
          <div style={{ color: '#9ca3af', textAlign: 'center' }}>Loading logs...</div>
        ) : (
          <DataTable columns={columns} data={logs} emptyMessage="No audit logs found." />
        )}
      </div>
    </>
  );
};
