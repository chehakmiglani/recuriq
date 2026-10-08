import React from 'react';
import { Database } from 'lucide-react';

interface Column<T> {
  key: string;
  header: string;
  render?: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  emptyMessage?: string;
  onRowClick?: (row: T) => void;
}

export function DataTable<T extends Record<string, any>>({ 
  columns, 
  data, 
  emptyMessage = 'No data found',
  onRowClick 
}: DataTableProps<T>) {
  if (data.length === 0) {
    return (
      <div className="empty-state">
        <Database size={40} className="empty-state-icon" />
        <div className="empty-state-text">{emptyMessage}</div>
      </div>
    );
  }

  return (
    <table className="admin-table">
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key}>{col.header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row, idx) => (
          <tr 
            key={idx} 
            onClick={() => onRowClick?.(row)}
            style={onRowClick ? { cursor: 'pointer' } : undefined}
          >
            {columns.map((col) => (
              <td key={col.key}>
                {col.render ? col.render(row) : row[col.key]}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
