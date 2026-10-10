import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { ConfirmDialog } from '../../../components/shared/ConfirmDialog';
import { useAuthContext } from '../../../context/AuthContext';
import { getStaff, createStaff, deleteStaff } from '../../../services/admin/adminService';
import type { StaffResponse } from '../../../services/admin/adminTypes';
import { errorMessage } from '../../../utils/apiError';

const roleStyles: Record<string, { border: string; color: string }> = {
  ADMIN: { border: '#0f766e', color: '#0f766e' },
  FINANCE: { border: '#e5e7eb', color: '#6b7280' },
  SUPPORT: { border: '#e5e7eb', color: '#6b7280' }
};
const emptyForm = { fullName: '', email: '', passwordHash: '', role: 'SUPPORT' };

export const StaffAccountsPage: React.FC = () => {
  const { user: currentUser } = useAuthContext();
  const [staff, setStaff] = useState<StaffResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  // Confirm Dialog State
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    isDanger?: boolean;
    confirmLabel?: string;
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {},
  });

  const closeConfirmDialog = () => {
    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
  };

  const load = () => { getStaff().then(setStaff).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);

  const openCreate = () => { setForm(emptyForm); setModalOpen(true); };

  const handleSave = async () => {
    if (!/^[a-zA-Z\s]+$/.test(form.fullName)) {
      alert("Name must contain only letters and spaces");
      return;
    }
    setSaving(true);
    try {
      await createStaff(form);
      setModalOpen(false); load();
    } catch (e) { alert(errorMessage(e)); }
    finally { setSaving(false); }
  };

  const handleDelete = (id: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Remove Staff Member',
      message: 'Are you sure you want to remove this staff member? This action cannot be undone.',
      isDanger: true,
      confirmLabel: 'Remove Staff',
      onConfirm: async () => {
        closeConfirmDialog();
        await deleteStaff(id);
        load();
      }
    });
  };

  const columns = [
    { key: 'fullName', header: 'Name', render: (r: StaffResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.fullName}</span> },
    { key: 'email', header: 'Email' },
    { key: 'role', header: 'Role', render: (r: StaffResponse) => {
      const style = roleStyles[r.role] || roleStyles.SUPPORT;
      return (
        <span style={{ border: `1px solid ${style.border}`, color: style.color, padding: '4px 10px', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase' as const, letterSpacing: '0.1em' }}>
          {r.role}
        </span>
      );
    }},
    { key: 'status', header: 'Status', render: (r: StaffResponse) => <StatusBadge status={r.status} /> },
    { key: 'actions', header: 'Actions', render: (r: StaffResponse) => (
      <button 
        className={`btn-admin-sm btn-delete-sm ${currentUser?.email === r.email ? 'disabled' : ''}`} 
        onClick={(e) => { 
          if (currentUser?.email === r.email) return;
          e.stopPropagation(); 
          handleDelete(r.id); 
        }}
        disabled={currentUser?.email === r.email}
        title={currentUser?.email === r.email ? "You cannot remove your own account" : ""}
      >
        Remove
      </button>
    )},
  ];

  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading staff...</div>;

  return (
    <>
      <PageHeader subtitle="Manage Admin, Finance, and Support staff." actionLabel="Add Staff" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={staff} emptyMessage="No staff accounts." />
      </div>

      <AdminModal isOpen={modalOpen} title="Add Staff Member" onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Full Name" value={form.fullName} onChange={(v) => setForm({ ...form, fullName: v })} required placeholder="John Doe" />
        <FormField label="Email" value={form.email} onChange={(v) => setForm({ ...form, email: v })} type="email" required placeholder="john@streamflix.com" />
        <FormField label="Password" value={form.passwordHash} onChange={(v) => setForm({ ...form, passwordHash: v })} type="password" required placeholder="Minimum 8 characters" />
        <FormField label="Role" value={form.role} onChange={(v) => setForm({ ...form, role: v })} required options={[{ value: 'ADMIN', label: 'Admin' }, { value: 'FINANCE', label: 'Finance' }, { value: 'SUPPORT', label: 'Support' }]} />
      </AdminModal>

      {/* Shared Confirm Dialog */}
      <ConfirmDialog
        isOpen={confirmDialog.isOpen}
        title={confirmDialog.title}
        message={confirmDialog.message}
        onConfirm={confirmDialog.onConfirm}
        onCancel={closeConfirmDialog}
        isDanger={confirmDialog.isDanger}
        confirmLabel={confirmDialog.confirmLabel}
      />
    </>
  );
};
