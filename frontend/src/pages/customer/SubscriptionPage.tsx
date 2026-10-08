import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as CustomerService from '../../services/customer/customerService';
import { useAuthContext } from '../../context/AuthContext';
import {
  Check,
  Pause,
  Play,
  Plus,
  Trash2,
  ArrowRight,
  AlertCircle,
  Calendar,
  Package,
  Zap,
  TrendingUp,
  X
} from 'lucide-react';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import './SubscriptionPage.css';

export const SubscriptionPage: React.FC = () => {
  const navigate = useNavigate();
  const { refreshUserStatus } = useAuthContext();
  const [subscription, setSubscription] = useState<CustomerService.Subscription | null>(null);
  const [availablePlans, setAvailablePlans] = useState<CustomerService.Plan[]>([]);
  const [availableAddOns, setAvailableAddOns] = useState<CustomerService.AddOn[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const [showPauseModal, setShowPauseModal] = useState(false);
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [refundInfo, setRefundInfo] = useState<CustomerService.CancellationResponse | null>(null);
  const [pauseDate, setPauseDate] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Cancellation Request State
  const [pendingCancelRequest, setPendingCancelRequest] = useState<CustomerService.CancellationRequest | null>(null);
  const [showCancelRequestModal, setShowCancelRequestModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelAtPeriodEnd, setCancelAtPeriodEnd] = useState(true);
  
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

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [sub, plans, addons, cancelReq] = await Promise.all([
        CustomerService.getCurrentSubscription(),
        CustomerService.getAvailablePlans(),
        CustomerService.getAvailableAddOns(),
        CustomerService.getPendingCancellationRequest()
      ]);
      setSubscription(sub);
      setAvailablePlans(plans);
      setAvailableAddOns(addons);
      setPendingCancelRequest(cancelReq);
    } catch (error) {
      console.error('Failed to load subscription data:', error);
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

  const handleUpgrade = async (planId: number) => {
    setActionLoading(true);
    try {
      await CustomerService.upgradeSubscription({ planId, proration: true });
      await loadData();
      setShowUpgradeModal(false);
    } catch (error) {
      console.error('handleUpgrade error:', error);
      showAlert('Error', 'Failed to change plan. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const initiatePlanChange = (plan: CustomerService.Plan) => {
    setShowUpgradeModal(false); // Close the underlying modal
    setConfirmDialog({
      isOpen: true,
      title: 'Confirm Plan Change',
      message: `Are you sure you want to change your subscription to the ${plan.name} plan?`,
      confirmLabel: 'Confirm Change',
      cancelLabel: 'Cancel',
      onConfirm: () => {
        closeConfirmDialog();
        handleUpgrade(plan.planId);
      }
    });
  };

  const handleCancel = (atPeriodEnd: boolean) => {
    setCancelAtPeriodEnd(atPeriodEnd);
    setCancelReason('');
    setShowCancelRequestModal(true);
  };

  const handleSubmitCancelRequest = async () => {
    if (!cancelReason.trim()) {
      showAlert('Validation Error', 'Please provide a reason for cancellation.');
      return;
    }
    setActionLoading(true);
    try {
      await CustomerService.submitCancellationRequest(cancelReason, cancelAtPeriodEnd);
      setShowCancelRequestModal(false);
      setConfirmDialog({
        isOpen: true,
        title: 'Request Submitted',
        message: 'Cancellation request submitted to support for approval.',
        confirmLabel: 'OK',
        cancelLabel: '',
        onConfirm: () => {
          closeConfirmDialog();
          loadData();
        }
      });
    } catch (error) {
      showAlert('Error', 'Failed to submit cancellation request.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleWithdrawCancellation = () => {
    setConfirmDialog({
      isOpen: true,
      title: 'Withdraw Cancellation Request',
      message: 'Are you sure you want to withdraw your pending cancellation request?',
      onConfirm: async () => {
        closeConfirmDialog();
        setActionLoading(true);
        try {
          await CustomerService.withdrawCancellationRequest();
          setConfirmDialog({
            isOpen: true,
            title: 'Request Withdrawn',
            message: 'Cancellation request successfully withdrawn.',
            confirmLabel: 'OK',
            cancelLabel: '',
            onConfirm: () => {
              closeConfirmDialog();
              loadData();
            }
          });
        } catch (error) {
          showAlert('Error', 'Failed to withdraw cancellation request.');
        } finally {
          setActionLoading(false);
        }
      }
    });
  };

  const handleRefundModalClose = async () => {
    setShowRefundModal(false);
    setRefundInfo(null);
    await refreshUserStatus();
    navigate('/');
  };

  const handlePause = async () => {
    if (!pauseDate) {
      showAlert('Validation Error', 'Please select a resume date');
      return;
    }
    setActionLoading(true);
    try {
      await CustomerService.pauseSubscription({ pausedTo: pauseDate });
      await loadData();
      setShowPauseModal(false);
    } catch (error) {
      showAlert('Error', 'Failed to pause subscription.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResume = async () => {
    setActionLoading(true);
    try {
      await CustomerService.resumeSubscription();
      await loadData();
    } catch (error) {
      showAlert('Error', 'Failed to resume subscription.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddAddOn = async (addonId: number) => {
    setActionLoading(true);
    try {
      await CustomerService.addAddOn(addonId);
      await loadData();
    } catch (error) {
      showAlert('Error', 'Failed to add add-on.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveAddOn = (addonId: number) => {
    setConfirmDialog({
      isOpen: true,
      title: 'Remove Add-on',
      message: 'Are you sure you want to remove this add-on from your subscription?',
      isDanger: true,
      confirmLabel: 'Remove Add-on',
      onConfirm: async () => {
        closeConfirmDialog();
        setActionLoading(true);
        try {
          await CustomerService.removeAddOn(addonId);
          await loadData();
        } catch (error) {
          showAlert('Error', 'Failed to remove add-on.');
        } finally {
          setActionLoading(false);
        }
      }
    });
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
      case 'trialing':
        return '#22c55e';
      case 'paused':
        return '#3b82f6';
      case 'past_due':
        return '#f59e0b';
      case 'canceled':
      case 'canceled':
        return '#5b4fff';
      default:
        return '#6b7280';
    }
  };

  if (loading) {
    return (
      <div className="subscription-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading subscription details...</p>
      </div>
    );
  }

  // No Active Subscription - Show Available Plans
  if (!subscription) {
    return (
      <div className="subscription-page">
        {/* Plans Grid */}

        <div className="plans-grid">
          {availablePlans.map((plan) => (
            <div key={plan.planId} className="plan-card">
              <div className="plan-header">
                <h3 className="plan-name">{plan.name}</h3>
                <span className="plan-badge">{plan.billingPeriod}</span>
              </div>

              <div className="plan-price">
                <span className="price-amount">{formatAmount(plan.defaultPriceMinor, plan.defaultCurrency)}</span>
                <span className="price-period">/{plan.billingPeriod.toLowerCase()}</span>
                <div style={{ fontSize: '13px', color: '#6B7280', marginTop: '4px', fontWeight: 500 }}>
                  {plan.taxMode === 'INCLUSIVE' ? '(incl. tax)' : '+ tax'}
                </div>
              </div>

              {plan.trialDays > 0 && (
                <div className="trial-badge">
                  <Zap size={16} />
                  <span>{plan.trialDays}-day free trial</span>
                </div>
              )}

              <ul className="plan-features">
                <li><Check size={16} className="feature-icon" /> All features included</li>
                <li><Check size={16} className="feature-icon" /> HD streaming</li>
                <li><Check size={16} className="feature-icon" /> Cancel anytime</li>
              </ul>

              <Link to={`/subscribe?planId=${plan.planId}`} className="btn-subscribe">
                Subscribe <ArrowRight size={16} />
              </Link>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="subscription-page">
      {/* Current Subscription Card */}

      {/* Current Subscription Card */}
      <div className="subscription-details-card">
        <div className="subscription-info-header">
          <div className="plan-info">
            <h2 className="plan-title">{subscription.planName}</h2>
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
          <div className="billing-info">
            <Calendar size={18} />
            <span>Next billing: {subscription.status === 'TRIALING' && subscription.trialEndDate
              ? new Date(subscription.trialEndDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
              : new Date(subscription.currentPeriodEnd).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
            }</span>
          </div>
        </div>

        {/* Subscription Meta */}
        <div className="subscription-meta">
          {subscription.status === 'TRIALING' ? (
            <>
              <div className="meta-item" style={{ gridColumn: 'span 3', backgroundColor: '#f3f4f6', padding: '12px', borderRadius: '8px' }}>
                <p className="meta-value" style={{ margin: 0, color: '#374151' }}>
                  <Zap size={16} style={{ display: 'inline', verticalAlign: 'text-bottom', marginRight: '6px', color: '#f59e0b' }} />
                  You're on a free trial until <strong>{new Date(subscription.trialEndDate!).toLocaleDateString()}</strong>.
                  Your first charge of <strong>{formatAmount(
                    subscription.totalDueMinor || 0,
                    subscription.currency
                  )}</strong> will occur on that date.
                </p>
              </div>
            </>
          ) : (
            <div className="meta-item">
              <p className="meta-label">Current Period</p>
              <p className="meta-value">
                {new Date(subscription.currentPeriodStart).toLocaleDateString()} - {new Date(subscription.currentPeriodEnd).toLocaleDateString()}
              </p>
            </div>
          )}
          <div className="meta-item">
            <p className="meta-label">Currency</p>
            <p className="meta-value">{subscription.currency}</p>
          </div>
          <div className="meta-item">
            <p className="meta-label">Account Credit</p>
            <p className="meta-value">{formatAmount(subscription.creditBalanceMinor || 0, subscription.currency)}</p>
          </div>
          {subscription.status !== 'TRIALING' && subscription.trialEndDate && (
            <div className="meta-item">
              <p className="meta-label">Trial Ended</p>
              <p className="meta-value">{new Date(subscription.trialEndDate).toLocaleDateString()}</p>
            </div>
          )}
        </div>

        {/* Cancel Warning - show above actions */}
        {subscription.cancelAtPeriodEnd && (
          <div className="alert-warning">
            <AlertCircle size={20} />
            <p>Your subscription will be canceled at the end of the current billing period.</p>
          </div>
        )}

        {pendingCancelRequest && (
          <div className="alert-warning" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertCircle size={20} />
              <div>
                <p style={{ margin: 0, fontWeight: 'bold' }}>Subscription Cancellation Pending Support Approval</p>
                <p style={{ margin: 0, fontSize: '13px', color: '#94a3b8' }}>
                  Requested {pendingCancelRequest.atPeriodEnd ? 'at Period End' : 'Immediately'} for reason: "{pendingCancelRequest.reason}"
                </p>
              </div>
            </div>
            <button
              onClick={handleWithdrawCancellation}
              className="btn-secondary"
              style={{ padding: '6px 12px', fontSize: '13px' }}
              disabled={actionLoading}
            >
              Withdraw Request
            </button>
          </div>
        )}

        {/* Action Buttons */}
        <div className="subscription-actions">
          {!subscription.cancelAtPeriodEnd && (
            <button
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setShowUpgradeModal(true);
              }}
              className="btn-primary"
              disabled={actionLoading || !!pendingCancelRequest}
            >
              <TrendingUp size={18} /> Change Plan
            </button>
          )}

          {subscription.status === 'ACTIVE' && (
            <button
              onClick={() => setShowPauseModal(true)}
              className="btn-secondary"
              disabled={actionLoading || !!pendingCancelRequest}
            >
              <Pause size={18} /> Pause
            </button>
          )}

          {subscription.status === 'PAUSED' && (
            <button
              onClick={handleResume}
              className="btn-success"
              disabled={actionLoading || !!pendingCancelRequest}
            >
              <Play size={18} /> Resume
            </button>
          )}

          {!subscription.cancelAtPeriodEnd && (
            <>
              <button
                onClick={() => handleCancel(true)}
                className="btn-danger-outline"
                disabled={actionLoading || !!pendingCancelRequest}
              >
                Cancel at Period End
              </button>
              <button
                onClick={() => handleCancel(false)}
                className="btn-danger"
                disabled={actionLoading || !!pendingCancelRequest}
                style={{ marginLeft: '12px' }}
              >
                Cancel Immediately
              </button>
            </>
          )}
        </div>
      </div>

      {/* Active Add-ons */}
      <div className="addons-section">
        <div className="section-header">
          <h3 className="section-title">Active Add-ons</h3>
          <Package size={20} className="section-icon" />
        </div>
        {subscription.addOns?.length ? (
          <div className="addons-list">
            {subscription.addOns.map((addon) => (
              <div key={addon.itemId} className="addon-item">
                <div className="addon-info">
                  <p className="addon-name">{addon.addonName}</p>
                  <p className="addon-price">
                    {formatAmount(addon.unitPriceMinor, subscription.currency)} × {addon.quantity}
                  </p>
                </div>
                <button
                  onClick={() => handleRemoveAddOn(addon.addonId)}
                  className="btn-icon-danger"
                  disabled={actionLoading || !!pendingCancelRequest}
                  title="Remove add-on"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <Package size={40} className="empty-icon" />
            <p className="empty-text">No active add-ons</p>
            <p className="empty-hint">Add-ons enhance your subscription with extra features</p>
          </div>
        )}
      </div>

      {/* Available Add-ons */}
      {availableAddOns.filter(a => a.status === 'ACTIVE' && !subscription.addOns?.some(subAddOn => subAddOn.addonId === a.addOnId)).length > 0 && (
        <div className="addons-section">
          <div className="section-header">
            <h3 className="section-title">Available Add-ons</h3>
            <Plus size={20} className="section-icon" />
          </div>
          <div className="addons-grid">
            {availableAddOns
              .filter(a => a.status === 'ACTIVE' && !subscription.addOns?.some(subAddOn => subAddOn.addonId === a.addOnId))
              .map((addon) => (
                <div key={addon.addOnId} className="addon-card">
                  <div className="addon-details">
                    <p className="addon-name">{addon.name}</p>
                    <p className="addon-price">
                      {formatAmount(addon.priceMinor, addon.currency)}/{addon.billingPeriod.toLowerCase()}
                    </p>
                  </div>
                  <button
                    onClick={() => handleAddAddOn(addon.addOnId)}
                    className="btn-icon-add"
                    disabled={actionLoading || !!pendingCancelRequest}
                    title="Add add-on"
                  >
                    <Plus size={20} />
                  </button>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* Metered Usage Summary */}
      {subscription.meteredUsage?.length > 0 && (
        <div className="usage-section">
          <div className="section-header">
            <h3 className="section-title">Metered Usage</h3>
            <TrendingUp size={20} className="section-icon" />
          </div>
          <div className="usage-list">
            {subscription.meteredUsage.map((usage) => (
              <div key={usage.componentId} className="usage-item">
                <div className="usage-info">
                  <p className="usage-name">{usage.componentName}</p>
                  <p className="usage-details">
                    {usage.quantityUsed} {usage.unitName} used
                    {usage.freeTierQuantity > 0 && (
                      <span className="free-tier"> ({usage.freeTierQuantity} free included)</span>
                    )}
                  </p>
                </div>
                <p className="usage-cost">
                  {formatAmount(usage.costMinor, subscription.currency)}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Upgrade Modal */}
      {showUpgradeModal && (
        <div className="modal-overlay" onClick={() => setShowUpgradeModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Change Plan</h3>
              <button 
                onClick={() => setShowUpgradeModal(false)} 
                className="modal-close"
              >
                <X size={20} />
              </button>
            </div>
            <div className="plan-options">
              {availablePlans.map((plan) => (
                <div
                  key={plan.planId}
                  className={`plan-option ${plan.planId === subscription.planId ? 'current' : ''}`}
                >
                  <div>
                    <div className="plan-option-header">
                      <h4 className="plan-option-name">{plan.name}</h4>
                      {plan.planId === subscription.planId && (
                        <span className="current-badge">Current</span>
                      )}
                    </div>
                    <p className="plan-option-price">
                      {formatAmount(plan.defaultPriceMinor, plan.defaultCurrency)}
                      <span className="period">/{plan.billingPeriod.toLowerCase()}</span>
                      <span style={{ fontSize: '12px', color: '#6B7280', marginLeft: '6px' }}>
                        {plan.taxMode === 'INCLUSIVE' ? '(incl. tax)' : '+ tax'}
                      </span>
                    </p>
                  </div>
                  {plan.planId !== subscription.planId && (
                    <button 
                      className="btn-primary" 
                      onClick={() => initiatePlanChange(plan)}
                      disabled={actionLoading}
                    >
                      {actionLoading ? 'Updating...' : 'Select Plan'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Pause Modal */}
      {showPauseModal && (
        <div className="modal-overlay" onClick={() => setShowPauseModal(false)}>
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Pause Subscription</h3>
              <button 
                onClick={() => setShowPauseModal(false)} 
                className="modal-close"
              >
                <X size={20} />
              </button>
            </div>
            <p className="modal-description" style={{ color: '#64748b', marginBottom: '1.25rem' }}>
              Select resume date (up to 30 days):
            </p>
            <input
              type="date"
              value={pauseDate}
              onChange={(e) => setPauseDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              max={new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}
              className="date-input"
            />
            <div className="modal-actions">
              <button onClick={handlePause} className="btn-warning" disabled={actionLoading}>
                {actionLoading ? 'Pausing...' : 'Pause Subscription'}
              </button>
              <button onClick={() => setShowPauseModal(false)} className="btn-secondary">
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Refund Confirmation Modal */}
      {showRefundModal && refundInfo && (
        <div className="modal-overlay">
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: '#22c55e' }}>Refund Processed</h3>
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
                Your subscription has been canceled.
              </p>
              <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '1.5rem' }}>
                A refund has been processed to your original payment method.
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
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <span style={{ color: '#64748b', fontSize: '13px' }}>Transaction ID</span>
                  <span style={{ color: '#334155', fontSize: '13px', fontFamily: 'monospace' }}>
                    {refundInfo.refundGatewayRef}
                  </span>
                </div>
                {refundInfo.creditNoteNumber && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#64748b', fontSize: '13px' }}>Credit Note</span>
                    <span style={{ color: '#334155', fontSize: '13px', fontFamily: 'monospace' }}>
                      {refundInfo.creditNoteNumber}
                    </span>
                  </div>
                )}
              </div>
              <p style={{ color: '#64748b', fontSize: '12px', marginTop: '1rem' }}>
                Please allow 5-7 business days for the refund to reflect in your account.
              </p>
            </div>
            <div className="modal-actions" style={{ justifyContent: 'center' }}>
              <button onClick={handleRefundModalClose} className="btn-primary">
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancellation Request Modal */}
      {showCancelRequestModal && (
        <div className="modal-overlay" onClick={() => setShowCancelRequestModal(false)}>
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Request Subscription Cancellation</h3>
              <button 
                onClick={() => setShowCancelRequestModal(false)} 
                className="modal-close"
              >
                <X size={20} />
              </button>
            </div>
            <p className="modal-description" style={{ color: '#64748b', marginBottom: '1.25rem', fontSize: '14px' }}>
              Please let us know why you would like to cancel your subscription. Your request will be sent to our support team for review.
            </p>
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', color: '#475569', marginBottom: '8px', fontSize: '13px', fontWeight: 500 }}>
                Reason for cancellation
              </label>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="E.g., Too expensive, no longer needed, moving to another service..."
                className="date-input"
                style={{
                  width: '100%',
                  height: '100px',
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
              <button onClick={() => setShowCancelRequestModal(false)} className="btn-secondary">
                Cancel
              </button>
              <button onClick={handleSubmitCancelRequest} className="btn-danger" disabled={actionLoading}>
                {actionLoading ? 'Submitting...' : 'Submit Request'}
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
    </div>
  );
};
