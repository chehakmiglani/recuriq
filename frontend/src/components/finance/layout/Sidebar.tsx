import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  DollarSign,
  TrendingUp,
  Activity,
  TrendingDown,
  UserCheck,
  FileText,
  CreditCard,
  RefreshCcw,
  Camera,
} from 'lucide-react';

const navItems = [
  { section: 'Main', label: 'Overview', path: '/finance', icon: <DollarSign size={18} /> },
  { section: 'Analytics', label: 'MRR', path: '/finance/mrr', icon: <TrendingUp size={18} /> },
  { section: 'Analytics', label: 'ARR', path: '/finance/arr', icon: <Activity size={18} /> },
  { section: 'Analytics', label: 'Churn', path: '/finance/churn', icon: <TrendingDown size={18} /> },
  { section: 'Analytics', label: 'ARPU & LTV', path: '/finance/arpu', icon: <UserCheck size={18} /> },
  { section: 'Billing', label: 'Invoices', path: '/finance/invoices', icon: <FileText size={18} /> },
  { section: 'Billing', label: 'Payments', path: '/finance/payments', icon: <CreditCard size={18} /> },
  { section: 'Billing', label: 'Credits & Refunds', path: '/finance/credits', icon: <RefreshCcw size={18} /> },
  { section: 'Reports', label: 'Snapshots', path: '/finance/snapshots', icon: <Camera size={18} /> },
];

// Group items by section
const sections = navItems.reduce<Record<string, typeof navItems>>((acc, item) => {
  if (!acc[item.section]) acc[item.section] = [];
  acc[item.section].push(item);
  return acc;
}, {});

export const Sidebar: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/finance') return location.pathname === '/finance';
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="admin-sidebar">
      {/* Logo - Editorial */}
      <div className="sidebar-logo">
        <div className="sidebar-logo-text">
          STREAMFLIX
        </div>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        {Object.entries(sections).map(([sectionTitle, items]) => (
          <div key={sectionTitle} className="sidebar-section">
            <div className="sidebar-section-label">{sectionTitle}</div>
            {items.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/finance'}
                className={`sidebar-link ${isActive(item.path) ? 'active' : ''}`}
              >
                <span className="sidebar-link-icon">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
    </aside>
  );
};
