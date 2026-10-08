import React, { useState, useEffect } from 'react';
import { Loader2, CheckCircle, Shield, Tag, X, Ticket } from 'lucide-react';
import * as CustomerService from '../../../services/customer/customerService';

interface MockPaymentStepProps {
  plan: CustomerService.Plan;
  customerId: number;
  paymentMethodRequest: CustomerService.PaymentMethodRequest;
  onComplete: (data: any) => void;
}

export const MockPaymentStep: React.FC<MockPaymentStepProps> = ({ 
  plan, 
  customerId, 
  paymentMethodRequest, 
  onComplete 
}) => {
  const getTaxRate = (): number => {
    if (plan.defaultCurrency === 'USD') return 7.25;
    if (plan.defaultCurrency === 'GBP') return 20;
    return 18; // Default to India GST
  };
  
  const TAX_RATE_PERCENT = getTaxRate();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);
  
  // Coupon state
  const [couponCode, setCouponCode] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState<CustomerService.Coupon | null>(null);
  const [couponError, setCouponError] = useState('');
  const [couponLoading, setCouponLoading] = useState(false);
  const [availableCoupons, setAvailableCoupons] = useState<CustomerService.Coupon[]>([]);

  useEffect(() => {
    // Fetch available coupons
    CustomerService.getAvailableCoupons()
      .then(setAvailableCoupons)
      .catch(() => {}); // Silent fail
  }, []);

  const formatPrice = (amount: number, currency: string): string => {
    const locale = currency === 'USD' ? 'en-US' : currency === 'GBP' ? 'en-GB' : 'en-IN';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount / 100);
  };

  // Calculate discount
  const calculateDiscount = (): number => {
    if (!appliedCoupon) return 0;
    if (appliedCoupon.type === 'PERCENT') {
      return Math.round(plan.defaultPriceMinor * appliedCoupon.amount / 100);
    } else {
      // FIXED type — amount is in minor units
      return Math.min(appliedCoupon.amount, plan.defaultPriceMinor);
    }
  };

  const discountMinor = calculateDiscount();
  const priceAfterDiscount = plan.defaultPriceMinor - discountMinor;
  let taxMinor = 0;
  let totalMinor = priceAfterDiscount;
  
  if (plan.taxMode === 'INCLUSIVE') {
    const baseMinor = Math.round(priceAfterDiscount / (1 + TAX_RATE_PERCENT / 100));
    taxMinor = priceAfterDiscount - baseMinor;
    totalMinor = priceAfterDiscount;
  } else {
    taxMinor = Math.round(priceAfterDiscount * TAX_RATE_PERCENT / 100);
    totalMinor = priceAfterDiscount + taxMinor;
  }
  const isTrial = plan.trialDays > 0;

  const handleApplyCoupon = async (code?: string) => {
    const codeToApply = code || couponCode.trim();
    if (!codeToApply) return;
    
    setCouponError('');
    setCouponLoading(true);
    try {
      const coupon = await CustomerService.validateCoupon(codeToApply);
      setAppliedCoupon(coupon);
      setCouponCode(coupon.code);
    } catch (err: any) {
      setCouponError(err.message || 'Invalid coupon code');
      setAppliedCoupon(null);
    } finally {
      setCouponLoading(false);
    }
  };

  const handleRemoveCoupon = () => {
    setAppliedCoupon(null);
    setCouponCode('');
    setCouponError('');
  };

  const handlePay = async () => {
    setError('');
    setLoading(true);

    try {
      const result = await CustomerService.completeSubscription(customerId, {
        planId: plan.planId,
        paymentMethod: paymentMethodRequest,
        billingPeriod: plan.billingPeriod,
        couponCode: appliedCoupon?.code,
      });

      setShowSuccess(true);
      setTimeout(() => {
        onComplete({ subscriptionId: result.subscriptionId });
      }, 2000);
    } catch (err: any) {
      setError(err.message || 'Payment failed');
    } finally {
      setLoading(false);
    }
  };

  if (showSuccess) {
    return (
      <div className="card border-0" style={{ borderRadius: '24px', background: '#FFFFFF', border: '1px solid #E5E7EB', boxShadow: '0 12px 32px -8px rgba(0,0,0,0.05)' }}>
        <div className="card-body p-4 p-md-5 text-center">
          <div 
            className="d-inline-flex align-items-center justify-content-center rounded-circle mb-4"
            style={{ 
              width: '80px', 
              height: '80px', 
              background: '#ECFDF5'
            }}
          >
            <CheckCircle size={40} color="#059669" />
          </div>
          <h2 style={{ 
            fontFamily: 'Outfit, sans-serif', 
            fontSize: '32px', 
            fontWeight: 700, 
            color: '#111827',
            marginBottom: '12px',
            letterSpacing: '-1px'
          }}>
            {isTrial ? 'Trial Started!' : 'Payment Successful!'}
          </h2>
          <p style={{ color: '#6B7280', fontSize: '16px', lineHeight: '1.6' }}>
            {isTrial 
              ? `Your ${plan.trialDays}-day free trial is now active. Your first charge of ${formatPrice(totalMinor, plan.defaultCurrency)} will occur on ${new Date(Date.now() + plan.trialDays * 24 * 60 * 60 * 1000).toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' })}.` 
              : 'Your subscription is now active. Redirecting you home...'}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="card border-0" style={{ borderRadius: '24px', background: '#FFFFFF', border: '1px solid #E5E7EB', boxShadow: '0 12px 32px -8px rgba(0,0,0,0.05)' }}>
      <div className="card-body p-4 p-md-5">
        <h2 style={{ 
          fontFamily: 'Outfit, sans-serif', 
          fontSize: '28px', 
          fontWeight: 700, 
          color: '#111827',
          marginBottom: '8px',
          letterSpacing: '-0.5px'
        }}>
          Confirm & Pay
        </h2>
        <p style={{ color: '#6B7280', fontSize: '15px', marginBottom: '32px' }}>
          Review your order and complete the payment.
        </p>

        {error && (
          <div className="alert alert-danger border-0 mb-4" style={{ borderRadius: '12px', background: '#FEF2F2', color: '#DC2626', fontSize: '14px' }}>
            {error}
          </div>
        )}

        {/* Order Summary */}
        <div 
          className="p-4 mb-4" 
          style={{ background: '#F9FAFB', borderRadius: '16px', border: '1px solid #E5E7EB' }}
        >
          <h4 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '14px', fontWeight: 700, color: '#111827', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Order Summary
          </h4>
          
          {/* Plan Price */}
          <div className="d-flex justify-content-between align-items-start mb-3">
            <div>
              <p style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>
                {plan.name}
              </p>
              <p style={{ fontSize: '14px', color: '#6B7280', margin: 0 }}>
                {plan.billingPeriod === 'YEARLY' ? 'Annual Subscription' : 'Monthly Subscription'}
              </p>
            </div>
            <p style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>
              {formatPrice(plan.defaultPriceMinor, plan.defaultCurrency)}
            </p>
          </div>

          {/* Coupon Discount Line */}
          {appliedCoupon && discountMinor > 0 && (
            <div className="d-flex justify-content-between align-items-center mb-3">
              <div className="d-flex align-items-center gap-2">
                <Tag size={14} color="#059669" />
                <span style={{ fontSize: '14px', fontWeight: 500, color: '#059669' }}>
                  {appliedCoupon.code} — {appliedCoupon.type === 'PERCENT' ? `${appliedCoupon.amount}% off` : `${formatPrice(appliedCoupon.amount, plan.defaultCurrency)} off`}
                </span>
              </div>
              <span style={{ fontSize: '14px', fontWeight: 600, color: '#059669' }}>
                -{formatPrice(discountMinor, plan.defaultCurrency)}
              </span>
            </div>
          )}

          {/* Tax */}
          <div className="d-flex justify-content-between align-items-center mb-3">
            <span style={{ fontSize: '14px', color: '#6B7280' }}>
              {plan.taxMode === 'INCLUSIVE' ? `Includes GST (${TAX_RATE_PERCENT}%)` : `GST (${TAX_RATE_PERCENT}%)`}
            </span>
            <span style={{ fontSize: '14px', color: '#6B7280' }}>
              {plan.taxMode === 'INCLUSIVE' ? formatPrice(taxMinor, plan.defaultCurrency) : `+${formatPrice(taxMinor, plan.defaultCurrency)}`}
            </span>
          </div>

          {/* Trial Banner */}
          {isTrial && (
            <div 
              className="p-3 mb-3" 
              style={{ background: '#ECFDF5', borderRadius: '12px', border: '1px solid #A7F3D0' }}
            >
              <p style={{ fontSize: '13px', color: '#065F46', margin: 0, fontWeight: 600 }}>
                🎉 {plan.trialDays}-day free trial included!
              </p>
              <p style={{ fontSize: '12px', color: '#047857', margin: '2px 0 0 0' }}>
                You won't be charged until your trial ends.
              </p>
            </div>
          )}

          {/* Total */}
          <div 
            className="pt-3 mt-3"
            style={{ borderTop: '1px dashed #D1D5DB' }}
          >
            <div className="d-flex justify-content-between align-items-center">
              <p style={{ fontSize: '16px', fontWeight: 600, color: '#111827', margin: 0 }}>
                {isTrial ? 'Due today' : 'Total'}
              </p>
              <p style={{ fontSize: '24px', fontWeight: 700, color: '#5B4FFF', margin: 0 }}>
                {isTrial ? 'FREE' : formatPrice(totalMinor, plan.defaultCurrency)}
              </p>
            </div>
            {isTrial && (
              <p style={{ fontSize: '13px', color: '#6B7280', marginTop: '8px', marginBottom: 0 }}>
                After trial ends, you'll be charged <strong style={{ color: '#111827' }}>{formatPrice(totalMinor, plan.defaultCurrency)}</strong> per {plan.billingPeriod === 'YEARLY' ? 'year' : 'month'} {plan.taxMode === 'INCLUSIVE' ? '(incl. tax)' : '+ tax'}
              </p>
            )}
          </div>
        </div>

        {/* Coupon Code Section */}
        <div className="mb-4">
          <label style={{ fontSize: '14px', fontWeight: 600, color: '#374151', marginBottom: '8px', display: 'block' }}>
            <Ticket size={16} style={{ display: 'inline', marginRight: '6px', verticalAlign: 'text-bottom' }} />
            Have a coupon code?
          </label>
          
          {appliedCoupon ? (
            <div 
              className="d-flex align-items-center justify-content-between p-3"
              style={{ background: '#ECFDF5', borderRadius: '12px', border: '1px solid #A7F3D0' }}
            >
              <div className="d-flex align-items-center gap-2">
                <Tag size={16} color="#059669" />
                <div>
                  <span style={{ fontSize: '14px', fontWeight: 600, color: '#065F46' }}>
                    {appliedCoupon.code}
                  </span>
                  <span style={{ fontSize: '13px', color: '#047857', marginLeft: '8px' }}>
                    — You save {formatPrice(discountMinor, plan.defaultCurrency)}!
                  </span>
                </div>
              </div>
              <button
                onClick={handleRemoveCoupon}
                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px' }}
              >
                <X size={18} color="#6B7280" />
              </button>
            </div>
          ) : (
            <>
              <div className="d-flex gap-2">
                <input
                  type="text"
                  className="form-control"
                  placeholder="Enter coupon code"
                  value={couponCode}
                  onChange={(e) => { setCouponCode(e.target.value.toUpperCase()); setCouponError(''); }}
                  style={{ 
                    borderRadius: '12px', 
                    border: '1px solid #E5E7EB', 
                    padding: '10px 16px',
                    fontSize: '14px',
                    flex: 1
                  }}
                />
                <button
                  onClick={() => handleApplyCoupon()}
                  disabled={couponLoading || !couponCode.trim()}
                  className="btn"
                  style={{
                    background: '#5B4FFF',
                    color: 'white',
                    border: 'none',
                    borderRadius: '12px',
                    padding: '10px 20px',
                    fontSize: '14px',
                    fontWeight: 600,
                    opacity: couponLoading || !couponCode.trim() ? 0.6 : 1,
                    whiteSpace: 'nowrap'
                  }}
                >
                  {couponLoading ? 'Checking...' : 'Apply'}
                </button>
              </div>
              {couponError && (
                <p style={{ fontSize: '13px', color: '#DC2626', marginTop: '6px', marginBottom: 0 }}>
                  {couponError}
                </p>
              )}
              
              {/* Available Coupons */}
              {availableCoupons.length > 0 && (
                <div style={{ marginTop: '12px' }}>
                  <p style={{ fontSize: '12px', color: '#9CA3AF', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
                    Available Coupons
                  </p>
                  <div className="d-flex flex-wrap gap-2">
                    {availableCoupons.map(c => (
                      <button
                        key={c.couponId}
                        onClick={() => { setCouponCode(c.code); handleApplyCoupon(c.code); }}
                        className="d-flex align-items-center gap-1"
                        style={{
                          background: '#F3F4F6',
                          border: '1px dashed #D1D5DB',
                          borderRadius: '8px',
                          padding: '6px 12px',
                          cursor: 'pointer',
                          fontSize: '13px',
                          fontWeight: 500,
                          color: '#374151',
                          transition: 'all 0.2s'
                        }}
                        onMouseEnter={(e) => { e.currentTarget.style.background = '#EEF2FF'; e.currentTarget.style.borderColor = '#818CF8'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.background = '#F3F4F6'; e.currentTarget.style.borderColor = '#D1D5DB'; }}
                      >
                        <Tag size={12} />
                        <span>{c.code}</span>
                        <span style={{ color: '#6B7280', fontSize: '12px' }}>
                          ({c.type === 'PERCENT' ? `${c.amount}% off` : `${formatPrice(c.amount, plan.defaultCurrency)} off`})
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Security Badge */}
        <div className="d-flex align-items-center justify-content-center gap-2 mb-4 p-2" style={{ background: '#F0FDF4', borderRadius: '8px' }}>
          <Shield size={16} color="#059669" />
          <span style={{ fontSize: '13px', color: '#065F46', fontWeight: 500 }}>
            Secure, encrypted payment processing
          </span>
        </div>

        {/* Pay Button */}
        <button
          onClick={handlePay}
          disabled={loading}
          className="btn w-100 d-flex align-items-center justify-content-center gap-2"
          style={{
            background: '#5B4FFF',
            color: 'white',
            border: 'none',
            padding: '18px 24px',
            borderRadius: '9999px',
            fontSize: '16px',
            fontWeight: 600,
            transition: 'all 0.2s',
            opacity: loading ? 0.7 : 1,
            boxShadow: '0 4px 12px rgba(91, 79, 255, 0.2)'
          }}
        >
          {loading ? (
            <>
              <Loader2 size={24} className="spin" />
              Processing...
            </>
          ) : (
            <>
              {isTrial ? 'Start Free Trial' : `Pay ${formatPrice(totalMinor, plan.defaultCurrency)}`}
            </>
          )}
        </button>

        {/* Terms */}
        <p className="text-center mt-4 mb-0" style={{ fontSize: '12px', color: '#9CA3AF', lineHeight: '1.5' }}>
          By clicking above, you agree to our <a href="#" style={{ color: '#5B4FFF', textDecoration: 'none' }}>Terms of Service</a> and authorize recurring charges.
        </p>
      </div>
    </div>
  );
};
