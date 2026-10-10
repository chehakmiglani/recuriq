import React, { useState } from 'react';
import { FlushedInput, FlushedPasswordField, LoginButton } from '../shared/FormFields';
import { useAuthContext } from '../../../context/AuthContext';
import { authService } from '../../../services/auth/authService';
import { useNavigate } from 'react-router-dom';
import { asApiError, errorMessage } from '../../../utils/apiError';

export const CustomerLoginForm: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const { login } = useAuthContext();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await authService.customerLogin({ email, password });
      await login(user);
      navigate('/');
    } catch (err) {
      const fieldErrors = asApiError(err).validationErrors;
      if (fieldErrors) {
        setFieldErrors(fieldErrors);
      }
      setError(errorMessage(err) || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && (
        <div className="auth-error">
          {error}
        </div>
      )}
      
      <FlushedInput 
        label="Email Address"
        type="email"
        value={email}
        onChange={(e) => { setEmail(e.target.value); setFieldErrors({...fieldErrors, email: ''}) }}
        disabled={loading}
        error={fieldErrors['email']}
        placeholder="Enter your email"
        required
      />
      
      <FlushedPasswordField 
        label="Password"
        value={password}
        onChange={(e) => { setPassword(e.target.value); setFieldErrors({...fieldErrors, password: ''}) }}
        disabled={loading}
        error={fieldErrors['password']}
        placeholder="Enter your password"
        required
      />
      <LoginButton loading={loading}>
        {loading ? 'Signing In...' : 'Sign In'}
      </LoginButton>

    </form>
  );
};
