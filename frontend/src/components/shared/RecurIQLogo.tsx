import React from 'react';
import { Link } from 'react-router-dom';

interface Props {
  portal?: string;   // e.g. "Finance" - shown under the wordmark
  to?: string;
}

/** RecurIQ mark: a recurring-cycle arrow wrapped around an insight spark. */
export const RecurIQMark: React.FC<{ size?: number }> = ({ size = 32 }) => (
  <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
    <defs>
      <linearGradient id="recuriq-mark-bg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#14b8a6" />
        <stop offset="100%" stopColor="#0f766e" />
      </linearGradient>
    </defs>
    <rect width="32" height="32" rx="9" fill="url(#recuriq-mark-bg)" />
    {/* Cycle arrow, open at the top right */}
    <path
      d="M22.5 11.2A8 8 0 1 0 24 16"
      fill="none"
      stroke="#ffffff"
      strokeWidth="2.6"
      strokeLinecap="round"
    />
    <path d="M24.9 7.6 L23.4 12.6 L18.6 11" fill="none" stroke="#ffffff" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    {/* Insight spark */}
    <path d="M16 12.2 L17.1 14.9 L19.8 16 L17.1 17.1 L16 19.8 L14.9 17.1 L12.2 16 L14.9 14.9 Z" fill="#fde68a" />
  </svg>
);

export const RecurIQLogo: React.FC<Props> = ({ portal, to = '/' }) => (
  <Link to={to} style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '10px' }} aria-label="RecurIQ home">
    <RecurIQMark />
    <span style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.05 }}>
      <span style={{ fontFamily: "'Outfit', sans-serif", fontWeight: 700, fontSize: '1.2rem', letterSpacing: '-0.3px', color: '#0f172a' }}>
        Recur<span style={{ color: '#0f766e' }}>IQ</span>
      </span>
      {portal && (
        <span style={{ fontFamily: "'Inter', sans-serif", fontSize: '10px', fontWeight: 600, letterSpacing: '0.14em', textTransform: 'uppercase', color: '#64748b', marginTop: '3px' }}>
          {portal}
        </span>
      )}
    </span>
  </Link>
);
