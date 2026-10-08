import React from 'react';

interface Props {
  title: string;
  children: React.ReactNode;
  action?: React.ReactNode;
}

export const AnalyticsChartCard: React.FC<Props> = ({ title, children, action }) => {
  return (
    <div className="data-panel" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div className="data-panel-header">
        <h3 className="data-panel-title">{title}</h3>
        {action && <div>{action}</div>}
      </div>
      <div style={{ padding: '24px', flex: 1, minHeight: '300px' }}>
        {children}
      </div>
    </div>
  );
};
