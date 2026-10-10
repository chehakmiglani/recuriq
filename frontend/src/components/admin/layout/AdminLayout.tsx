import React, { useState, useRef, useEffect, useMemo } from 'react';
import { NavLink, useLocation, useNavigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../../context/AuthContext';
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
  Search,
  LogOut,
} from 'lucide-react';
import { RecurIQLogo } from '../../shared/RecurIQLogo';
import '../../../styles/admin.css';
import '../../../styles/admin-modal.css';

interface NavItem {
  section: string;
  label: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
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

const pageTitles: Record<string, string> = {
  '/admin': 'Dashboard',
  '/admin/product': 'Product Management',
  '/admin/plans': 'Plans',
  '/admin/addons': 'Add-ons',
  '/admin/metered': 'Metered Components',
  '/admin/pricebooks': 'Price Books',
  '/admin/taxrates': 'Tax Rates',
  '/admin/coupons': 'Coupons',
  '/admin/customers': 'Customers',
  '/admin/staff': 'Staff Accounts',
  '/admin/subscriptions': 'Subscriptions',
};

// Group items by section
const sections = navItems.reduce<Record<string, typeof navItems>>((acc, item) => {
  if (!acc[item.section]) acc[item.section] = [];
  acc[item.section].push(item);
  return acc;
}, {});

export const AdminLayout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthContext();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLFormElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setSearchOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Nav items matching the search box
  const searchResults = useMemo(() => {
    if (searchQuery.trim().length === 0) return [];
    return navItems.filter(item =>
      item.label.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [searchQuery]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
    setDropdownOpen(false);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchResults.length > 0) {
      navigate(searchResults[0].path);
      setSearchQuery('');
      setSearchOpen(false);
    }
  };

  const handleSelectResult = (path: string) => {
    navigate(path);
    setSearchQuery('');
    setSearchOpen(false);
  };

  const getTitle = () => {
    if (pageTitles[location.pathname]) return pageTitles[location.pathname];
    const basePath = Object.keys(pageTitles).find(path =>
      path !== '/admin' && location.pathname.startsWith(path)
    );
    return basePath ? pageTitles[basePath] : 'Admin';
  };

  const isActive = (path: string) => {
    if (path === '/admin') return location.pathname === '/admin';
    return location.pathname.startsWith(path);
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(w => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div className="admin-shell">
      {/* Google Fonts */}
      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Outfit:wght@700&display=swap" rel="stylesheet" />

      {/* Sidebar - Modern White theme integrated */}
      <aside className="admin-sidebar" style={{ boxShadow: 'none' }}>
        <div
          style={{
            padding: '0 28px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            borderBottom: '1px solid #e5e7eb',
            flexShrink: 0,
            height: '72px',
            boxSizing: 'border-box',
          }}
        >
          <RecurIQLogo portal="Admin" to="/admin" />
        </div>

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

      {/* Main Content Area */}
      <div className="admin-content-area">
        {/* Top Header */}
        <header
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 40px',
            height: '72px',
            backgroundColor: '#ffffff',
            borderBottom: '1px solid #e5e7eb',
            position: 'sticky',
            top: 0,
            zIndex: 50,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 700, color: '#1f2937', margin: 0, fontFamily: "'Outfit', sans-serif" }}>
              {getTitle()}
            </h2>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <form
              onSubmit={handleSearch}
              ref={searchRef}
              style={{ width: '300px', position: 'relative' }}
            >
              <Search
                size={16}
                style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}
              />
              <input
                type="text"
                placeholder="Quick Search Admin..."
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setSearchOpen(true); }}
                style={{
                  width: '100%',
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '99px',
                  padding: '10px 16px 10px 42px',
                  fontSize: '13px',
                  outline: 'none',
                  transition: 'all 0.2s'
                }}
              />
              {/* Search Results Dropdown */}
              {searchOpen && searchResults.length > 0 && (
                <div style={{
                  position: 'absolute',
                  top: 'calc(100% + 8px)',
                  left: 0,
                  right: 0,
                  background: 'white',
                  borderRadius: '12px',
                  boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
                  border: '1px solid #e5e7eb',
                  padding: '8px',
                  zIndex: 100
                }}>
                  {searchResults.map((result) => (
                    <div
                      key={result.path}
                      onClick={() => handleSelectResult(result.path)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        padding: '10px 12px',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontSize: '13px',
                        color: '#475569',
                        transition: 'background 0.2s'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.background = '#f1f5f9'}
                      onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                    >
                      <span style={{ color: '#94a3b8' }}>{result.icon}</span>
                      <span style={{ fontWeight: 500 }}>{result.label}</span>
                    </div>
                  ))}
                </div>
              )}
            </form>

            <div style={{ position: 'relative' }} ref={dropdownRef}>
              <button
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '8px',
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                }}
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                <div style={{ textAlign: 'right' }}>
                  <p style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#1E293B' }}>{user?.fullName || 'Admin User'}</p>
                  <p style={{ margin: 0, fontSize: '11px', color: '#64748B', fontWeight: 500 }}>{user?.role || 'ADMIN'}</p>
                </div>
                <div
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: '12px',
                    backgroundColor: '#0f766e',
                    color: 'white',
                    fontSize: '14px',
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxShadow: '0 4px 12px rgba(15, 118, 110, 0.2)'
                  }}
                >
                  {user ? getInitials(user.fullName || user.email) : 'AD'}
                </div>
              </button>

              {dropdownOpen && (
                <div
                  style={{
                    position: 'absolute',
                    top: 'calc(100% + 12px)',
                    right: 0,
                    width: '240px',
                    background: '#ffffff',
                    border: '1px solid #f1f5f9',
                    borderRadius: '16px',
                    zIndex: 100,
                    overflow: 'hidden',
                    boxShadow: '0 10px 40px rgba(0,0,0,0.1)',
                  }}
                >
                  <div style={{ padding: '16px', background: '#f8fafc', borderBottom: '1px solid #f1f5f9' }}>
                    <p style={{ fontSize: '14px', fontWeight: 700, color: '#1E293B', margin: 0 }}>{user?.fullName || 'Admin'}</p>
                    <p style={{ fontSize: '12px', color: '#64748B', margin: 0 }}>{user?.email}</p>
                  </div>
                  <div style={{ padding: '8px' }}>
                    <button
                      onClick={handleLogout}
                      style={{
                        width: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        padding: '8px 12px',
                        background: 'transparent',
                        border: 'none',
                        borderRadius: '12px',
                        textAlign: 'left',
                        fontSize: '14px',
                        color: '#EF4444',
                        fontWeight: 600,
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#FEF2F2')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                    >
                      <LogOut size={18} />
                      Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
