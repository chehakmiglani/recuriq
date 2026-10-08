import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { getAddOns, createAddOn, updateAddOn, toggleAddOnStatus } from '../../../services/admin/adminService';
import type { AddOnResponse } from '../../../services/admin/adminTypes';
const emptyForm = { name: '', priceMinor: '', currency: 'INR', billingPeriod: 'MONTHLY', taxMode: 'EXCLUSIVE' };
export const AddOnsPage: React.FC = () => {
  const [addOns, setAddOns] = useState<AddOnResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [confirmToggle, setConfirmToggle] = useState<AddOnResponse | null>(null);
  const load = () => { getAddOns().then(setAddOns).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);
  const openCreate = () => { setForm(emptyForm); setEditId(null); setModalOpen(true); };
  const openEdit = (row: AddOnResponse) => {
    setForm({ name: row.name, priceMinor: String(row.priceMinor / 100), currency: row.currency, billingPeriod: row.billingPeriod, taxMode: row.taxMode });
    setEditId(row.id); setModalOpen(true);
  };
  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, priceMinor: Math.round(Number(form.priceMinor) * 100), product: { id: 1 } };
    try {
      if (editId) await updateAddOn(editId, payload);
      else await createAddOn(payload);
      setModalOpen(false); load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };
  const handleToggle = async (id: number) => {
    setSaving(true);
    await toggleAddOnStatus(id);
    setSaving(false);
    setConfirmToggle(null);
    load();
  };
  const columns = [
    { key: 'name', header: 'Add-on Name', render: (r: AddOnResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.name}</span> },
    { key: 'priceMinor', header: 'Price', render: (r: AddOnResponse) => `₹${r.priceMinor / 100}/${r.billingPeriod === 'MONTHLY' ? 'mo' : 'yr'}` },
    { key: 'billingPeriod', header: 'Billing' },
    { key: 'status', header: 'Status', render: (r: AddOnResponse) => <StatusBadge status={r.status} /> },
    {
      key: 'actions', header: 'Actions', render: (r: AddOnResponse) => (
        <button className={`btn-admin-sm ${r.status === 'ACTIVE' ? 'btn-toggle-active' : 'btn-toggle-inactive'}`} onClick={(e) => { e.stopPropagation(); setConfirmToggle(r); }}>
          {r.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        </button>
      )
    },
  ];
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading add-ons...</div>;
  return (
    <>
      <PageHeader subtitle="Optional features customers can add to their subscription." actionLabel="Create Add-on" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={addOns} emptyMessage="No add-ons created yet." onRowClick={openEdit} />
      </div>
      <AdminModal isOpen={modalOpen} title={editId ? 'Edit Add-on' : 'Create Add-on'} onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Add-on Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required placeholder="e.g. Ad-Free Experience" />
        <div className="form-row">
          <FormField label="Price" value={form.priceMinor} onChange={(v) => setForm({ ...form, priceMinor: v })} type="number" required placeholder="99" />
          <FormField label="Currency" value={form.currency} onChange={(v) => setForm({ ...form, currency: v })} options={[{ value: 'INR', label: 'INR' }, { value: 'USD', label: 'USD' }]} />
        </div>
        <div className="form-row">
          <FormField label="Billing Period" value={form.billingPeriod} onChange={(v) => setForm({ ...form, billingPeriod: v })} options={[{ value: 'MONTHLY', label: 'Monthly' }, { value: 'YEARLY', label: 'Yearly' }]} />
          <FormField label="Tax Mode" value={form.taxMode} onChange={(v) => setForm({ ...form, taxMode: v })} options={[{ value: 'EXCLUSIVE', label: 'Exclusive' }, { value: 'INCLUSIVE', label: 'Inclusive' }]} />
        </div>
      </AdminModal>
      <AdminModal
        isOpen={!!confirmToggle}
        title="Confirm Status Change"
        onClose={() => setConfirmToggle(null)}
        onSave={() => confirmToggle && handleToggle(confirmToggle.id)}
        saveLabel={confirmToggle?.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        saving={saving}
      >
        <p style={{ color: '#4B5563', fontFamily: 'Inter, sans-serif' }}>
          Are you sure you want to {confirmToggle?.status === 'ACTIVE' ? 'deactivate' : 'activate'} <strong>{confirmToggle?.name}</strong>?
        </p>
      </AdminModal>
    </>
  );
};
