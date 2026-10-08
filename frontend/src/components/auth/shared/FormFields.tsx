import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export const FlushedInput: React.FC<React.InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }> = ({ label, error, ...props }) => {
  return (
    <div className="auth-form-group">
      <label className={error ? 'error' : ''}>{label}</label>
      <input 
        className={`auth-input ${error ? 'error' : ''}`} 
        {...props} 
      />
      {error && <div className="auth-field-error">{error}</div>}
    </div>
  );
};

export const FlushedPasswordField: React.FC<React.InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; helperText?: string }> = ({ label, error, helperText, ...props }) => {
  const [show, setShow] = useState(false);
  
  return (
    <div className="auth-form-group">
      <label className={error ? 'error' : ''}>{label}</label>
      <div className="auth-password-wrapper">
        <input 
          type={show ? 'text' : 'password'} 
          className={`auth-input ${error ? 'error' : ''}`} 
          {...props} 
        />
        <button 
          type="button" 
          onClick={() => setShow(!show)}
          className="auth-password-toggle"
          tabIndex={-1}
        >
          {show ? <EyeOff size={20} /> : <Eye size={20} />}
        </button>
      </div>
      {error ? (
        <div className="auth-field-error">{error}</div>
      ) : helperText && (
        <div className="auth-helper-text">{helperText}</div>
      )}
    </div>
  );
};

interface LoginButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
}

export const LoginButton: React.FC<LoginButtonProps> = ({ children, loading, ...props }) => {
  return (
    <button className="auth-submit-btn" type="submit" disabled={loading} {...props}>
      {loading && <div className="auth-spinner" />}
      {children}
    </button>
  );
};
