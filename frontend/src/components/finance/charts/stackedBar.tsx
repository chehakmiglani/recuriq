import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

interface Props {
  data: any[];
  bars: Array<{ key: string; color: string; name: string }>;
  valuePrefix?: string;
}

export const StackedBarChart: React.FC<Props> = ({ data, bars, valuePrefix = '₹' }) => {
  const formattedData = data.map(item => ({
    ...item,
    formattedDate: new Date(item.date).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
  }));

  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={formattedData} margin={{ top: 20, right: 15, left: -10, bottom: 5 }} barSize={32}>
        <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#f3f4f6" />
        <XAxis dataKey="formattedDate" axisLine={false} tickLine={false} tick={{ fontSize: 12, fontWeight: 500, fill: '#9ca3af' }} dy={10} />
        <YAxis 
          axisLine={false} 
          tickLine={false} 
          tick={{ fontSize: 12, fontWeight: 500, fill: '#9ca3af' }} 
          tickFormatter={(val) => `${valuePrefix}${val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val}`} 
        />
        <Tooltip
          contentStyle={{ borderRadius: '12px', border: 'none', background: '#1e293b', color: '#ffffff', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)', padding: '8px 12px' }}
          labelStyle={{ color: '#94a3b8', fontSize: '11px', fontWeight: 500, marginBottom: '4px' }}
          itemStyle={{ fontSize: '13px', fontWeight: 500 }}
          formatter={(value: any, name: any) => [`${valuePrefix}${Number(value).toLocaleString()}`, name]}
        />
        <Legend verticalAlign="bottom" height={40} iconType="circle" wrapperStyle={{ fontSize: '12px', fontWeight: 500, color: '#4b5563', paddingTop: '20px' }} />
        {bars.map((bar, idx) => (
          <Bar 
            key={bar.key} 
            dataKey={bar.key} 
            name={bar.name} 
            stackId="a" 
            fill={bar.color} 
            radius={idx === bars.length - 1 ? [6, 6, 0, 0] : [0, 0, 0, 0]}
            maxBarSize={40}
          />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
};
