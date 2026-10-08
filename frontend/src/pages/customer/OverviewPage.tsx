import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  CreditCard,
  Receipt,
  AlertCircle,
  CheckCircle,
  Calendar,
  ArrowRight,
  Plus,
  Package,
  DollarSign,
  Database
} from 'lucide-react';
import * as CustomerService from '../../services/customer/customerService';
import './OverviewPage.css';

export const OverviewPage: React.FC = () => {
  const [subscription, setSubscription] = useState<CustomerService.Subscription | null>(null);
  const [invoices, setInvoices] = useState<CustomerService.Invoice[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<CustomerService.PaymentMethod[]>([]);
  const [notifications, setNotifications] = useState<CustomerService.Notification[]>([]);
  const [isCustomer, setIsCustomer] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      let sub: CustomerService.Subscription | null = null;

      // 1. Try to get subscription / check if customer record exists
      try {
        sub = await CustomerService.getCurrentSubscription();
        setIsCustomer(true);
        setSubscription(sub);
      } catch (err: any) {
        const errMsg = err.message || '';
        if (errMsg.includes('400') || errMsg.includes('404') || errMsg.includes('Customer not found')) {
          setIsCustomer(false);
          setLoading(false);
          return;
        } else {
          throw err;
        }
      }

      // 2. Fetch other customer details in parallel, catching errors individually so they don't block each other
      await Promise.all([
        CustomerService.getInvoices()
          .then(setInvoices)
          .catch(err => console.error('Failed to load invoices:', err)),

        CustomerService.getPaymentMethods()
          .then(setPaymentMethods)
          .catch(err => console.error('Failed to load payment methods:', err)),

        CustomerService.getUnreadNotifications()
          .then(setNotifications)
          .catch(err => console.error('Failed to load notifications:', err))
      ]);

    } catch (error: any) {
      console.error('Failed to load dashboard data:', error.message || error);
    } finally {
      setLoading(false);
    }
  };

  const formatAmount = (amount: number, currency: string) => {
    const locale = currency === 'USD' ? 'en-US' : currency === 'GBP' ? 'en-GB' : 'en-IN';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount / 100);
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  const getNextBillingAmount = () => {
    if (!subscription) return 0;
    return subscription.totalDueMinor || 0;
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
      case 'paid':
      case 'trialing':
        return '#22c55e';
      case 'pending':
      case 'open':
      case 'past_due':
        return '#f59e0b';
      case 'canceled':
      case 'canceled':
      case 'failed':
        return '#dc2626';
      case 'paused':
        return '#3b82f6';
      default:
        return '#6b7280';
    }
  };

  if (loading) {
    return (
      <div className="overview-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading your dashboard...</p>
      </div>
    );
  }

  // Non-customer view - show CTA to subscribe
  if (isCustomer === false) {
    return (
      <div className="overview-container">
        <div className="non-customer-hero">
          <div className="hero-icon">
            <Package size={48} color="#5b4fff" />
          </div>
          <h1 className="hero-title">Welcome to StreamFlix</h1>
          <p className="hero-subtitle">
            You&apos;re almost there! Complete your subscription to start streaming your favorite content.
          </p>
          <Link to="/plans" className="btn-primary-large">
            <Plus size={20} />
            Choose a Plan
          </Link>
        </div>
      </div>
    );
  }

  const handleDismissNotification = async (id: number) => {
    try {
      await CustomerService.markNotificationAsRead(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
    } catch (err) {
      console.error('Failed to dismiss notification', err);
    }
  };

  return (
    <div className="overview-page">
      {/* Notifications Section */}
      {notifications.length > 0 && (
        <div className="notifications-container" style={{ marginBottom: '24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {notifications.map(notif => (
            <div key={notif.id} className="alert-warning" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <AlertCircle size={20} />
                <div>
                  <strong>{notif.subject}</strong>: {notif.body}
                </div>
              </div>
              <button
                onClick={() => handleDismissNotification(notif.id)}
                style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', textDecoration: 'underline' }}
              >
                Dismiss
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Stats Grid */}
      <div className="stats-grid">
        {/* Current Plan Card */}
        <Link to="/dashboard/subscription" className="stat-card stat-card-clickable" style={{ textDecoration: 'none' }}>
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Current Plan</p>
                <p className="stat-value">{subscription?.planName || 'No Active Plan'}</p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <CreditCard size={24} />
              </div>
            </div>
            <p className="stat-subtext" style={{ color: '#5b4fff', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px', margin: 'auto 0 0 0' }}>
              Manage Subscription <ArrowRight size={14} />
            </p>
          </div>
        </Link>

        {/* Next Billing Card */}
        <div className="stat-card">
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Next Billing</p>
                <p className="stat-value">
                  {subscription
                    ? formatDate(subscription.status === 'TRIALING' ? subscription.trialEndDate : subscription.currentPeriodEnd)
                    : 'N/A'}
                </p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <Calendar size={24} />
              </div>
            </div>
            <p className="stat-subtext">
              {subscription?.status === 'TRIALING'
                ? `Amount after trial: ${formatAmount(getNextBillingAmount(), subscription?.currency || 'INR')}`
                : `Amount: ${formatAmount(getNextBillingAmount(), subscription?.currency || 'INR')}`}
            </p>
          </div>
        </div>

        {/* Account Credit Card */}
        <div className="stat-card">
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Account Credit</p>
                <p className="stat-value">
                  {formatAmount(subscription?.creditBalanceMinor || 0, subscription?.currency || 'INR')}
                </p>
              </div>
              <div className="stat-icon" style={{ color: '#22c55e' }}>
                <DollarSign size={24} />
              </div>
            </div>
            <p className="stat-subtext">
              Auto-applied to your next invoice
            </p>
          </div>
        </div>

        {/* Next Payment Card */}
        <div className="stat-card">
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Next Payment</p>
                <p className="stat-value">
                  {formatAmount(getNextBillingAmount(), subscription?.currency || 'INR')}
                </p>
              </div>
              <div className="stat-icon" style={{ color: '#f59e0b' }}>
                <DollarSign size={24} />
              </div>
            </div>
            <p className="stat-subtext">
              Recurring {subscription?.billingPeriod?.toLowerCase() || 'monthly'} charge
            </p>
          </div>
        </div>

        {/* Amount Due Card */}
        <Link to="/dashboard/billing" className="stat-card stat-card-clickable" style={{ textDecoration: 'none' }}>
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Amount Due</p>
                <p className="stat-value">
                  {formatAmount(
                    invoices.filter(i => i.status === 'OPEN').reduce((sum, i) => sum + i.balanceMinor, 0),
                    subscription?.currency || 'INR'
                  )}
                </p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <Receipt size={24} />
              </div>
            </div>
            <p className="stat-subtext" style={{ color: '#5b4fff', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px', margin: 'auto 0 0 0' }}>
              View Invoices <ArrowRight size={14} />
            </p>
          </div>
        </Link>

        {/* Payment Methods Card */}
        <Link to="/dashboard/payment-methods" className="stat-card stat-card-clickable" style={{ textDecoration: 'none' }}>
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Payment Methods</p>
                <p className="stat-value">{paymentMethods.length} Saved</p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <CheckCircle size={24} />
              </div>
            </div>
            <p className="stat-subtext" style={{ color: '#5b4fff', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px', margin: 'auto 0 0 0' }}>
              Manage Methods <ArrowRight size={14} />
            </p>
          </div>
        </Link>


        {/*Metred Component*/}
        <div className="stat-card">
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Download Storage</p>
                <p className="stat-value">7.2 GB</p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <Database size={24} />
              </div>
            </div>
            <p className="stat-subtext">
              2.8 GB of 10 GB free tier used
            </p>
          </div>
        </div>


        {/*Total  Amount Due Card */}
        <Link to="/dashboard/billing" className="stat-card stat-card-clickable" style={{ textDecoration: 'none' }}>
          <div className="stat-card-content">
            <div className="stat-card-header">
              <div>
                <p className="stat-label">Next Billing Cycle Total</p>
                <p className="stat-value">
                  {/* {formatAmount(
                    // invoices.filter(i => i.status === 'OPEN').reduce((sum, i) => sum + i.balanceMinor, 0),
                    // subscription?.currency || 'INR'
                  )} */}
                  {formatAmount(getNextBillingAmount(), subscription?.currency || 'INR')}
                </p>
              </div>
              <div className="stat-icon" style={{ color: '#5b4fff' }}>
                <Receipt size={24} />
              </div>
            </div>
            <p className="stat-subtext">
              Next Payment + Metered Charges
            </p>
            <p className="stat-subtext" style={{ color: '#5b4fff', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px', margin: 'auto 0 0 0' }}>
              View Invoices <ArrowRight size={14} />
            </p>
          </div>
        </Link>
      </div>

      {/* Subscription Status */}
      {subscription && (
        <div className="subscription-card">
          <div className="subscription-header">
            <h2 className="section-title">Subscription Status</h2>
            <span
              className="status-badge"
              style={{
                backgroundColor: `${getStatusColor(subscription.status)}20`,
                color: getStatusColor(subscription.status)
              }}
            >
              {subscription.status === 'TRIALING' ? 'FREE TRIAL' : subscription.status}
            </span>
          </div>

          <div className="subscription-details">
            <div className="detail-item">
              <p className="detail-label">Plan</p>
              <p className="detail-value">{subscription.planName}</p>
            </div>

            {subscription.status === 'TRIALING' ? (
              <>
                <div className="detail-item">
                  <p className="detail-label">Trial Ends On</p>
                  <p className="detail-value">{formatDate(subscription.trialEndDate)}</p>
                </div>
                <div className="detail-item">
                  <p className="detail-label">First Payment</p>
                  <p className="detail-value">
                    {formatAmount(
                      subscription.totalDueMinor || 0,
                      subscription.currency
                    )} due on {formatDate(subscription.trialEndDate)}
                  </p>
                </div>
              </>
            ) : (
              <div className="detail-item">
                <p className="detail-label">Current Period</p>
                <p className="detail-value">
                  {formatDate(subscription.currentPeriodStart)} - {formatDate(subscription.currentPeriodEnd)}
                </p>
              </div>
            )}

            <div className="detail-item">
              <p className="detail-label">Add-ons</p>
              <p className="detail-value">{subscription.addOns?.length || 0} Active</p>
            </div>
          </div>

          {subscription.cancelAtPeriodEnd && (
            <div className="alert-warning">
              <AlertCircle size={20} />
              <p>Your subscription will be canceled at the end of the current billing period.</p>
            </div>
          )}
        </div>
      )}

      {/* No Subscription CTA */}
      {!subscription && (
        <div className="empty-state-card">
          <h3 className="empty-state-title">Start Your Subscription</h3>
          <p className="empty-state-subtitle">Choose a plan to get started with StreamFlix</p>
          <Link to="/plans" className="btn-primary-large">
            <Plus size={20} /> Subscribe Now
          </Link>
        </div>
      )}

      {/* Recent Invoices */}
      <div className="invoices-section">
        <div className="section-header">
          <h2 className="section-title">Recent Invoices</h2>
          <Link to="/dashboard/billing" className="view-all-link">
            View All
          </Link>
        </div>

        <div className="invoices-list">
          {invoices.length ? (
            invoices.map((invoice) => (
              <div key={invoice.invoiceId} className="invoice-item">
                <div className="invoice-info">
                  <p className="invoice-number">{invoice.invoiceNumber}</p>
                  <p className="invoice-date">{formatDate(invoice.issueDate)}</p>
                </div>
                <div className="invoice-meta">
                  <p className="invoice-amount">{formatAmount(invoice.totalMinor, invoice.currency)}</p>
                  <span
                    className="status-badge"
                    style={{
                      backgroundColor: `${getStatusColor(invoice.status)}20`,
                      color: getStatusColor(invoice.status)
                    }}
                  >
                    {invoice.status}
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="empty-list">
              <Receipt size={48} />
              <p>No recent invoices</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
