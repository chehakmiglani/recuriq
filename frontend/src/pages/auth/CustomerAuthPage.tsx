import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../../components/auth/shared/AuthLayout';
import { CustomerLoginForm } from '../../components/auth/customer/CustomerLoginForm';
import { CustomerSignupForm } from '../../components/auth/customer/CustomerSignupForm';

export const CustomerAuthPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const isRegister = location.pathname === '/register';

  return (
    <AuthLayout 
      title={isRegister ? "Create Account" : "Sign In"}
      subtitle={isRegister 
        ? "Begin your journey into curated cinema." 
        : "Welcome back. Continue your exploration."
      }
    >
      {isRegister ? (
        <CustomerSignupForm onSwitchToLogin={() => navigate('/login')} />
      ) : (
        <>
          <CustomerLoginForm />
          <div className="auth-footer">
            <span className="auth-footer-text">Don't have an account? </span>
            <button 
              type="button" 
              className="auth-footer-link"
              onClick={() => navigate('/register')}
            >
              Sign Up
            </button>
          </div>
        </>
      )}
    </AuthLayout>
  );
};
