import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';

interface Props {
  data: Array<{ name: string; value: number }>;
  colors?: string[];
  valuePrefix?: string;
}

const DEFAULT_COLORS = ['#0f766e', '#a855f7', '#06b6d4', '#10b981', '#f59e0b', '#f43f5e'];

export const DonutChart: React.FC<Props> = ({ data, colors = DEFAULT_COLORS, valuePrefix = '₹' }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={55}
          outerRadius={85}
          paddingAngle={4}
          dataKey="value"
          stroke="#ffffff"
          strokeWidth={2}
          style={{ outline: 'none' }}
        >
          {data.map((_, index) => (
            <Cell key={`cell-${index}`} fill={colors[index % colors.length]} style={{ cursor: 'pointer', transition: 'all 0.2s ease-in-out' }} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{ borderRadius: '12px', border: 'none', background: '#1e293b', color: '#ffffff', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)', padding: '8px 12px' }}
          itemStyle={{ color: '#ffffff', fontSize: '13px', fontWeight: 600 }}
          formatter={(value) => [`${valuePrefix}${Number(value).toLocaleString()}`, 'Value']}
        />
        <Legend verticalAlign="bottom" height={40} iconType="circle" wrapperStyle={{ fontSize: '12px', fontWeight: 500, color: '#4b5563', paddingTop: '15px' }} />
      </PieChart>
    </ResponsiveContainer>
  );
};
