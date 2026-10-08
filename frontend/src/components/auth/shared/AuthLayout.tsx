import React from 'react';
import { Link } from 'react-router-dom';
import '../../../styles/auth.css';

interface AuthLayoutProps {
  title: React.ReactNode;
  subtitle: React.ReactNode;
  children: React.ReactNode;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ 
  title, 
  subtitle, 
  children
}) => {
  return (
    <div className="auth-page">
      {/* Background Blobs matching Landing Page */}
      <div className="auth-blob-container">
        <div className="auth-blob auth-blob-1"></div>
        <div className="auth-blob auth-blob-2"></div>
      </div>

      {/* Header with Logo */}
      <header className="auth-header">
        <Link to="/" className="auth-logo">
          STREAM<span>FLIX</span>
        </Link>
      </header>

      <div className="auth-wrapper">
        <div className="auth-form-container">
          {/* Header Section */}
          <div className="auth-header-section">
            <h1 className="auth-title">{title}</h1>
            <p className="auth-subtitle">{subtitle}</p>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
};
