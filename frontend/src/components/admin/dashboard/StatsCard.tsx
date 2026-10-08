import React from 'react';
import { Link } from 'react-router-dom';

interface StatsCardProps {
  label: string;
  value: string;
  change?: string;
  trend?: 'up' | 'down';
  icon: React.ReactNode;
  to?: string;
}

export const StatsCard: React.FC<StatsCardProps> = ({ label, value, change, trend, icon, to }) => {
  const content = (
    <>
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
            background: '#f0fdfa',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#0f766e',
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
            color: trend === 'down' ? '#dc2626' : '#16a34a',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
          }}
        >
          {trend === 'down' ? '↓' : '↑'} {change}
        </span>
      )}
    </>
  );

  const containerStyle = {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
    textDecoration: 'none',
  };

  if (to) {
    return (
      <Link to={to} className="stat-card" style={containerStyle}>
        {content}
      </Link>
    );
  }

  return (
    <div className="stat-card" style={containerStyle}>
      {content}
    </div>
  );
};
