import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { ConfirmDialog } from '../../../components/shared/ConfirmDialog';
import { getAllPlans, createPlan, deletePlan } from '../../../services/admin/adminService';
import type { PlanResponse } from '../../../services/admin/adminTypes';
const formatPrice = (minor: number, cur: string) => {
  const amt = minor / 100;
  if (cur === 'INR') return `₹${amt.toLocaleString('en-IN')}`;
  if (cur === 'USD') return `$${amt.toFixed(2)}`;
  return `${cur} ${amt}`;
};
const emptyForm = { name: '', billingPeriod: 'MONTHLY', defaultPriceMinor: '', defaultCurrency: 'INR', trialDays: '7', taxMode: 'EXCLUSIVE', effectiveFrom: new Date().toISOString().split('T')[0] };
export const PlansPage: React.FC = () => {
  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    title: string;
    message: string | React.ReactNode;
    confirmLabel?: string;
    cancelLabel?: string;
    isDanger?: boolean;
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {}
  });
  const load = () => { getAllPlans().then(setPlans).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);
  const openCreate = () => { setForm(emptyForm); setModalOpen(true); };
  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, defaultPriceMinor: Math.round(Number(form.defaultPriceMinor) * 100), trialDays: Number(form.trialDays), setupFeeMinor: 0, product: { id: 1 } };
    try {
      await createPlan(payload);
      setModalOpen(false); load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };

  const closeConfirmDialog = () => {
    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
  };

  const handleDeleteConfirm = async (id: number) => {
    try {
      await deletePlan(id);
      load();
    } catch (e: any) {
      alert(e.message);
    } finally {
      closeConfirmDialog();
    }
  };

  const handleDelete = (id: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Confirm Plan Deletion',
      message: 'Are you sure you want to delete this plan? This will also delete associated pricebook entries.',
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      isDanger: true,
      onConfirm: () => handleDeleteConfirm(id)
    });
  };
  const columns = [
    { key: 'name', header: 'Plan Name', render: (r: PlanResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.name}</span> },
    { key: 'billingPeriod', header: 'Billing' },
    { key: 'defaultPriceMinor', header: 'Price', render: (r: PlanResponse) => formatPrice(r.defaultPriceMinor, r.defaultCurrency) },
    { key: 'trialDays', header: 'Trial', render: (r: PlanResponse) => `${r.trialDays} days` },
    { key: 'status', header: 'Status', render: (r: PlanResponse) => <StatusBadge status={r.status} /> },
    {
      key: 'actions', header: 'Actions', render: (r: PlanResponse) => (
        <div className="table-actions">
          <button className="btn-admin-sm btn-toggle-active" onClick={(e) => { e.stopPropagation(); handleDelete(r.id); }}>
            Delete
          </button>
        </div>
      )
    },
  ];
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading plans...</div>;
  return (
    <>
      <PageHeader subtitle="Create and manage subscription plans." actionLabel="Create Plan" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={plans} emptyMessage="No plans created yet." />
      </div>
      <AdminModal isOpen={modalOpen} title="Create Plan" onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Plan Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required placeholder="e.g. Basic Monthly" />
        <div className="form-row">
          <FormField label="Billing Period" value={form.billingPeriod} onChange={(v) => setForm({ ...form, billingPeriod: v })} options={[{ value: 'MONTHLY', label: 'Monthly' }, { value: 'YEARLY', label: 'Yearly' }]} />
          <FormField label="Currency" value={form.defaultCurrency} onChange={(v) => setForm({ ...form, defaultCurrency: v })} options={[{ value: 'INR', label: 'INR (₹)' }, { value: 'USD', label: 'USD ($)' }, { value: 'GBP', label: 'GBP (£)' }]} />
        </div>
        <div className="form-row">
          <FormField label="Price" value={form.defaultPriceMinor} onChange={(v) => setForm({ ...form, defaultPriceMinor: v })} type="number" placeholder="199" required />
          <FormField label="Trial Days" value={form.trialDays} onChange={(v) => setForm({ ...form, trialDays: v })} type="number" />
        </div>
        <div className="form-row">
          <FormField label="Tax Mode" value={form.taxMode} onChange={(v) => setForm({ ...form, taxMode: v })} options={[{ value: 'EXCLUSIVE', label: 'Exclusive' }, { value: 'INCLUSIVE', label: 'Inclusive' }]} />
          <FormField label="Effective From" value={form.effectiveFrom} onChange={(v) => setForm({ ...form, effectiveFrom: v })} type="date" />
        </div>
      </AdminModal>
      <ConfirmDialog
        isOpen={confirmDialog.isOpen}
        title={confirmDialog.title}
        message={confirmDialog.message}
        onConfirm={confirmDialog.onConfirm}
        onCancel={closeConfirmDialog}
        confirmLabel={confirmDialog.confirmLabel}
        cancelLabel={confirmDialog.cancelLabel}
        isDanger={confirmDialog.isDanger}
      />
    </>
  );
};
