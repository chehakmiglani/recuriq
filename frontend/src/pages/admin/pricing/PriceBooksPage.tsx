import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { ConfirmDialog } from '../../../components/shared/ConfirmDialog';
import { getPriceBooks, createPriceBook, updatePriceBook, archivePriceBook, getAllPlans } from '../../../services/admin/adminService';
import type { PriceBookResponse, PlanResponse } from '../../../services/admin/adminTypes';
const formatPrice = (minor: number, cur: string) => {
  const amt = minor / 100;
  if (cur === 'INR') return `₹${amt.toLocaleString('en-IN')}`;
  if (cur === 'USD') return `$${amt.toFixed(2)}`;
  return `${cur} ${amt}`;
};
const emptyForm = { planId: '', region: '', currency: 'INR', priceMinor: '', effectiveFrom: new Date().toISOString().split('T')[0] };
export const PriceBooksPage: React.FC = () => {
  const [entries, setEntries] = useState<PriceBookResponse[]>([]);
  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
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
    onConfirm: () => { },
  });
  const closeConfirmDialog = () => {
    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
  };
  const load = () => {
    Promise.all([getPriceBooks(), getAllPlans()])
      .then(([pb, pl]) => { setEntries(pb); setPlans(pl); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);
  const openCreate = () => { setForm(emptyForm); setEditId(null); setModalOpen(true); };
  const openEdit = (row: PriceBookResponse) => {
    setForm({ planId: String(row.planId), region: row.region, currency: row.currency, priceMinor: String(row.priceMinor / 100), effectiveFrom: row.effectiveFrom || '' });
    setEditId(row.id); setModalOpen(true);
  };
  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, priceMinor: Math.round(Number(form.priceMinor) * 100), plan: { id: Number(form.planId) } };
    try {
      if (editId) await updatePriceBook(editId, payload);
      else await createPriceBook(payload);
      setModalOpen(false); load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };
  const handleDelete = (id: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Delete Price Book',
      message: 'Delete this price book entry?\n\nNote: If active subscriptions use this plan, deletion will be blocked.',
      isDanger: true,
      confirmLabel: 'Delete',
      onConfirm: async () => {
        closeConfirmDialog();
        try {
          await archivePriceBook(id);
          load();
        } catch (e: any) {
          alert(e.message || 'Delete failed');
        }
      }
    });
  };
  const columns = [
    { key: 'planName', header: 'Plan', render: (r: PriceBookResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.planName}</span> },
    { key: 'region', header: 'Region' },
    { key: 'currency', header: 'Currency' },
    { key: 'priceMinor', header: 'Price', render: (r: PriceBookResponse) => formatPrice(r.priceMinor, r.currency) },
    { key: 'effectiveFrom', header: 'Effective From' },
    {
      key: 'actions', header: 'Actions', render: (r: PriceBookResponse) => (
        <button className="btn-admin-sm btn-delete-sm" onClick={(e) => { e.stopPropagation(); handleDelete(r.id); }}>Delete</button>
      )
    },
  ];
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading price books...</div>;
  return (
    <>
      <PageHeader subtitle="Manage regional pricing and currency rules." actionLabel="Create Price Book" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={entries} emptyMessage="No price book entries yet." onRowClick={openEdit} />
      </div>
      <AdminModal isOpen={modalOpen} title={editId ? 'Edit Price Book' : 'Add Price Book Entry'} onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Plan" value={form.planId} onChange={(v) => setForm({ ...form, planId: v })} required options={plans.map(p => ({ value: String(p.id), label: p.name }))} />
        <div className="form-row">
          <FormField label="Region" value={form.region} onChange={(v) => setForm({ ...form, region: v })} required placeholder="IN, US, GB" />
          <FormField label="Currency" value={form.currency} onChange={(v) => setForm({ ...form, currency: v })} required options={[{ value: 'INR', label: 'INR' }, { value: 'USD', label: 'USD' }, { value: 'GBP', label: 'GBP' }]} />
        </div>
        <div className="form-row">
          <FormField label="Price" value={form.priceMinor} onChange={(v) => setForm({ ...form, priceMinor: v })} type="number" required placeholder="199" />
          <FormField label="Effective From" value={form.effectiveFrom} onChange={(v) => setForm({ ...form, effectiveFrom: v })} type="date" />
        </div>
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
