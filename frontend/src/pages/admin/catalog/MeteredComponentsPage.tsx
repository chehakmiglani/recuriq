import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { getMeteredComponents, createMetered, updateMetered, toggleMeteredStatus, getAllPlans } from '../../../services/admin/adminService';
import type { MeteredComponentResponse, PlanResponse } from '../../../services/admin/adminTypes';
const emptyForm = { name: '', unitName: 'GB', pricePerUnitMinor: '', freeTierQuantity: '0', planId: '' };
export const MeteredComponentsPage: React.FC = () => {
  const [components, setComponents] = useState<MeteredComponentResponse[]>([]);
  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [confirmToggle, setConfirmToggle] = useState<MeteredComponentResponse | null>(null);
  const load = () => {
    Promise.all([getMeteredComponents(), getAllPlans()])
      .then(([mc, pl]) => { setComponents(mc); setPlans(pl); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);
  const openCreate = () => { setForm(emptyForm); setEditId(null); setModalOpen(true); };
  const openEdit = (row: MeteredComponentResponse) => {
    setForm({ name: row.name, unitName: row.unitName, pricePerUnitMinor: String(row.pricePerUnitMinor / 100), freeTierQuantity: String(row.freeTierQuantity), planId: String(row.planId) });
    setEditId(row.id); setModalOpen(true);
  };
  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, pricePerUnitMinor: Math.round(Number(form.pricePerUnitMinor) * 100), freeTierQuantity: Number(form.freeTierQuantity), plan: { id: Number(form.planId) } };
    try {
      if (editId) await updateMetered(editId, payload);
      else await createMetered(payload);
      setModalOpen(false); load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };
  const handleToggle = async (id: number) => {
    setSaving(true);
    await toggleMeteredStatus(id);
    setSaving(false);
    setConfirmToggle(null);
    load();
  };
  const columns = [
    { key: 'name', header: 'Component', render: (r: MeteredComponentResponse) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{r.name}</span> },
    { key: 'planName', header: 'Plan' },
    { key: 'unitName', header: 'Unit' },
    { key: 'pricePerUnitMinor', header: 'Price/Unit', render: (r: MeteredComponentResponse) => `₹${r.pricePerUnitMinor / 100}/${r.unitName}` },
    { key: 'freeTierQuantity', header: 'Free Tier', render: (r: MeteredComponentResponse) => `${r.freeTierQuantity} ${r.unitName}` },
    { key: 'status', header: 'Status', render: (r: MeteredComponentResponse) => <StatusBadge status={r.status} /> },
    {
      key: 'actions', header: 'Actions', render: (r: MeteredComponentResponse) => (
        <button className={`btn-admin-sm ${r.status === 'ACTIVE' ? 'btn-toggle-active' : 'btn-toggle-inactive'}`} onClick={(e) => { e.stopPropagation(); setConfirmToggle(r); }}>
          {r.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        </button>
      )
    },
  ];
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading metered components...</div>;
  return (
    <>
      <PageHeader subtitle="Usage-based billing — free tier included, then per-unit charges." actionLabel="Add Component" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={components} emptyMessage="No metered components yet." onRowClick={openEdit} />
      </div>
      <AdminModal isOpen={modalOpen} title={editId ? 'Edit Component' : 'Add Component'} onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <FormField label="Component Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required placeholder="e.g. Download Storage" />
        <FormField label="Linked Plan" value={form.planId} onChange={(v) => setForm({ ...form, planId: v })} required options={plans.map(p => ({ value: String(p.id), label: p.name }))} />
        <div className="form-row">
          <FormField label="Unit Name" value={form.unitName} onChange={(v) => setForm({ ...form, unitName: v })} placeholder="GB" />
          <FormField label="Price per Unit (₹)" value={form.pricePerUnitMinor} onChange={(v) => setForm({ ...form, pricePerUnitMinor: v })} type="number" required placeholder="20" />
        </div>
        <FormField label="Free Tier Quantity" value={form.freeTierQuantity} onChange={(v) => setForm({ ...form, freeTierQuantity: v })} type="number" placeholder="5" />
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