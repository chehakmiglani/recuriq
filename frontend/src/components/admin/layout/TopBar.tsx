import React, { useState, useRef, useEffect } from 'react';
import { Search, LogOut, ChevronDown } from 'lucide-react';
import { useAuthContext } from '../../../context/AuthContext';

interface TopBarProps {
  title: string;
}

export const TopBar: React.FC<TopBarProps> = ({ title: _title }) => {
  const { user, logout } = useAuthContext();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(w => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const handleLogout = async () => {
    try {
      await fetch('http://localhost:8765/api/logout', {
        method: 'POST',
        credentials: 'include',
      });
    } catch {
      // Fail silently
    }
    logout();
    setDropdownOpen(false);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement search functionality
  };

  return (
    <header className="admin-topbar">
      <form className="topbar-search-main" onSubmit={handleSearch}>
        <Search size={18} className="topbar-search-icon" />
        <input
          type="text"
          placeholder="Search products, plans, or subscribers..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="topbar-search-input"
        />
      </form>

      <div className="topbar-actions">

        <div className="topbar-profile" ref={dropdownRef}>
          <button
            className="topbar-profile-btn"
            onClick={() => setDropdownOpen(!dropdownOpen)}
          >
            <div className="sidebar-user-avatar" style={{ width: 34, height: 34, fontSize: '0.75rem' }}>
              {user ? getInitials(user.fullName || user.email) : 'AD'}
            </div>
            <ChevronDown size={16} style={{ color: '#6b7280' }} />
          </button>

          {dropdownOpen && (
            <div className="topbar-dropdown">
              <div className="topbar-dropdown-header">
                <div className="topbar-dropdown-name">{user?.fullName || 'Admin'}</div>
                <div className="topbar-dropdown-role">{user?.role || 'ADMIN'}</div>
              </div>
              <div className="topbar-dropdown-divider"></div>
              <button className="topbar-dropdown-item" onClick={handleLogout}>
                <LogOut size={16} />
                Sign Out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
