import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  Check,
  Shield,
  Lock,
  CreditCard,
  Rocket,
  HelpCircle
} from 'lucide-react';
import * as CustomerService from '../../services/customer/customerService';
import './SubscriptionCheckoutPage.css';

export const SubscriptionCheckoutPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const planId = searchParams.get('planId');
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [plan, setPlan] = useState<CustomerService.Plan | null>(null);
  const [addons, setAddons] = useState<CustomerService.AddOn[]>([]);
  const [selectedAddons] = useState<number[]>([]);

  // Form state
  const [cardholderName, setCardholderName] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [expiryMonth, setExpiryMonth] = useState('');
  const [expiryYear, setExpiryYear] = useState('');
  const [cvv, setCvv] = useState('');
  const [couponCode, setCouponCode] = useState('');
  const [couponApplied, setCouponApplied] = useState(false);
  const [discount, setDiscount] = useState(0);
  const [processing, setProcessing] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadPlanAndAddons = useCallback(async () => {
    try {
      setLoading(true);
      const [planData, addonsData] = await Promise.all([
        planId ? CustomerService.getAvailablePlans().then(plans => plans.find(p => p.planId === parseInt(planId)) || null) : null,
        CustomerService.getAvailableAddOns()
      ]);
      setPlan(planData);
      setAddons(addonsData);
    } catch (error) {
      console.error('Failed to load plan:', error);
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    loadPlanAndAddons();
  }, [loadPlanAndAddons]);

  const formatPrice = (amount: number): string => {
    const currency = plan?.defaultCurrency || 'INR';
    const locale = currency === 'USD' ? 'en-US' : currency === 'GBP' ? 'en-GB' : 'en-IN';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency,
    }).format(amount / 100);
  };

  const calculateTotal = () => {
    if (!plan) return 0;
    let total = plan.defaultPriceMinor;
    selectedAddons.forEach(addonId => {
      const addon = addons.find(a => a.addOnId === addonId);
      if (addon) total += addon.priceMinor;
    });
    total -= discount;
    if (plan.taxMode === 'EXCLUSIVE') {
      const taxRate = 0.18; // 18% tax
      total += Math.round(total * taxRate);
    }
    return total;
  };

  const handleApplyCoupon = () => {
    if (couponCode.toLowerCase() === 'streamflix10') {
      setDiscount(plan ? plan.defaultPriceMinor * 0.1 : 0);
      setCouponApplied(true);
    } else {
      alert('Invalid coupon code');
    }
  };

  const handleFieldChange = (field: string, value: string) => {
    setFieldErrors(prev => ({ ...prev, [field]: '' }));
    switch (field) {
      case 'cardholderName': setCardholderName(value); break;
      case 'cardNumber': setCardNumber(value); break;
      case 'expiryMonth': setExpiryMonth(value); break;
      case 'expiryYear': setExpiryYear(value); break;
      case 'cvv': setCvv(value); break;
      case 'couponCode': setCouponCode(value); break;
    }
  };

  const handleSubscribe = async () => {
    const errors: Record<string, string> = {};
    if (!cardholderName) errors.cardholderName = 'Cardholder name is required';
    else if (!/^[a-zA-Z\s]+$/.test(cardholderName)) errors.cardholderName = 'Cardholder name must contain only letters and spaces';
    if (!cardNumber) errors.cardNumber = 'Card number is required';
    if (!expiryMonth) errors.expiryMonth = 'Expiry month is required';
    if (!expiryYear) errors.expiryYear = 'Expiry year is required';
    if (!cvv) errors.cvv = 'CVV is required';

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setProcessing(true);
    try {
      // Create payment method
      const paymentMethod = await CustomerService.addPaymentMethod({
        paymentType: 'CARD',
        cardNumber,
        expiryMonth: parseInt(expiryMonth),
        expiryYear: parseInt(expiryYear),
        cvv,
        isDefault: true
      });

      // Complete subscription
      if (plan && paymentMethod) {
        await CustomerService.createSubscription({
          planId: plan.planId,
          paymentMethodId: paymentMethod.paymentMethodId
        });
        navigate('/dashboard');
      }
    } catch (error: any) {
      alert(error.message || 'Failed to complete subscription');
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <div className="checkout-loading">
        <div className="spinner"></div>
        <p>Loading checkout...</p>
      </div>
    );
  }

  if (!plan) {
    return (
      <div className="checkout-error">
        <p>Plan not found</p>
        <Link to="/plans">Back to Plans</Link>
      </div>
    );
  }

  return (
    <div className="checkout-page">
      {/* Header */}
      <header className="checkout-header">
        <div className="header-left">
          <h1 className="brand-logo">Streamflix</h1>
        </div>
        <div className="header-right">
          <Link to="/help" className="help-link">
            <HelpCircle size={20} />
            Need help?
          </Link>
        </div>
      </header>

      {/* Stepper */}
      <div className="stepper-container">
        <div className="stepper">
          <div className="step completed">
            <div className="step-icon"><Check size={16} /></div>
            <span className="step-label">Choose Plan</span>
          </div>
          <div className="step-line completed"></div>
          <div className="step completed">
            <div className="step-icon"><Check size={16} /></div>
            <span className="step-label">Add-ons</span>
          </div>
          <div className="step-line"></div>
          <div className="step active">
            <div className="step-icon">3</div>
            <span className="step-label">Payment</span>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="checkout-content">
        {/* Left - Payment Form */}
        <div className="payment-form-section">
          <div className="section-header">
            <h2 className="section-title">Payment Details</h2>
            <p className="section-subtitle">Enter your card information securely</p>
          </div>

          <div className="form-container">
            <div className="form-group">
              <label className="form-label">Cardholder Name</label>
              <input
                type="text"
                className="form-input"
                placeholder="John Doe"
                value={cardholderName}
                onChange={(e) => handleFieldChange('cardholderName', e.target.value)}
              />
              {fieldErrors['cardholderName'] && (
                <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                  {fieldErrors['cardholderName']}
                </span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Card Number</label>
              <div className="input-with-icon">
                <CreditCard size={20} className="input-icon" />
                <input
                  type="text"
                  className="form-input"
                  placeholder="1234 5678 9012 3456"
                  value={cardNumber.replace(/\D/g, '').replace(/(\d{4})/g, '$1 ').trim()}
                  onChange={(e) => handleFieldChange('cardNumber', e.target.value.replace(/\D/g, '').slice(0, 16))}
                  maxLength={19}
                />
              </div>
              {fieldErrors['cardNumber'] && (
                <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                  {fieldErrors['cardNumber']}
                </span>
              )}
            </div>

            <div className="form-row">
              <div className="form-group half">
                <label className="form-label">Expiry Month</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="MM"
                  value={expiryMonth}
                  onChange={(e) => handleFieldChange('expiryMonth', e.target.value.replace(/\D/g, '').slice(0, 2))}
                  maxLength={2}
                />
                {fieldErrors['expiryMonth'] && (
                  <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                    {fieldErrors['expiryMonth']}
                  </span>
                )}
              </div>
              <div className="form-group half">
                <label className="form-label">Expiry Year</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="YYYY"
                  value={expiryYear}
                  onChange={(e) => handleFieldChange('expiryYear', e.target.value.replace(/\D/g, '').slice(0, 4))}
                  maxLength={4}
                />
                {fieldErrors['expiryYear'] && (
                  <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                    {fieldErrors['expiryYear']}
                  </span>
                )}
              </div>
              <div className="form-group half">
                <label className="form-label">CVV</label>
                <input
                  type="password"
                  className="form-input"
                  placeholder="123"
                  value={cvv}
                  onChange={(e) => handleFieldChange('cvv', e.target.value.replace(/\D/g, '').slice(0, 4))}
                  maxLength={4}
                />
                {fieldErrors['cvv'] && (
                  <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                    {fieldErrors['cvv']}
                  </span>
                )}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Coupon Code</label>
              <div className="coupon-input-group">
                <input
                  type="text"
                  className="form-input"
                  placeholder="Enter coupon code"
                  value={couponCode}
                  onChange={(e) => handleFieldChange('couponCode', e.target.value)}
                  disabled={couponApplied}
                />
                <button
                  className="apply-coupon-btn"
                  onClick={handleApplyCoupon}
                  disabled={couponApplied}
                >
                  {couponApplied ? 'Applied' : 'Apply'}
                </button>
              </div>
              {fieldErrors['couponCode'] && (
                <span style={{ color: '#dc2626', fontSize: '12px', marginTop: '4px', display: 'block' }}>
                  {fieldErrors['couponCode']}
                </span>
              )}
              {couponApplied && (
                <p className="coupon-success">Coupon applied successfully!</p>
              )}
            </div>

            <div className="security-note">
              <Shield size={16} />
              <span>Your payment information is securely encrypted with SSL</span>
            </div>
          </div>
        </div>

        {/* Right - Order Summary */}
        <div className="order-summary-section">
          <div className="summary-card">
            <h3 className="summary-title">Order Summary</h3>

            <div className="summary-breakdown">
              <div className="summary-item">
                <span className="item-label">{plan.name}</span>
                <span className="item-value">{formatPrice(plan.defaultPriceMinor)}/{plan.billingPeriod.toLowerCase()}</span>
              </div>

              {selectedAddons.map(addonId => {
                const addon = addons.find(a => a.addOnId === addonId);
                return addon ? (
                  <div key={addonId} className="summary-item">
                    <span className="item-label">{addon.name}</span>
                    <span className="item-value">+{formatPrice(addon.priceMinor)}</span>
                  </div>
                ) : null;
              })}

              {discount > 0 && (
                <div className="summary-item discount">
                  <span className="item-label">Coupon Applied</span>
                  <span className="item-value">-{formatPrice(discount)}</span>
                </div>
              )}

              <div className="summary-divider"></div>

              <div className="summary-total">
                <span className="total-label">{plan.trialDays > 0 ? 'Due Today' : 'Total Due'}</span>
                <span className="total-value">
                  {plan.trialDays > 0 
                    ? formatPrice(0) 
                    : `${formatPrice(calculateTotal())} / ${plan.billingPeriod === 'YEARLY' ? 'year' : 'month'}`}
                </span>
              </div>
              {plan.trialDays > 0 && (
                <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '8px', marginBottom: 0 }}>
                  After your {plan.trialDays}-day trial, you'll be charged <strong>{formatPrice(calculateTotal())}</strong> per {plan.billingPeriod === 'YEARLY' ? 'year' : 'month'} {plan.taxMode === 'INCLUSIVE' ? '(incl. tax)' : '+ tax'}
                </p>
              )}
            </div>

            <div className="secure-checkout-card">
              <Lock size={20} />
              <div>
                <p className="secure-title">Secure Checkout</p>
                <p className="secure-subtitle">256-bit SSL encryption</p>
              </div>
            </div>
          </div>

          {/* Selected Configuration */}
          <div className="selected-config">
            <h4 className="config-title">Selected Configuration</h4>
            
            <div className="config-card selected">
              <div className="config-badge">CURRENT SELECTION</div>
              <h5 className="config-name">{plan.name}</h5>
              <ul className="config-features">
                {plan.features?.map((feature, idx) => (
                  <li key={idx}>{feature}</li>
                ))}
              </ul>
            </div>

            {selectedAddons.map(addonId => {
              const addon = addons.find(a => a.addOnId === addonId);
              return addon ? (
                <div key={addonId} className="config-card">
                  <h5 className="config-name">{addon.name}</h5>
                  <p className="config-price">{formatPrice(addon.priceMinor)}/{addon.billingPeriod.toLowerCase()}</p>
                </div>
              ) : null;
            })}
          </div>
        </div>
      </div>

      {/* Bottom Actions */}
      <div className="checkout-actions">
        <Link to="/subscribe?planId={planId}&step=2" className="back-btn">
          <ArrowLeft size={20} />
          Back to Add-ons
        </Link>
        <button
          className="subscribe-btn"
          onClick={handleSubscribe}
          disabled={processing}
        >
          <Rocket size={20} />
          {processing ? 'Processing...' : 'Start Subscription'}
        </button>
      </div>

      {/* Footer */}
      <footer className="checkout-footer">
        <div className="footer-links">
          <Link to="/terms">Terms of Service</Link>
          <Link to="/privacy">Privacy Policy</Link>
        </div>
        <div className="footer-security">
          <Lock size={16} />
          <Shield size={16} />
        </div>
      </footer>
    </div>
  );
};

export default SubscriptionCheckoutPage;
