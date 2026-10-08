import React from 'react';

interface StatsCardFinanceProps {
  label: string;
  value: string;
  change?: string;
  trend?: 'up' | 'down';
  icon: React.ReactNode;
}

export const StatsCardFinance: React.FC<StatsCardFinanceProps> = ({ label, value, change, trend, icon }) => {
  return (
    <div
      style={{
        background: '#ffffff',
        border: '1px solid #e5e7eb',
        borderRadius: '12px',
        padding: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
        }}
      >
        <span
          style={{
            fontFamily: 'Inter, sans-serif',
            fontSize: '13px',
            fontWeight: 500,
            color: '#6b7280',
            textTransform: 'uppercase',
            letterSpacing: '0.03em',
          }}
        >
          {label}
        </span>
        <div
          style={{
            width: '36px',
            height: '36px',
            borderRadius: '8px',
            background: '#ecfdf5',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#10b981',
          }}
        >
          {icon}
        </div>
      </div>
      <div
        style={{
          fontFamily: 'Inter, sans-serif',
          fontSize: '28px',
          fontWeight: 700,
          color: '#1f2937',
          lineHeight: 1,
        }}
      >
        {value}
      </div>
      {change && (
        <span
          style={{
            fontFamily: 'Inter, sans-serif',
            fontSize: '12px',
            fontWeight: 500,
            color: trend === 'down' ? '#dc2626' : '#10b981',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
          }}
        >
          {trend === 'down' ? '↓' : '↑'} {change}
        </span>
      )}
    </div>
  );
};
