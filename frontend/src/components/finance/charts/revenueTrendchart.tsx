import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

interface Props {
  data: Array<{ date: string; value: number }>;
  forecast?: Array<{ date: string; value: number }>;   // drawn as a dashed continuation
  color?: string;
  valuePrefix?: string;
}

const formatMonth = (date: string) => new Date(date).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });

export const RevenueTrendChart: React.FC<Props> = ({ data, forecast = [], color = '#0f766e', valuePrefix = '₹' }) => {
  const formattedData: Array<{ formattedDate: string; value?: number; forecast?: number }> = data.map(item => ({
    value: item.value,
    formattedDate: formatMonth(item.date),
  }));
  if (forecast.length > 0 && formattedData.length > 0) {
    // Start the dashed line at the last real point so the two lines join up
    formattedData[formattedData.length - 1].forecast = formattedData[formattedData.length - 1].value;
    forecast.forEach(item => formattedData.push({ forecast: item.value, formattedDate: formatMonth(item.date) }));
  }

  // Unique linear gradient ID based on color to prevent collision across multiple charts
  const gradientId = `colorValue-${color.replace('#', '')}`;
  const shadowId = `shadow-${color.replace('#', '')}`;

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={formattedData} margin={{ top: 15, right: 15, left: -10, bottom: 5 }}>
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={color} stopOpacity={0.45} />
            <stop offset="95%" stopColor={color} stopOpacity={0.01} />
          </linearGradient>
          <filter id={shadowId} x="-5%" y="-5%" width="110%" height="115%">
            <feDropShadow dx="0" dy="8" stdDeviation="6" floodColor={color} floodOpacity="0.22" />
          </filter>
        </defs>
        <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#f3f4f6" />
        <XAxis dataKey="formattedDate" axisLine={false} tickLine={false} tick={{ fontSize: 12, fontWeight: 500, fill: '#9ca3af' }} dy={10} />
        <YAxis 
          axisLine={false} 
          tickLine={false} 
          tick={{ fontSize: 12, fontWeight: 500, fill: '#9ca3af' }} 
          tickFormatter={(val) => `${valuePrefix}${val >= 100000 ? (val / 1000).toFixed(0) + 'k' : val >= 1000 ? (val / 1000).toFixed(1) + 'k' : val}`} 
        />
        <Tooltip
          contentStyle={{ borderRadius: '12px', border: 'none', background: '#1e293b', color: '#ffffff', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)', padding: '8px 12px' }}
          labelStyle={{ color: '#94a3b8', fontSize: '11px', fontWeight: 500, marginBottom: '4px' }}
          itemStyle={{ color: '#ffffff', fontSize: '13px', fontWeight: 600 }}
          formatter={(value, name) => [`${valuePrefix}${Number(value).toLocaleString()}`, name === 'forecast' ? 'Forecast' : 'Amount']}
        />
        <Area 
          type="monotone" 
          dataKey="value" 
          stroke={color} 
          fillOpacity={1} 
          fill={`url(#${gradientId})`} 
          strokeWidth={3.5} 
          filter={`url(#${shadowId})`}
          activeDot={{ r: 6, stroke: '#ffffff', strokeWidth: 2, fill: color }}
        />
        {forecast.length > 0 && (
          <Area
            type="monotone"
            dataKey="forecast"
            stroke={color}
            strokeDasharray="6 6"
            strokeWidth={2.5}
            fill="none"
            dot={false}
            activeDot={{ r: 5, stroke: '#ffffff', strokeWidth: 2, fill: color }}
          />
        )}
      </AreaChart>
    </ResponsiveContainer>
  );
};
