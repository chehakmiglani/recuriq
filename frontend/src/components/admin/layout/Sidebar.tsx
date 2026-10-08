import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  Layers,
  Puzzle,
  Gauge,
  Tag,
  Percent,
  Ticket,
  Users,
  ShieldCheck,
  Repeat,
} from 'lucide-react';

const navItems = [
  { section: 'Main', label: 'Dashboard', path: '/admin', icon: <LayoutDashboard size={18} /> },
  { section: 'Catalog', label: 'Product', path: '/admin/product', icon: <Package size={18} /> },
  { section: 'Catalog', label: 'Plans', path: '/admin/plans', icon: <Layers size={18} /> },
  { section: 'Catalog', label: 'Add-ons', path: '/admin/addons', icon: <Puzzle size={18} /> },
  { section: 'Catalog', label: 'Metered Components', path: '/admin/metered', icon: <Gauge size={18} /> },
  { section: 'Pricing', label: 'Price Books', path: '/admin/pricebooks', icon: <Tag size={18} /> },
  { section: 'Pricing', label: 'Tax Rates', path: '/admin/taxrates', icon: <Percent size={18} /> },
  { section: 'Pricing', label: 'Coupons', path: '/admin/coupons', icon: <Ticket size={18} /> },
  { section: 'Users', label: 'Customers', path: '/admin/customers', icon: <Users size={18} /> },
  { section: 'Users', label: 'Staff Accounts', path: '/admin/staff', icon: <ShieldCheck size={18} /> },
  { section: 'Subscriptions', label: 'All Subscriptions', path: '/admin/subscriptions', icon: <Repeat size={18} /> },
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
    if (path === '/admin') return location.pathname === '/admin';
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
                end={item.path === '/admin'}
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
