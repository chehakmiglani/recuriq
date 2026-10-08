import React, { useState, useEffect } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { DataTable } from '../../components/admin/shared/DataTable';
import { 
  getPendingCancellationRequests, 
  approveCancellationRequest, 
  rejectCancellationRequest 
} from '../../services/support/supportService';
import type { CancellationRequest } from '../../services/customer/customerService';
import { Check, X } from 'lucide-react';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';

export const CancellationRequestsPage: React.FC = () => {
  const [requests, setRequests] = useState<CancellationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<CancellationRequest | null>(null);
  const [modalType, setModalType] = useState<'APPROVE' | 'REJECT' | null>(null);
  const [agentNotes, setAgentNotes] = useState('');
  
  // Refund details modal
  const [refundInfo, setRefundInfo] = useState<{
    refundIssued: boolean;
    refundAmountMinor: number;
    currency: string;
    refundGatewayRef: string | null;
    creditNoteNumber: string | null;
    message: string;
  } | null>(null);

  // Confirm Dialog State
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    isDanger?: boolean;
    confirmLabel?: string;
    cancelLabel?: string;
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {},
  });

  const closeConfirmDialog = () => {
    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
  };

  const showAlert = (title: string, message: string) => {
    setConfirmDialog({
      isOpen: true,
      title,
      message,
      confirmLabel: 'OK',
      cancelLabel: '', // hides the cancel button
      onConfirm: closeConfirmDialog,
    });
  };

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const data = await getPendingCancellationRequests();
      setRequests(data);
    } catch (error) {
      console.error('Failed to load cancellation requests:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleOpenModal = (request: CancellationRequest, type: 'APPROVE' | 'REJECT') => {
    setSelectedRequest(request);
    setModalType(type);
    setAgentNotes('');
  };

  const handleCloseModal = () => {
    setSelectedRequest(null);
    setModalType(null);
    setAgentNotes('');
  };

  const handleCloseRefundInfo = async () => {
    setRefundInfo(null);
    await fetchRequests();
  };

  const handleProcessRequest = async () => {
    if (!selectedRequest || !modalType) return;
    
    if (modalType === 'REJECT' && !agentNotes.trim()) {
      showAlert('Validation Error', 'Please provide notes stating the reason for rejection.');
      return;
    }

    setActionLoading(true);
    try {
      if (modalType === 'APPROVE') {
        const response = await approveCancellationRequest(selectedRequest.requestId, agentNotes);
        handleCloseModal();
        if (response && response.refundIssued) {
          setRefundInfo(response);
        } else {
          setConfirmDialog({
            isOpen: true,
            title: 'Approved',
            message: 'Cancellation request approved successfully.',
            confirmLabel: 'OK',
            cancelLabel: '',
            onConfirm: async () => {
              closeConfirmDialog();
              await fetchRequests();
            }
          });
        }
      } else {
        await rejectCancellationRequest(selectedRequest.requestId, agentNotes);
        handleCloseModal();
        setConfirmDialog({
          isOpen: true,
          title: 'Declined',
          message: 'Cancellation request declined successfully.',
          confirmLabel: 'OK',
          cancelLabel: '',
          onConfirm: async () => {
            closeConfirmDialog();
            await fetchRequests();
          }
        });
      }
    } catch (error) {
      showAlert('Error', `Failed to process request: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setActionLoading(false);
    }
  };

  const formatAmount = (amount: number, currency: string) => {
    const locale = currency === 'USD' ? 'en-US' : currency === 'GBP' ? 'en-GB' : 'en-IN';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'INR'
    }).format(amount / 100);
  };

  const columns = [
    { key: 'requestId', header: 'Request ID' },
    {
      key: 'customer',
      header: 'Customer',
      render: (r: CancellationRequest) => (
        <div>
          <div style={{ fontWeight: 600, color: '#1e293b' }}>{r.customerName}</div>
          <div style={{ fontSize: '13px', color: '#64748b' }}>{r.customerEmail}</div>
        </div>
      )
    },
    {
      key: 'subscription',
      header: 'Subscription & Plan',
      render: (r: CancellationRequest) => (
        <div>
          <div style={{ fontWeight: 500, color: '#0f172a' }}>{r.planName}</div>
          <div style={{ fontSize: '12px', color: '#94a3b8' }}>ID: {r.subscriptionId}</div>
        </div>
      )
    },
    {
      key: 'timing',
      header: 'Requested Timing',
      render: (r: CancellationRequest) => (
        <span 
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            padding: '4px 8px',
            borderRadius: '12px',
            fontSize: '12px',
            fontWeight: 500,
            background: r.atPeriodEnd ? '#eff6ff' : '#fff1f2',
            color: r.atPeriodEnd ? '#1d4ed8' : '#b91c1c'
          }}
        >
          {r.atPeriodEnd ? 'End of Period' : 'Immediate'}
        </span>
      )
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (r: CancellationRequest) => (
        <div style={{ maxWidth: '250px', whiteSpace: 'normal', fontSize: '13px', color: '#475569' }}>
          {r.reason || <em style={{ color: '#94a3b8' }}>No reason provided</em>}
        </div>
      )
    },
    {
      key: 'createdAt',
      header: 'Requested On',
      render: (r: CancellationRequest) => new Date(r.createdAt).toLocaleDateString()
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (r: CancellationRequest) => (
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={() => handleOpenModal(r, 'APPROVE')}
            className="btn-success"
            style={{
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            <Check size={14} /> Approve
          </button>
          <button
            onClick={() => handleOpenModal(r, 'REJECT')}
            className="btn-danger"
            style={{
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            <X size={14} /> Decline
          </button>
        </div>
      )
    }
  ];

  return (
    <>
      <PageHeader subtitle="Review and process customer subscription cancellation requests." />

      <div style={{ padding: '0 40px 40px 40px' }}>
        <div className="data-panel" style={{ padding: '24px' }}>
          {loading ? (
            <div style={{ color: '#94a3b8', textAlign: 'center', padding: '24px' }}>
              Loading cancellation requests...
            </div>
          ) : (
            <DataTable 
              columns={columns} 
              data={requests} 
              emptyMessage="No pending cancellation requests found."
            />
          )}
        </div>
      </div>

      {/* Action Processing Modal */}
      {selectedRequest && modalType && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">
                {modalType === 'APPROVE' ? 'Approve Cancellation' : 'Decline Cancellation'}
              </h3>
              <button onClick={handleCloseModal} className="modal-close">
                <X size={20} />
              </button>
            </div>
            
            <div style={{ marginBottom: '1.5rem', background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '13px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ color: '#64748b' }}>Customer:</span>
                <span style={{ fontWeight: 600, color: '#1e293b' }}>{selectedRequest.customerName} ({selectedRequest.customerEmail})</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ color: '#64748b' }}>Plan to Cancel:</span>
                <span style={{ fontWeight: 600, color: '#1e293b' }}>{selectedRequest.planName}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ color: '#64748b' }}>Timing:</span>
                <span style={{ fontWeight: 600, color: selectedRequest.atPeriodEnd ? '#1d4ed8' : '#b91c1c' }}>
                  {selectedRequest.atPeriodEnd ? 'End of Current Billing Period' : 'Immediate Cancel'}
                </span>
              </div>
              <div style={{ marginTop: '8px', borderTop: '1px solid #e2e8f0', paddingTop: '8px' }}>
                <span style={{ color: '#64748b', display: 'block', marginBottom: '4px' }}>Customer Reason:</span>
                <span style={{ color: '#334155', fontStyle: selectedRequest.reason ? 'normal' : 'italic' }}>
                  "{selectedRequest.reason || 'No reason provided'}"
                </span>
              </div>
            </div>

            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: '#475569', marginBottom: '8px', fontSize: '13px', fontWeight: 600 }}>
                Internal Agent Notes {modalType === 'REJECT' && <span style={{ color: '#ef4444' }}>*</span>}
              </label>
              <textarea
                value={agentNotes}
                onChange={(e) => setAgentNotes(e.target.value)}
                placeholder={modalType === 'APPROVE' ? "Optional notes (e.g. approved based on request)" : "Reason for declining (required)..."}
                style={{
                  width: '100%',
                  height: '80px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #cbd5e1',
                  borderRadius: '8px',
                  color: '#1e293b',
                  padding: '10px',
                  fontSize: '14px',
                  resize: 'none',
                  outline: 'none',
                }}
              />
            </div>

            <div className="modal-actions" style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
              <button onClick={handleCloseModal} className="btn-secondary" disabled={actionLoading}>
                Cancel
              </button>
              <button 
                onClick={handleProcessRequest} 
                className={modalType === 'APPROVE' ? 'btn-success' : 'btn-danger'}
                disabled={actionLoading}
              >
                {actionLoading ? 'Processing...' : modalType === 'APPROVE' ? 'Confirm Approval' : 'Confirm Decline'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Refund Details Modal */}
      {refundInfo && (
        <div className="modal-overlay">
          <div className="modal modal-large">
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: '#22c55e' }}>Refund Issued</h3>
            </div>
            <div style={{ textAlign: 'center', padding: '1rem 0' }}>
              <div style={{
                width: 64, height: 64, borderRadius: '50%',
                background: '#f0fdf4',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                margin: '0 auto 1rem', border: '2px solid #bbf7d0'
              }}>
                <Check size={32} style={{ color: '#22c55e' }} />
              </div>
              <p style={{ color: '#1e293b', fontSize: '16px', marginBottom: '0.5rem', fontWeight: 600 }}>
                Subscription Canceled & Refund Processed
              </p>
              <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '1.5rem' }}>
                An immediate cancellation refund has been successfully credited to the customer.
              </p>
              <div style={{
                background: '#f8fafc', borderRadius: '12px', padding: '1rem',
                border: '1px solid #e2e8f0', textAlign: 'left'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <span style={{ color: '#64748b', fontSize: '13px' }}>Refund Amount</span>
                  <span style={{ color: '#22c55e', fontWeight: 600, fontSize: '15px' }}>
                    {formatAmount(refundInfo.refundAmountMinor, refundInfo.currency)}
                  </span>
                </div>
                {refundInfo.refundGatewayRef && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                    <span style={{ color: '#64748b', fontSize: '13px' }}>Transaction ID</span>
                    <span style={{ color: '#334155', fontSize: '13px', fontFamily: 'monospace' }}>
                      {refundInfo.refundGatewayRef}
                    </span>
                  </div>
                )}
                {refundInfo.creditNoteNumber && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#64748b', fontSize: '13px' }}>Credit Note</span>
                    <span style={{ color: '#334155', fontSize: '13px', fontFamily: 'monospace' }}>
                      {refundInfo.creditNoteNumber}
                    </span>
                  </div>
                )}
              </div>
            </div>
            <div className="modal-actions" style={{ justifyContent: 'center' }}>
              <button onClick={handleCloseRefundInfo} className="btn-primary">
                Done
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Shared Confirm Dialog */}
      <ConfirmDialog
        isOpen={confirmDialog.isOpen}
        title={confirmDialog.title}
        message={confirmDialog.message}
        onConfirm={confirmDialog.onConfirm}
        onCancel={closeConfirmDialog}
        isDanger={confirmDialog.isDanger}
        confirmLabel={confirmDialog.confirmLabel}
        cancelLabel={confirmDialog.cancelLabel}
      />
    </>
  );
};
