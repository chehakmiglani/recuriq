import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { DataTable } from '../../../components/admin/shared/DataTable';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { getCoupons, createCoupon, updateCoupon, toggleCouponStatus } from '../../../services/admin/adminService';
import type { Coupon } from '../../../services/admin/adminTypes';
import { errorMessage } from '../../../utils/apiError';
const emptyForm = { code: '', name: '', type: 'PERCENT', amount: '', currency: 'INR', duration: 'ONCE', durationInMonths: '', maxRedemptions: '', validFrom: new Date().toISOString().split('T')[0], validTo: '' };
export const CouponsPage: React.FC = () => {
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [confirmToggle, setConfirmToggle] = useState<Coupon | null>(null);
  const load = () => { getCoupons().then(setCoupons).catch(console.error).finally(() => setLoading(false)); };
  useEffect(() => { load(); }, []);
  const openCreate = () => { setForm(emptyForm); setEditId(null); setModalOpen(true); };
  const openEdit = (row: Coupon) => {
    setForm({ code: row.code, name: row.name, type: row.type, amount: String(row.type === 'PERCENT' ? row.amount : row.amount / 100), currency: row.currency || 'INR', duration: row.duration, durationInMonths: row.durationInMonths ? String(row.durationInMonths) : '', maxRedemptions: row.maxRedemptions ? String(row.maxRedemptions) : '', validFrom: row.validFrom || '', validTo: row.validTo || '' });
    setEditId(row.id); setModalOpen(true);
  };
  const handleSave = async () => {
    setSaving(true);
    const payload = { ...form, amount: form.type === 'PERCENT' ? Number(form.amount) : Math.round(Number(form.amount) * 100), durationInMonths: form.durationInMonths ? Number(form.durationInMonths) : null, maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null, validTo: form.validTo || null };
    try {
      if (editId) await updateCoupon(editId, payload);
      else await createCoupon(payload);
      setModalOpen(false); load();
    } catch (e) { alert(errorMessage(e)); }
    finally { setSaving(false); }
  };
  const handleToggle = async (id: number) => {
    setSaving(true);
    await toggleCouponStatus(id);
    setSaving(false);
    setConfirmToggle(null);
    load();
  };
  const columns = [
    { key: 'code', header: 'Code', render: (r: Coupon) => <span style={{ fontWeight: 700, color: '#0f766e', fontFamily: 'Inter, sans-serif', fontSize: '13px' }}>{r.code}</span> },
    { key: 'name', header: 'Name' },
    { key: 'type', header: 'Type' },
    { key: 'amount', header: 'Amount', render: (r: Coupon) => r.type === 'PERCENT' ? `${r.amount}%` : `₹${r.amount / 100}` },
    { key: 'duration', header: 'Duration' },
    { key: 'redeemedCount', header: 'Redeemed', render: (r: Coupon) => `${r.redeemedCount} / ${r.maxRedemptions ?? '∞'}` },
    { key: 'status', header: 'Status', render: (r: Coupon) => <StatusBadge status={r.status} /> },
    {
      key: 'actions', header: 'Actions', render: (r: Coupon) => (
        <button className={`btn-admin-sm ${r.status === 'ACTIVE' ? 'btn-toggle-active' : 'btn-toggle-inactive'}`} onClick={(e) => { e.stopPropagation(); setConfirmToggle(r); }}>
          {r.status === 'ACTIVE' ? 'Disable' : 'Enable'}
        </button>
      )
    },
  ];
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading coupons...</div>;
  return (
    <>
      <PageHeader subtitle="Create and manage promotional discount codes." actionLabel="Create Coupon" onAction={openCreate} />
      <div className="data-panel">
        <DataTable columns={columns} data={coupons} emptyMessage="No coupons yet." onRowClick={openEdit} />
      </div>
      <AdminModal isOpen={modalOpen} title={editId ? 'Edit Coupon' : 'Create Coupon'} onClose={() => setModalOpen(false)} onSave={handleSave} saving={saving}>
        <div className="form-row">
          <FormField label="Coupon Code" value={form.code} onChange={(v) => setForm({ ...form, code: v.toUpperCase() })} required placeholder="WELCOME10" />
          <FormField label="Display Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required placeholder="Welcome 10% Off" />
        </div>
        <div className="form-row">
          <FormField label="Type" value={form.type} onChange={(v) => setForm({ ...form, type: v })} options={[{ value: 'PERCENT', label: 'Percentage' }, { value: 'FIXED', label: 'Fixed Amount' }]} />
          <FormField label={form.type === 'PERCENT' ? 'Discount (%)' : 'Amount (₹)'} value={form.amount} onChange={(v) => setForm({ ...form, amount: v })} type="number" required placeholder={form.type === 'PERCENT' ? '10' : '100'} />
        </div>
        <div className="form-row">
          <FormField label="Duration" value={form.duration} onChange={(v) => setForm({ ...form, duration: v })} options={[{ value: 'ONCE', label: 'Once' }, { value: 'FOREVER', label: 'Forever' }, { value: 'REPEATING', label: 'Repeating' }]} />
          {form.duration === 'REPEATING' && <FormField label="Duration (months)" value={form.durationInMonths} onChange={(v) => setForm({ ...form, durationInMonths: v })} type="number" placeholder="3" />}
        </div>
        <div className="form-row">
          <FormField label="Max Redemptions" value={form.maxRedemptions} onChange={(v) => setForm({ ...form, maxRedemptions: v })} type="number" placeholder="∞ (leave empty)" />
          <FormField label="Valid From" value={form.validFrom} onChange={(v) => setForm({ ...form, validFrom: v })} type="date" />
        </div>
        <FormField label="Valid To (optional)" value={form.validTo} onChange={(v) => setForm({ ...form, validTo: v })} type="date" />
      </AdminModal>
      <AdminModal
        isOpen={!!confirmToggle}
        title="Confirm Status Change"
        onClose={() => setConfirmToggle(null)}
        onSave={() => confirmToggle && handleToggle(confirmToggle.id)}
        saveLabel={confirmToggle?.status === 'ACTIVE' ? 'Disable' : 'Enable'}
        saving={saving}
      >
        <p style={{ color: '#4B5563', fontFamily: 'Inter, sans-serif' }}>
          Are you sure you want to {confirmToggle?.status === 'ACTIVE' ? 'disable' : 'enable'} coupon <strong>{confirmToggle?.code}</strong>?
        </p>
      </AdminModal>
    </>
  );
};
