import React, { useState } from 'react';
import { FlushedInput, FlushedPasswordField, LoginButton } from '../shared/FormFields';
import { authService } from '../../../services/auth/authService';
import { useNavigate } from 'react-router-dom';

export const CustomerSignupForm: React.FC<{ onSwitchToLogin: () => void }> = ({ onSwitchToLogin }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!/^[a-zA-Z\s]+$/.test(fullName)) {
      setFieldErrors({ fullName: 'Name must contain only letters and spaces' });
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await authService.customerRegister({ fullName, email, password });
      navigate('/login');
    } catch (err: any) {
      if (err.validationErrors) {
        setFieldErrors(err.validationErrors);
      }
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="auth-error">{error}</div>}
      
      <FlushedInput 
        label="Full Name"
        value={fullName}
        onChange={(e) => { setFullName(e.target.value); setFieldErrors({...fieldErrors, fullName: ''}) }}
        disabled={loading}
        error={fieldErrors['fullName']}
        placeholder="Enter your full name"
        required
      />

      <FlushedInput 
        label="Email Address"
        type="email"
        value={email}
        onChange={(e) => { setEmail(e.target.value); setFieldErrors({...fieldErrors, email: ''}) }}
        disabled={loading}
        error={fieldErrors['email']}
        placeholder="you@example.com"
        required
      />

      <FlushedPasswordField 
        label="Password"
        value={password}
        onChange={(e) => { setPassword(e.target.value); setFieldErrors({...fieldErrors, password: ''}) }}
        disabled={loading}
        error={fieldErrors['password']}
        placeholder="Create a password"
        required
      />

      <FlushedPasswordField 
        label="Confirm Password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        disabled={loading}
        placeholder="Confirm your password"
        required
      />
      
      <div className="auth-terms">
        By creating an account, you confirm that you are 18 or over, you agree to our Terms of Use and Privacy Policy.
      </div>
      
      <LoginButton loading={loading}>
        {loading ? 'Creating Account...' : 'Create Account'}
      </LoginButton>
      
      <div className="auth-footer">
        <span className="auth-footer-text">Already have an account? </span>
        <button type="button" className="auth-footer-link" onClick={onSwitchToLogin}>Sign In</button>
      </div>
    </form>
  );
};
