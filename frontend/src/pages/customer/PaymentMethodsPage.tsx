import React, { useEffect, useState } from 'react';
import * as CustomerService from '../../services/customer/customerService';
import {
  CreditCard,
  Smartphone,
  Plus,
  Trash2,
  Star,
  X,
  Wallet,
  ShieldCheck
} from 'lucide-react';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import './PaymentMethodsPage.css';

export const PaymentMethodsPage: React.FC = () => {
  const [paymentMethods, setPaymentMethods] = useState<CustomerService.PaymentMethod[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [paymentType, setPaymentType] = useState<'CARD' | 'UPI'>('CARD');
  const [cardNumber, setCardNumber] = useState('');
  const [expiryMonth, setExpiryMonth] = useState('');
  const [expiryYear, setExpiryYear] = useState('');
  const [cvv, setCvv] = useState('');
  const [upiId, setUpiId] = useState('');
  const [isDefault, setIsDefault] = useState(false);
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

  useEffect(() => {
    loadPaymentMethods();
  }, []);

  const loadPaymentMethods = async () => {
    try {
      const methods = await CustomerService.getPaymentMethods();
      setPaymentMethods(methods);
    } catch (error) {
      console.error('Failed to load payment methods:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddPaymentMethod = async () => {
    const data: Parameters<typeof CustomerService.addPaymentMethod>[0] = {
      paymentType,
      isDefault
    };

    if (paymentType === 'CARD') {
      if (!cardNumber || cardNumber.length < 16) {
        alert('Please enter a valid card number');
        return;
      }
      data.cardNumber = cardNumber;
      data.expiryMonth = parseInt(expiryMonth);
      data.expiryYear = parseInt(expiryYear);
      data.cvv = cvv;
    } else {
      if (!upiId || !upiId.includes('@')) {
        alert('Please enter a valid UPI ID (e.g., name@upi)');
        return;
      }
      data.upiId = upiId;
    }

    setSaving(true);
    try {
      await CustomerService.addPaymentMethod(data);
      await loadPaymentMethods();
      setShowAddModal(false);
      resetForm();
    } catch (error) {
      alert('Failed to add payment method');
    } finally {
      setSaving(false);
    }
  };

  const handleSetDefault = async (id: number) => {
    try {
      await CustomerService.setDefaultPaymentMethod(id);
      await loadPaymentMethods();
    } catch (error) {
      alert('Failed to set default payment method');
    }
  };

  const handleDelete = (id: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Remove Payment Method',
      message: 'Are you sure you want to remove this payment method? This action cannot be undone.',
      isDanger: true,
      confirmLabel: 'Remove Method',
      onConfirm: async () => {
        closeConfirmDialog();
        try {
          await CustomerService.deletePaymentMethod(id);
          await loadPaymentMethods();
        } catch (error: any) {
          alert(error.message || 'Failed to delete payment method');
        }
      }
    });
  };

  const resetForm = () => {
    setPaymentType('CARD');
    setCardNumber('');
    setExpiryMonth('');
    setExpiryYear('');
    setCvv('');
    setUpiId('');
    setIsDefault(false);
  };

  const formatCardNumber = (value: string) => {
    return value.replace(/\D/g, '').slice(0, 16);
  };

  const getPaymentIcon = (type: string) => {
    return type === 'CARD' ? (
      <CreditCard size={24} className="payment-type-icon card" />
    ) : (
      <Smartphone size={24} className="payment-type-icon upi" />
    );
  };

  if (loading) {
    return (
      <div className="payment-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading payment methods...</p>
      </div>
    );
  }

  return (
    <div className="payment-methods-page">
      <div className="payment-methods-actions" style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '24px' }}>
        <button
          onClick={() => setShowAddModal(true)}
          className="btn-primary"
        >
          <Plus size={20} /> Add Method
        </button>
      </div>

      {/* Payment Methods List */}
      {paymentMethods.length > 0 ? (
        <div className="payment-methods-list">
          {paymentMethods.map((method) => (
            <div
              key={method.paymentMethodId}
              className={`payment-method-card ${method.isDefault ? 'default' : ''}`}
            >
              <div className="payment-method-header">
                <div className="payment-method-icon">
                  {getPaymentIcon(method.paymentType)}
                </div>
                <div className="payment-method-details">
                  <h3 className="payment-name">
                    {method.paymentType === 'CARD'
                      ? `${method.cardBrand || 'Card'} •••• ${method.cardLast4}`
                      : `${method.upiId}`}
                  </h3>
                  {method.paymentType === 'CARD' && (
                    <p className="payment-meta">
                      Expires {method.expiryMonth?.toString().padStart(2, '0')}/{method.expiryYear}
                    </p>
                  )}
                  {method.paymentType === 'UPI' && (
                    <p className="payment-meta">UPI ID</p>
                  )}
                </div>
              </div>
              
              <div className="payment-method-footer">
                {method.isDefault ? (
                  <span className="default-badge">
                    <ShieldCheck size={14} /> Default
                  </span>
                ) : (
                  <button
                    onClick={() => handleSetDefault(method.paymentMethodId)}
                    className="btn-set-default"
                  >
                    <Star size={14} /> Set Default
                  </button>
                )}
                <button
                  onClick={() => handleDelete(method.paymentMethodId)}
                  className="btn-delete-method"
                  title="Remove"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <div className="empty-icon-container">
            <Wallet size={48} className="empty-icon" />
          </div>
          <h3 className="empty-title">No payment methods</h3>
          <p className="empty-text">Add a payment method to manage your subscriptions and billing</p>
          <button
            onClick={() => setShowAddModal(true)}
            className="btn-primary empty-action"
          >
            <Plus size={20} /> Add Payment Method
          </button>
        </div>
      )}

      {/* Add Payment Method Modal */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal payment-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Add Payment Method</h3>
              <button 
                onClick={() => setShowAddModal(false)} 
                className="modal-close"
              >
                <X size={24} />
              </button>
            </div>

            {/* Type Selection */}
            <div className="payment-type-selector">
              <button
                className={`type-btn ${paymentType === 'CARD' ? 'active' : ''}`}
                onClick={() => setPaymentType('CARD')}
              >
                <CreditCard size={20} /> Card
              </button>
              <button
                className={`type-btn ${paymentType === 'UPI' ? 'active' : ''}`}
                onClick={() => setPaymentType('UPI')}
              >
                <Smartphone size={20} /> UPI
              </button>
            </div>

            {/* Card Form */}
            {paymentType === 'CARD' && (
              <div className="card-form">
                <div className="form-group">
                  <label className="form-label">Card Number</label>
                  <input
                    type="text"
                    className="form-input"
                    value={cardNumber}
                    onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
                    placeholder="1234 5678 9012 3456"
                    maxLength={16}
                  />
                </div>
                <div className="form-row three-col">
                  <div className="form-group">
                    <label className="form-label">Month</label>
                    <input
                      type="text"
                      className="form-input"
                      value={expiryMonth}
                      onChange={(e) => setExpiryMonth(e.target.value.replace(/\D/g, '').slice(0, 2))}
                      placeholder="MM"
                      maxLength={2}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Year</label>
                    <input
                      type="text"
                      className="form-input"
                      value={expiryYear}
                      onChange={(e) => setExpiryYear(e.target.value.replace(/\D/g, '').slice(0, 4))}
                      placeholder="YYYY"
                      maxLength={4}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">CVV</label>
                    <input
                      type="password"
                      className="form-input"
                      value={cvv}
                      onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, 4))}
                      placeholder="123"
                      maxLength={4}
                    />
                  </div>
                </div>
              </div>
            )}

            {/* UPI Form */}
            {paymentType === 'UPI' && (
              <div className="form-group">
                <label className="form-label">UPI ID</label>
                <input
                  type="text"
                  className="form-input"
                  value={upiId}
                  onChange={(e) => setUpiId(e.target.value)}
                  placeholder="username@upi"
                />
                <p className="form-hint">Enter your UPI ID (e.g., name@okaxis, name@paytm)</p>
              </div>
            )}

            {/* Default Checkbox */}
            <label className="checkbox-label">
              <input
                type="checkbox"
                className="checkbox-input"
                checked={isDefault}
                onChange={(e) => setIsDefault(e.target.checked)}
              />
              <span className="checkbox-text">Set as default payment method</span>
            </label>

            {/* Security Note */}
            <div className="security-note">
              <ShieldCheck size={16} />
              <span>Your payment information is securely encrypted</span>
            </div>

            {/* Actions */}
            <div className="modal-actions">
              <button
                onClick={handleAddPaymentMethod}
                className="btn-primary btn-full"
                disabled={saving}
              >
                {saving ? 'Adding...' : `Add ${paymentType === 'CARD' ? 'Card' : 'UPI'}`}
              </button>
              <button
                onClick={() => {
                  setShowAddModal(false);
                  resetForm();
                }}
                className="btn-secondary btn-full"
              >
                Cancel
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
      />
    </div>
  );
};
