import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, RefreshCw, TrendingUp, TrendingDown, ShieldAlert, ArrowRight } from 'lucide-react';
import { aiService } from '../../../services/ai/aiService';
import type { Insights } from '../../../services/ai/aiService';

const rupees = (minor: number) => `₹${Math.round(minor / 100).toLocaleString('en-IN')}`;

const fallbackBriefing = (i: Insights) => {
  const parts: string[] = [];
  if (i.projectedGrowthPercent !== null) {
    parts.push(`MRR is on track to ${i.projectedGrowthPercent >= 0 ? 'grow' : 'shrink'} about ${Math.abs(i.projectedGrowthPercent)}% over the next three months.`);
  }
  if (i.highRisk + i.mediumRisk > 0) {
    parts.push(`${i.highRisk} high-risk and ${i.mediumRisk} medium-risk customers need attention.`);
  } else {
    parts.push('No customers are currently flagged as likely to cancel.');
  }
  return parts.join(' ');
};

export const AiInsightsCard: React.FC<{ insights: Insights; onRefresh: (next: Insights) => void }> = ({ insights, onRefresh }) => {
  const [refreshing, setRefreshing] = useState(false);
  const growth = insights.projectedGrowthPercent;
  // Apply the trend's growth rate to live MRR so the figure lines up with the MRR card
  const projectedMrrMinor = growth !== null ? insights.currentMrrMinor * (1 + growth / 100) : null;

  const refresh = async () => {
    setRefreshing(true);
    try {
      onRefresh(await aiService.getInsights(true));
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div
      style={{
        display: 'grid', gridTemplateColumns: 'minmax(0, 1.6fr) minmax(0, 1fr)', gap: '24px',
        background: 'linear-gradient(135deg, #f0fdfa 0%, #ffffff 60%)',
        border: '1px solid #99f6e4', borderRadius: '16px', padding: '24px', marginBottom: '28px',
      }}
      className="ai-insights-card"
    >
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#0f766e', fontWeight: 700, fontSize: '13px', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
            <Sparkles size={16} /> AI briefing
          </div>
          {insights.aiBriefing && (
            <button
              onClick={refresh}
              disabled={refreshing}
              title="Generate a new briefing"
              style={{ display: 'flex', alignItems: 'center', gap: '6px', border: 'none', background: 'transparent', color: '#64748b', fontSize: '12px', cursor: 'pointer' }}
            >
              <RefreshCw size={13} style={refreshing ? { animation: 'spin 1s linear infinite' } : undefined} />
              {refreshing ? 'Updating...' : 'Refresh'}
            </button>
          )}
        </div>
        <p style={{ margin: 0, fontSize: '15px', lineHeight: 1.65, color: '#0f172a' }}>
          {insights.aiBriefing ?? fallbackBriefing(insights)}
        </p>
        <div style={{ marginTop: '14px', display: 'flex', gap: '16px', flexWrap: 'wrap', fontSize: '13px' }}>
          <Link to="/finance/assistant" style={{ color: '#0f766e', fontWeight: 600, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}>
            Ask the AI analyst <ArrowRight size={14} />
          </Link>
          {!insights.aiBriefing && (
            <span style={{ color: '#64748b' }}>Set <code>GROQ_API_KEY</code> (free) for a written AI briefing.</span>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '14px 16px' }}>
          <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>Projected MRR in 3 months</div>
          {projectedMrrMinor !== null && growth !== null ? (
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px' }}>
              <span style={{ fontSize: '22px', fontWeight: 700, color: '#0f172a' }}>{rupees(projectedMrrMinor)}</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '3px', fontSize: '13px', fontWeight: 600, color: growth >= 0 ? '#15803d' : '#b91c1c' }}>
                {growth >= 0 ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                {growth >= 0 ? '+' : ''}{growth}%
              </span>
            </div>
          ) : (
            <div style={{ fontSize: '14px', color: '#64748b' }}>Needs at least two months of history</div>
          )}
        </div>

        <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <ShieldAlert size={13} /> Most at risk
            </span>
            <Link to="/finance/at-risk" style={{ fontSize: '12px', color: '#0f766e', textDecoration: 'none', fontWeight: 600 }}>View all</Link>
          </div>
          {insights.topRisks.length === 0 && <div style={{ fontSize: '13px', color: '#64748b' }}>No customers flagged.</div>}
          {insights.topRisks.map((r) => (
            <div key={`${r.customerId}-${r.planName}`} style={{ display: 'flex', justifyContent: 'space-between', gap: '8px', padding: '4px 0', fontSize: '13px' }}>
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                <strong style={{ color: '#0f172a' }}>{r.customerName ?? `Customer #${r.customerId}`}</strong>
                <span style={{ color: '#64748b' }}> · {r.reasons[0]}</span>
              </span>
              <span style={{ fontWeight: 700, color: r.level === 'HIGH' ? '#b91c1c' : r.level === 'MEDIUM' ? '#b45309' : '#15803d' }}>{r.score}</span>
            </div>
          ))}
        </div>
      </div>
      <style>{'@keyframes spin { to { transform: rotate(360deg); } } @media (max-width: 900px) { .ai-insights-card { grid-template-columns: 1fr !important; } }'}</style>
    </div>
  );
};
