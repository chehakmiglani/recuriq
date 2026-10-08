import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { ConfirmDialog } from '../../../components/shared/ConfirmDialog';
import { getTaxRates, createTaxRate, updateTaxRate, deleteTaxRate } from '../../../services/admin/adminService';
import type { TaxRate } from '../../../services/admin/adminTypes';

const emptyForm = { name: '', region: '', ratePercent: '', inclusive: 'false', effectiveFrom: new Date().toISOString().split('T')[0] };

export const TaxRatesPage: React.FC = () => {
  const [rates, setRates] = useState<TaxRate[]>([]);
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
    onConfirm: () => {},
  });

  const closeConfirmDialog = () => {
    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
  };

  const load = () => { getTaxRates().then(setRates).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);

  const openCreate = () => { setForm(emptyForm); setEditId(null); setModalOpen(true); };
  const openEdit = (row: TaxRate) => {
    setForm({ name: row.name, region: row.region, ratePercent: String(row.ratePercent), inclusive: String(row.inclusive), effectiveFrom: row.effectiveFrom || '' });
    setEditId(row.id); setModalOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, ratePercent: Number(form.ratePercent), inclusive: form.inclusive === 'true' };
    try {
      if (editId) await updateTaxRate(editId, payload);
      else await createTaxRate(payload);
      setModalOpen(false); load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };

  const handleDelete = (id: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Delete Tax Rate',
      message: 'Are you sure you want to delete this tax rate? This action cannot be undone.',
      isDanger: true,
      confirmLabel: 'Delete',
      onConfirm: async () => {
        closeConfirmDialog();
        await deleteTaxRate(id);
        load();
      }
    });
  };

  const columns = [
    { key: 'name', header: 'Tax Name', render: (r: TaxRate) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.name}</span> },
    { key: 'region', header: 'Region' },
    { key: 'ratePercent', header: 'Rate', render: (r: TaxRate) => `${r.ratePercent}%` },
    { key: 'inclusive', header: 'Inclusive', render: (r: TaxRate) => r.inclusive ? 'Yes' : 'No' },
    { key: 'effectiveFrom', header: 'Effective From' },
    { key: 'actions', header: 'Actions', render: (r: TaxRate) => (
      <button className="btn-admin-sm btn-delete-sm" onClick={(e) => { e.stopPropagation(); handleDelete(r.id); }}>Delete</button>
    )},
  ];

  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading tax rates...</div>;

  return (
    <>
      <PageHeader subtitle="Manage regional tax rules and percentages." actionLabel="Add Tax Rate" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={rates} emptyMessage="No tax rates configured." onRowClick={openEdit} />
      </div>

      <AdminModal isOpen={modalOpen} title={editId ? 'Edit Tax Rate' : 'Add Tax Rate'} onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Tax Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required placeholder="e.g. GST (India)" />
        <div className="form-row">
          <FormField label="Region" value={form.region} onChange={(v) => setForm({ ...form, region: v })} required placeholder="IN, US, GB" />
          <FormField label="Rate (%)" value={form.ratePercent} onChange={(v) => setForm({ ...form, ratePercent: v })} type="number" required placeholder="18" />
        </div>
        <div className="form-row">
          <FormField label="Inclusive" value={form.inclusive} onChange={(v) => setForm({ ...form, inclusive: v })} options={[{ value: 'false', label: 'No (Exclusive)' }, { value: 'true', label: 'Yes (Inclusive)' }]} />
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
