import React, { useState } from 'react';
import { FlushedInput, FlushedPasswordField, LoginButton } from '../shared/FormFields';
import { useAuthContext } from '../../../context/AuthContext';
import { authService } from '../../../services/auth/authService';
import { useNavigate } from 'react-router-dom';

export const ManagementLoginForm: React.FC = () => {
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
      const user = await authService.managerLogin({ email, password });
      await login(user);
      if (user.role === 'FINANCE') {
        navigate('/finance');
      } else if (user.role === 'ADMIN') {
        navigate('/admin');
      } else if (user.role === 'SUPPORT') {
        navigate('/support');
      }
    } catch (err: any) {
      if (err.validationErrors) {
        setFieldErrors(err.validationErrors);
      }
      setError(err.message || 'Login failed');
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
        label="Corporate Email"
        type="email"
        value={email}
        onChange={(e) => { setEmail(e.target.value); setFieldErrors({ ...fieldErrors, email: '' }) }}
        disabled={loading}
        error={fieldErrors['email']}
        placeholder="Enter corporate email"
        required
      />

      <FlushedPasswordField
        label="Password"
        value={password}
        onChange={(e) => { setPassword(e.target.value); setFieldErrors({ ...fieldErrors, password: '' }) }}
        disabled={loading}
        error={fieldErrors['password']}
        placeholder="Enter password"
        required
      />

      <LoginButton loading={loading}>
        {loading ? 'Authenticating...' : 'Access Portal'}
      </LoginButton>
    </form>
  );
};
