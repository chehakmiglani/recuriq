import React, { useState } from 'react';
import { Loader2, CreditCard, Smartphone, Check } from 'lucide-react';
import * as CustomerService from '../../../services/customer/customerService';
import { validatePaymentMethod, validateExpiryDate } from '../../../utils/subscriptionValidation';

interface PaymentMethodStepProps {
  plan: CustomerService.Plan;
  customerId: number;
  onComplete: (data: { paymentMethodRequest: CustomerService.PaymentMethodRequest }) => void;
}

export const PaymentMethodStep: React.FC<PaymentMethodStepProps> = ({ plan: _plan, onComplete }) => {
  const [paymentType, setPaymentType] = useState<'CARD' | 'UPI'>('CARD');
  const [cardData, setCardData] = useState({
    cardNumber: '',
    cardholderName: '',
    expiryMonth: '',
    expiryYear: '',
    cvv: '',
  });
  const [upiId, setUpiId] = useState('');
  const [loading, setLoading] = useState(false);
  const [backendError, setBackendError] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const detectCardBrand = (number: string): string => {
    if (number.startsWith('4')) return 'VISA';
    if (number.startsWith('5')) return 'MASTERCARD';
    if (number.startsWith('34') || number.startsWith('37')) return 'AMEX';
    return 'UNKNOWN';
  };

  const handleCardChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    const newCardData = { ...cardData, [name]: value };
    setCardData(newCardData);
    
    // Standard validation
    let error = validatePaymentMethod(name, value, 'CARD');
    
    // Future date validation if it's month or year
    if (!error && (name === 'expiryMonth' || name === 'expiryYear')) {
      error = validateExpiryDate(newCardData.expiryMonth, newCardData.expiryYear);
    }
    
    setErrors(prev => ({ ...prev, [name]: error }));
  };

  const handleUpiChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    setUpiId(value);
    
    // Validation on change
    const error = validatePaymentMethod('upiId', value, 'UPI');
    setErrors(prev => ({ ...prev, upiId: error }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Final validation check
    const newErrors: Record<string, string> = {};
    if (paymentType === 'CARD') {
      Object.keys(cardData).forEach(key => {
        let error = validatePaymentMethod(key, cardData[key as keyof typeof cardData], 'CARD');
        if (!error && (key === 'expiryMonth' || key === 'expiryYear')) {
          error = validateExpiryDate(cardData.expiryMonth, cardData.expiryYear);
        }
        if (error) newErrors[key] = error;
      });
    } else {
      const error = validatePaymentMethod('upiId', upiId, 'UPI');
      if (error) newErrors.upiId = error;
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setBackendError('');
    setLoading(true);

    try {
      let paymentData: CustomerService.PaymentMethodRequest;

      if (paymentType === 'CARD') {
        paymentData = {
          paymentType: 'CARD',
          cardNumber: cardData.cardNumber,
          cardholderName: cardData.cardholderName,
          expiryMonth: cardData.expiryMonth,
          expiryYear: cardData.expiryYear,
          cvv: cardData.cvv,
        };
      } else {
        paymentData = {
          paymentType: 'UPI',
          upiId: upiId,
        };
      }

      onComplete({ paymentMethodRequest: paymentData });
    } catch (err: any) {
      setBackendError(err.message || 'Failed to validate payment method');
    } finally {
      setLoading(false);
    }
  };

  const cardBrand = detectCardBrand(cardData.cardNumber);

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
          Payment Method
        </h2>
        <p style={{ color: '#6B7280', fontSize: '15px', marginBottom: '32px' }}>
          Add a payment method for your subscription.
        </p>

        {backendError && (
          <div className="alert alert-danger border-0 mb-4" style={{ borderRadius: '12px', background: '#FEF2F2', color: '#DC2626', fontSize: '14px' }}>
            {backendError}
          </div>
        )}

        {/* Payment Type Tabs */}
        <div className="d-flex gap-3 mb-4">
          <button
            type="button"
            onClick={() => {
              setPaymentType('CARD');
              setErrors({});
            }}
            className="btn flex-fill d-flex align-items-center justify-content-center gap-2 shadow-none"
            style={{
              padding: '14px',
              borderRadius: '12px',
              border: paymentType === 'CARD' ? '2px solid #5B4FFF' : '1px solid #E5E7EB',
              background: paymentType === 'CARD' ? 'rgba(91, 79, 255, 0.05)' : 'white',
              color: paymentType === 'CARD' ? '#5B4FFF' : '#6B7280',
              transition: 'all 0.2s'
            }}
          >
            <CreditCard size={20} />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>Card</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setPaymentType('UPI');
              setErrors({});
            }}
            className="btn flex-fill d-flex align-items-center justify-content-center gap-2 shadow-none"
            style={{
              padding: '14px',
              borderRadius: '12px',
              border: paymentType === 'UPI' ? '2px solid #5B4FFF' : '1px solid #E5E7EB',
              background: paymentType === 'UPI' ? 'rgba(91, 79, 255, 0.05)' : 'white',
              color: paymentType === 'UPI' ? '#5B4FFF' : '#6B7280',
              transition: 'all 0.2s'
            }}
          >
            <Smartphone size={20} />
            <span style={{ fontSize: '14px', fontWeight: 600 }}>UPI</span>
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {paymentType === 'CARD' ? (
            <>
              {/* Card Number */}
              <div className="mb-4">
                <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
                  Card Number
                </label>
                <div className="position-relative">
                  <input
                    type="text"
                    name="cardNumber"
                    value={cardData.cardNumber}
                    onChange={handleCardChange}
                    placeholder="1234 5678 9012 3456"
                    maxLength={16}
                    className={`form-control shadow-none ${errors.cardNumber ? 'is-invalid' : ''}`}
                    style={{ 
                      padding: '12px 16px', 
                      border: `1px solid ${errors.cardNumber ? '#DC2626' : '#D1D5DB'}`,
                      borderRadius: '12px',
                      fontSize: '15px',
                      paddingRight: '64px'
                    }}
                    required
                  />
                  {cardBrand !== 'UNKNOWN' && (
                    <span 
                      className="position-absolute"
                      style={{ 
                        right: '16px', 
                        top: '50%', 
                        transform: 'translateY(-50%)',
                        fontSize: '11px',
                        fontWeight: 700,
                        color: '#5B4FFF',
                        background: 'rgba(91, 79, 255, 0.1)',
                        padding: '2px 8px',
                        borderRadius: '4px'
                      }}
                    >
                      {cardBrand}
                    </span>
                  )}
                </div>
                {errors.cardNumber && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.cardNumber}</div>}
              </div>

              {/* Cardholder Name */}
              <div className="mb-4">
                <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
                  Cardholder Name
                </label>
                <input
                  type="text"
                  name="cardholderName"
                  value={cardData.cardholderName}
                  onChange={handleCardChange}
                  placeholder="Name as on card"
                  className={`form-control shadow-none ${errors.cardholderName ? 'is-invalid' : ''}`}
                  style={{ 
                    padding: '12px 16px', 
                    border: `1px solid ${errors.cardholderName ? '#DC2626' : '#D1D5DB'}`,
                    borderRadius: '12px',
                    fontSize: '15px'
                  }}
                  required
                />
                {errors.cardholderName && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.cardholderName}</div>}
              </div>

              {/* Expiry and CVV */}
              <div className="row g-3 mb-4">
                <div className="col-6">
                  <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
                    Expiry Date
                  </label>
                  <div className="d-flex gap-2">
                    <input
                      type="text"
                      name="expiryMonth"
                      value={cardData.expiryMonth}
                      onChange={handleCardChange}
                      placeholder="MM"
                      maxLength={2}
                      className={`form-control shadow-none text-center ${errors.expiryMonth ? 'is-invalid' : ''}`}
                      style={{ 
                        padding: '12px 0', 
                        border: `1px solid ${errors.expiryMonth ? '#DC2626' : '#D1D5DB'}`,
                        borderRadius: '12px',
                        fontSize: '15px'
                      }}
                      required
                    />
                    <input
                      type="text"
                      name="expiryYear"
                      value={cardData.expiryYear}
                      onChange={handleCardChange}
                      placeholder="YYYY"
                      maxLength={4}
                      className={`form-control shadow-none text-center ${errors.expiryYear ? 'is-invalid' : ''}`}
                      style={{ 
                        padding: '12px 0', 
                        border: `1px solid ${errors.expiryYear ? '#DC2626' : '#D1D5DB'}`,
                        borderRadius: '12px',
                        fontSize: '15px'
                      }}
                      required
                    />
                  </div>
                  {(errors.expiryMonth || errors.expiryYear) && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.expiryMonth || errors.expiryYear}</div>}
                </div>
                <div className="col-6">
                  <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
                    CVV
                  </label>
                  <input
                    type="password"
                    name="cvv"
                    value={cardData.cvv}
                    onChange={handleCardChange}
                    placeholder="123"
                    maxLength={4}
                    className={`form-control shadow-none text-center ${errors.cvv ? 'is-invalid' : ''}`}
                    style={{ 
                      padding: '12px 0', 
                      border: `1px solid ${errors.cvv ? '#DC2626' : '#D1D5DB'}`,
                      borderRadius: '12px',
                      fontSize: '15px'
                    }}
                    required
                  />
                  {errors.cvv && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.cvv}</div>}
                </div>
              </div>
            </>
          ) : (
            /* UPI */
            <div className="mb-4">
              <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
                UPI ID
              </label>
              <input
                type="text"
                name="upiId"
                value={upiId}
                onChange={handleUpiChange}
                placeholder="yourname@upi"
                className={`form-control shadow-none ${errors.upiId ? 'is-invalid' : ''}`}
                style={{ 
                  padding: '12px 16px', 
                  border: `1px solid ${errors.upiId ? '#DC2626' : '#D1D5DB'}`,
                  borderRadius: '12px',
                  fontSize: '15px'
                }}
                required
              />
              {errors.upiId && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.upiId}</div>}
              <small style={{ color: '#6B7280', fontSize: '12px', marginTop: '6px', display: 'block' }}>
                Enter your UPI ID (e.g., name@upi)
              </small>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="btn w-100 d-flex align-items-center justify-content-center gap-2 mt-2"
            style={{
              background: '#111827',
              color: 'white',
              border: 'none',
              padding: '16px 24px',
              borderRadius: '9999px',
              fontSize: '15px',
              fontWeight: 600,
              transition: 'all 0.2s',
              opacity: loading ? 0.7 : 1
            }}
          >
            {loading ? (
              <>
                <Loader2 size={20} className="spin" />
                Processing...
              </>
            ) : (
              <>
                <Check size={20} />
                Confirm Payment Method
              </>
            )}
          </button>
        </form>

        {/* Security Note */}
        <div className="mt-5 text-center">
          <p style={{ fontSize: '12px', color: '#6B7280', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
            <span style={{ color: '#059669' }}>🔒 Secure</span> Your payment information is encrypted.
          </p>
        </div>
      </div>
    </div>
  );
};
