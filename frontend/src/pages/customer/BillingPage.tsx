import React, { useEffect, useState } from 'react';
import * as CustomerService from '../../services/customer/customerService';
import {
  Download,
  CheckCircle,
  XCircle,
  Clock,
  CreditCard,
  Receipt,
  FileText,
  DollarSign
} from 'lucide-react';
import './BillingPage.css';

export const BillingPage: React.FC = () => {
  const [subscription, setSubscription] = useState<CustomerService.Subscription | null>(null);
  const [invoices, setInvoices] = useState<CustomerService.Invoice[]>([]);
  const [payments, setPayments] = useState<CustomerService.Payment[]>([]);
  const [creditNotes, setCreditNotes] = useState<CustomerService.CreditNote[]>([]);
  const [activeTab, setActiveTab] = useState<'invoices' | 'payments' | 'credit-notes'>('invoices');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBillingData();
  }, []);

  const loadBillingData = async () => {
    try {
      const [sub, inv, pay, cn] = await Promise.all([
        CustomerService.getCurrentSubscription(),
        CustomerService.getInvoices(),
        CustomerService.getPayments(),
        CustomerService.getCreditNotes()
      ]);
      setSubscription(sub);
      setInvoices(inv);
      setPayments(pay);
      setCreditNotes(cn);
    } catch (error) {
      console.error('Failed to load billing data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatAmount = (amount: number, currency: string) => {
    const locale = currency === 'USD' ? 'en-US' : currency === 'GBP' ? 'en-GB' : 'en-IN';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'INR'
    }).format(amount / 100);
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN');
  };

  const handleDownloadInvoice = async (invoiceId: number) => {
    try {
      const blob = await CustomerService.downloadInvoice(invoiceId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice-${invoiceId}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('Failed to download invoice');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'paid':
      case 'success':
      case 'issued':
      case 'applied':
        return '#22c55e';
      case 'failed':
      case 'void':
      case 'voided':
        return '#5b4fff';
      case 'pending':
      case 'open':
      case 'draft':
        return '#f59e0b';
      default:
        return '#6b7280';
    }
  };

  const getStatusIcon = (status: string) => {
    const color = getStatusColor(status);
    switch (status.toLowerCase()) {
      case 'paid':
      case 'success':
      case 'issued':
      case 'applied':
        return <CheckCircle size={16} style={{ color }} />;
      case 'failed':
      case 'void':
      case 'voided':
        return <XCircle size={16} style={{ color }} />;
      default:
        return <Clock size={16} style={{ color }} />;
    }
  };

  if (loading) {
    return (
      <div className="billing-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading billing information...</p>
      </div>
    );
  }

  return (
    <div className="billing-page">
      {/* Summary Cards */}
      <div className="billing-summary">
        <div className="summary-card">
          <div className="summary-icon" style={{ backgroundColor: '#E8F5E9', color: '#22c55e' }}>
            <FileText size={24} />
          </div>
          <div className="summary-info">
            <p className="summary-value">{invoices.length}</p>
            <p className="summary-label">Total Invoices</p>
          </div>
        </div>
        <div className="summary-card">
          <div className="summary-icon" style={{ backgroundColor: '#FEF3C7', color: '#f59e0b' }}>
            <DollarSign size={24} />
          </div>
          <div className="summary-info">
            <p className="summary-value">
              {subscription 
                ? formatAmount(subscription.totalDueMinor || 0, subscription.currency)
                : formatAmount(0, 'INR')}
            </p>
            <p className="summary-label">Next Payment</p>
          </div>
        </div>
        <div className="summary-card">
          <div className="summary-icon" style={{ backgroundColor: '#FFEBEE', color: '#dc2626' }}>
            <Receipt size={24} />
          </div>
          <div className="summary-info">
            <p className="summary-value">
              {formatAmount(
                invoices.filter(i => i.status === 'OPEN').reduce((sum, i) => sum + i.balanceMinor, 0),
                subscription?.currency || 'INR'
              )}
            </p>
            <p className="summary-label">Amount Due</p>
          </div>
        </div>
        <div className="summary-card">
          <div className="summary-icon" style={{ backgroundColor: '#E0F2FE', color: '#3b82f6' }}>
            <CreditCard size={24} />
          </div>
          <div className="summary-info">
            <p className="summary-value">{payments.length}</p>
            <p className="summary-label">Payments</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="billing-tabs">
        <button
          className={`tab ${activeTab === 'invoices' ? 'active' : ''}`}
          onClick={() => setActiveTab('invoices')}
        >
          <Receipt size={18} />
          Invoices
          <span className="tab-count">{invoices.length}</span>
        </button>
        <button
          className={`tab ${activeTab === 'payments' ? 'active' : ''}`}
          onClick={() => setActiveTab('payments')}
        >
          <CreditCard size={18} />
          Payments
          <span className="tab-count">{payments.length}</span>
        </button>
        <button
          className={`tab ${activeTab === 'credit-notes' ? 'active' : ''}`}
          onClick={() => setActiveTab('credit-notes')}
        >
          <FileText size={18} />
          Credit Notes
          <span className="tab-count">{creditNotes.length}</span>
        </button>
      </div>

      {/* Invoices Tab */}
      {activeTab === 'invoices' && (
        <div className="tab-content">
          {invoices.length > 0 ? (
            <div className="invoices-list">
              {invoices.map((invoice) => (
                <div key={invoice.invoiceId} className="invoice-card">
                  <div className="invoice-header">
                    <div className="invoice-info">
                      <h4 className="invoice-number">{invoice.invoiceNumber}</h4>
                      <p className="invoice-date">
                        {formatDate(invoice.issueDate)}
                        {invoice.dueDate && (
                          <span className="due-date"> • Due {formatDate(invoice.dueDate)}</span>
                        )}
                      </p>
                    </div>
                    <span
                      className="status-badge"
                      style={{
                        backgroundColor: `${getStatusColor(invoice.status)}20`,
                        color: getStatusColor(invoice.status)
                      }}
                    >
                      {getStatusIcon(invoice.status)}
                      {invoice.status}
                    </span>
                  </div>
                  <div className="invoice-footer">
                    <p className="invoice-amount">{formatAmount(invoice.totalMinor, invoice.currency)}</p>
                    <button
                      onClick={() => handleDownloadInvoice(invoice.invoiceId)}
                      className="btn-download"
                      title="Download PDF"
                    >
                      <Download size={18} />
                      Download
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <Receipt size={48} className="empty-icon" />
              <p className="empty-text">No invoices found</p>
              <p className="empty-hint">Your billing history will appear here</p>
            </div>
          )}
        </div>
      )}

      {/* Payments Tab */}
      {activeTab === 'payments' && (
        <div className="tab-content">
          {payments.length > 0 ? (
            <div className="payments-list">
              {payments.map((payment) => (
                <div key={payment.paymentId} className="payment-card">
                  <div className="payment-info">
                    <div className="payment-header">
                      <h4 className="payment-id">#{payment.paymentId}</h4>
                      <span
                        className="status-badge"
                        style={{
                          backgroundColor: `${getStatusColor(payment.status)}20`,
                          color: getStatusColor(payment.status)
                        }}
                      >
                        {getStatusIcon(payment.status)}
                        {payment.status}
                      </span>
                    </div>
                    <p className="payment-invoice">{payment.invoiceNumber}</p>
                    <p className="payment-date">{formatDate(payment.createdAt)}</p>
                  </div>
                  <p className="payment-amount">{formatAmount(payment.amountMinor, payment.currency)}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <CreditCard size={48} className="empty-icon" />
              <p className="empty-text">No payments found</p>
              <p className="empty-hint">Your payment history will appear here</p>
            </div>
          )}
        </div>
      )}

      {/* Credit Notes Tab */}
      {activeTab === 'credit-notes' && (
        <div className="tab-content">
          {creditNotes.length > 0 ? (
            <div className="credit-notes-list">
              {creditNotes.map((cn) => (
                <div key={cn.creditNoteId} className="credit-note-card">
                  <div className="credit-note-info">
                    <div className="credit-note-header">
                      <h4 className="credit-note-number">{cn.creditNoteNumber}</h4>
                      <span
                        className="status-badge"
                        style={{
                          backgroundColor: `${getStatusColor(cn.status)}20`,
                          color: getStatusColor(cn.status)
                        }}
                      >
                        {getStatusIcon(cn.status)}
                        {cn.status}
                      </span>
                    </div>
                    <p className="credit-note-invoice">{cn.invoiceNumber}</p>
                    <p className="credit-note-date">{formatDate(cn.createdAt)}</p>
                  </div>
                  <p className="credit-note-amount">{formatAmount(cn.amountMinor, subscription?.currency || 'INR')}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <FileText size={48} className="empty-icon" />
              <p className="empty-text">No credit notes found</p>
              <p className="empty-hint">Credit notes will appear here</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
