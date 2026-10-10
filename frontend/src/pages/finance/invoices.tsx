import React, { useEffect, useState } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { type Column, FinanceDataTable } from '../../components/finance/components/finanaceDataTable';
import { financeService } from '../../services/finance/financeService';
import type { Invoice, InvoiceLineItem } from '../../types/financeTypes';
import { X, Receipt, FileText } from 'lucide-react';

export const InvoicesPage: React.FC = () => {
  const [data, setData] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [lineItemsLoading, setLineItemsLoading] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const result = await financeService.getInvoices(page, 10);
        setData(result.data);
        setTotal(result.total);
      } catch (error) {
        console.error('Error fetching invoices:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [page]);

  const columns: Column<Invoice>[] = [
    { header: 'Invoice ID', accessor: (row) => <span style={{ fontWeight: 600, color: '#1f2937', fontFamily: 'Inter, sans-serif' }}>{row.id}</span> },
    { header: 'Customer ID', accessor: 'customerId' },
    { header: 'Amount', accessor: (row) => `₹${row.amount.toFixed(2)}` },
    { header: 'Date', accessor: (row) => new Date(row.date).toLocaleDateString() },
    { header: 'Due Date', accessor: (row) => new Date(row.dueDate).toLocaleDateString() },
    { header: 'Status', accessor: (row) => <StatusBadge status={row.status} /> },
  ];

  const handleRowClick = async (invoice: Invoice) => {
    setLineItemsLoading(true);
    setSelectedInvoice(invoice);
    setIsDrawerOpen(true);
    setIsClosing(false);
    
    try {
      const detail = await financeService.getInvoiceDetail(invoice.id);
      setSelectedInvoice(detail);
    } catch (error) {
      console.error('Error fetching invoice details:', error);
    } finally {
      setLineItemsLoading(false);
    }
  };

  const closeDrawer = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsDrawerOpen(false);
      setSelectedInvoice(null);
      setIsClosing(false);
    }, 300); // Wait for closing animation
  };

  const calculateSubtotal = (items: InvoiceLineItem[]) => {
    return items
      .filter((item) => item.lineType !== 'TAX' && item.lineType !== 'CREDIT')
      .reduce((sum, item) => sum + item.amountMinor / 100, 0);
  };

  const calculateTax = (items: InvoiceLineItem[]) => {
    return items
      .filter((item) => item.lineType === 'TAX')
      .reduce((sum, item) => sum + item.amountMinor / 100, 0);
  };

  const calculateCredit = (items: InvoiceLineItem[]) => {
    return items
      .filter((item) => item.lineType === 'CREDIT')
      .reduce((sum, item) => sum + item.amountMinor / 100, 0);
  };

  return (
    <>
      <PageHeader subtitle="Manage and monitor customer invoices." />

      {loading ? (
        <div style={{ color: '#9ca3af', padding: '40px', textAlign: 'center' }}>Loading invoices...</div>
      ) : (
        <>
          <FinanceDataTable
            columns={columns}
            data={data}
            keyExtractor={(row) => row.id}
            onRowClick={handleRowClick}
          />

          {/* Pagination Controls */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
              Showing {(page - 1) * 10 + 1} to {Math.min(page * 10, total)} of {total} entries
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
                className="btn-admin-sm"
                style={{ background: '#fff', border: '1px solid #e5e7eb', color: page === 1 ? '#9ca3af' : '#1f2937' }}
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={page * 10 >= total}
                className="btn-admin-sm"
                style={{ background: '#fff', border: '1px solid #e5e7eb', color: page * 10 >= total ? '#9ca3af' : '#1f2937' }}
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}

      {/* Slide-over Drawer for Invoice Details */}
      {isDrawerOpen && selectedInvoice && (
        <>
          <div className="drawer-backdrop" onClick={closeDrawer} />
          <div className={`drawer-panel ${isClosing ? 'closing' : ''}`}>
            
            {/* Drawer Header */}
            <div className="drawer-header">
              <div>
                <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 600, color: '#111827', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Receipt size={20} color="#0f766e" />
                  Invoice {selectedInvoice.id}
                </h2>
                <div style={{ marginTop: '8px' }}>
                  <StatusBadge status={selectedInvoice.status} />
                </div>
              </div>
              <button
                onClick={closeDrawer}
                style={{
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  padding: '4px',
                  color: '#6b7280',
                  borderRadius: '50%',
                  transition: 'background 0.2s, color 0.2s',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = '#e5e7eb'; e.currentTarget.style.color = '#1f2937'; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#6b7280'; }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Drawer Body */}
            <div className="drawer-body">
              {/* Summary Cards */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '32px' }}>
                <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                  <p style={{ margin: 0, fontSize: '12px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>Customer Info</p>
                  <p style={{ margin: '8px 0 0 0', fontSize: '14px', color: '#0f172a', fontWeight: 500 }}>Customer ID: {selectedInvoice.customerId}</p>
                </div>
                <div style={{ background: '#f8fafc', padding: '16px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                  <p style={{ margin: 0, fontSize: '12px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>Timeline</p>
                  <p style={{ margin: '8px 0 0 0', fontSize: '14px', color: '#0f172a', fontWeight: 500 }}>Issued: {new Date(selectedInvoice.date).toLocaleDateString()}</p>
                  <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: '#0f172a', fontWeight: 500 }}>Due: {new Date(selectedInvoice.dueDate).toLocaleDateString()}</p>
                </div>
              </div>

              {/* Line Items Table */}
              <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#1e293b', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <FileText size={16} /> Line Items
              </h3>
              
              {lineItemsLoading ? (
                <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>Loading line items...</div>
              ) : selectedInvoice.lineItems && selectedInvoice.lineItems.length > 0 ? (
                <div className="admin-table-container" style={{ border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
                  <table className="admin-table" style={{ width: '100%', margin: 0 }}>
                    <thead>
                      <tr>
                        <th style={{ textAlign: 'left' }}>Description</th>
                        <th style={{ textAlign: 'right' }}>Qty</th>
                        <th style={{ textAlign: 'right' }}>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedInvoice.lineItems.map((item) => (
                        <tr key={item.lineItemId}>
                          <td>
                            <div style={{ fontWeight: 500 }}>{item.description}</div>
                            {item.periodStart && item.periodEnd && (
                              <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>
                                {new Date(item.periodStart).toLocaleDateString()} - {new Date(item.periodEnd).toLocaleDateString()}
                              </div>
                            )}
                          </td>
                          <td style={{ textAlign: 'right' }}>{item.quantity}</td>
                          <td style={{ textAlign: 'right', fontWeight: 500 }}>₹{(item.amountMinor / 100).toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8', background: '#f8fafc', borderRadius: '12px', border: '1px dashed #cbd5e1' }}>
                  No line items found for this invoice.
                </div>
              )}
            </div>

            {/* Drawer Footer */}
            {selectedInvoice.lineItems && selectedInvoice.lineItems.length > 0 && !lineItemsLoading && (
              <div className="drawer-footer">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end', width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', width: '200px', fontSize: '14px', color: '#64748b' }}>
                    <span>Subtotal:</span>
                    <span>₹{calculateSubtotal(selectedInvoice.lineItems).toFixed(2)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', width: '200px', fontSize: '14px', color: '#64748b' }}>
                    <span>Tax:</span>
                    <span>₹{calculateTax(selectedInvoice.lineItems).toFixed(2)}</span>
                  </div>
                  {calculateCredit(selectedInvoice.lineItems) !== 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '200px', fontSize: '14px', color: '#64748b' }}>
                      <span>Credit Applied:</span>
                      <span>₹{calculateCredit(selectedInvoice.lineItems).toFixed(2)}</span>
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', width: '200px', fontSize: '16px', fontWeight: 600, color: '#0f172a', paddingTop: '8px', borderTop: '1px solid #cbd5e1' }}>
                    <span>Total:</span>
                    <span>₹{selectedInvoice.amount.toFixed(2)}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
};
