import React from 'react';

export interface Column<T> {
  header: string;
  accessor: keyof T | ((row: T) => React.ReactNode);
}

interface Props<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string | number;
  onRowClick?: (row: T) => void;
}

export function FinanceDataTable<T>({ columns, data, keyExtractor, onRowClick }: Props<T>) {
  const renderCell = (row: T, col: Column<T>) => {
    if (typeof col.accessor === 'function') {
      return col.accessor(row);
    }
    const value = row[col.accessor];
    return value as React.ReactNode;
  };

  return (
    <div className="data-panel">
      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              {columns.map((col, i) => {
                return (
                  <th
                    key={i}
                  >
                    {col.header}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {data.map((row) => (
              <tr
                key={keyExtractor(row)}
                onClick={() => onRowClick && onRowClick(row)}
                style={onRowClick ? { cursor: 'pointer' } : undefined}
              >
                {columns.map((col, i) => {
                  return (
                    <td
                      key={i}
                    >
                      {renderCell(row, col)}
                    </td>
                  );
                })}
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={columns.length} style={{ padding: '48px 16px', textAlign: 'center' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '12px' }}>
                    <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '50%', color: '#94a3b8' }}>
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
                        <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
                        <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
                      </svg>
                    </div>
                    <span style={{ color: '#64748b', fontSize: '14px', fontWeight: 500 }}>No data available</span>
                    <span style={{ color: '#94a3b8', fontSize: '13px' }}>There are currently no records to display.</span>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
