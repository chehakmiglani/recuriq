import React from 'react';
import { AuthLayout } from '../../components/auth/shared/AuthLayout';
import { ManagementLoginForm } from '../../components/auth/management/ManagementLoginForm';

export const ManagementAuthPage: React.FC = () => {
  return (
    <AuthLayout
      title="Management Portal"
      subtitle="Internal access for authorized personnel."
    >
      <ManagementLoginForm />
    </AuthLayout>
  );
};
