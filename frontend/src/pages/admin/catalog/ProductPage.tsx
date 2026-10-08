import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../../components/admin/shared/PageHeader';
import { StatusBadge } from '../../../components/shared/StatusBadge';
import { AdminModal } from '../../../components/admin/shared/AdminModal';
import { FormField } from '../../../components/admin/shared/FormField';
import { Package } from 'lucide-react';
import { getAllProducts, updateProduct, toggleProductStatus } from '../../../services/admin/adminService';
import type { Product } from '../../../services/admin/adminTypes';
export const ProductPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [confirmToggle, setConfirmToggle] = useState<Product | null>(null);
  const load = () => {
    getAllProducts().then(setProducts).catch(console.error).finally(() => setLoading(false));
  };
  useEffect(() => { load(); }, []);
  const openEdit = (p: Product) => {
    setForm({ name: p.name, description: p.description });
    setEditing(p);
  };
  const handleSave = async () => {
    if (!editing) return;
    setSaving(true);
    try {
      await updateProduct(editing.id, form);
      setEditing(null);
      load();
    } catch (e: any) { alert(e.message); }
    finally { setSaving(false); }
  };
  const handleToggle = async (id: number) => {
    setSaving(true);
    await toggleProductStatus(id);
    setSaving(false);
    setConfirmToggle(null);
    load();
  };
  if (loading) return <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center', fontFamily: 'Inter, sans-serif' }}>Loading products...</div>;
  return (
    <>
      <PageHeader subtitle="Manage the StreamFlix streaming platform product." />
      <div className="data-panel">
        {products.length === 0 ? (
          <div className="empty-state">
            <Package size={40} className="empty-state-icon" />
            <div className="empty-state-text">No products found</div>
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                      <div className="product-icon-box" style={{
                        width: '48px',
                        height: '48px',
                        borderRadius: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#0f766e',
                        backgroundColor: '#f0fdfa',
                        border: '1px solid #e5e7eb'
                      }}>
                        <Package size={24} />
                      </div>
                      <div>
                        <div style={{ fontSize: '15px', fontWeight: 600, color: '#0f172a' }}>{product.name}</div>
                        <div style={{ fontSize: '13px', color: '#64748b', marginTop: '2px' }}>{product.description}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <StatusBadge status={product.status} />
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div className="table-actions" style={{ justifyContent: 'flex-end' }}>
                      <button
                        className="btn-admin-secondary"
                        onClick={() => setConfirmToggle(product)}
                        style={{ padding: '6px 12px', fontSize: '12px' }}
                      >
                        {product.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        className="btn-admin-primary"
                        onClick={() => openEdit(product)}
                        style={{ padding: '6px 12px', fontSize: '12px' }}
                      >
                        Edit
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <AdminModal isOpen={!!editing} title="Edit Product" onClose={() => setEditing(null)} onSave={handleSave} saving={saving}>
        <FormField label="Product Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} required />
        <FormField label="Description" value={form.description} onChange={(v) => setForm({ ...form, description: v })} />
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