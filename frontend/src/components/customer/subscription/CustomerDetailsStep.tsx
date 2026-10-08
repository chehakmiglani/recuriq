import React, { useState } from 'react';
import { Loader2, MapPin, Phone, Globe } from 'lucide-react';
import type { Plan } from '../../../services/customer/customerService';
import * as CustomerService from '../../../services/customer/customerService';
import { validateCustomerDetails } from '../../../utils/subscriptionValidation';

interface CustomerDetailsStepProps {
  plan: Plan;
  onComplete: (data: { customerId: number; country: string }) => void;
}

const countryCurrencyMap: Record<string, string> = {
  'IN': 'INR',
  'US': 'USD',
  'GB': 'GBP',
};

export const CustomerDetailsStep: React.FC<CustomerDetailsStepProps> = ({ plan, onComplete }) => {
  const [formData, setFormData] = useState({
    phone: '',
    country: 'IN',
    state: '',
    city: '',
    addressLine1: '',
    postalCode: '',
    currency: 'INR',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [backendError, setBackendError] = useState('');

  const handleCountryChange = (country: string) => {
    setFormData(prev => ({
      ...prev,
      country,
      currency: countryCurrencyMap[country] || 'INR'
    }));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Field level validation on change
    const error = validateCustomerDetails(name, value);
    setErrors(prev => ({ ...prev, [name]: error }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Final validation check
    const newErrors: Record<string, string> = {};
    Object.keys(formData).forEach(key => {
      const error = validateCustomerDetails(key, formData[key as keyof typeof formData]);
      if (error) newErrors[key] = error;
    });

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setBackendError('');
    setLoading(true);

    try {
      const result = await CustomerService.registerCustomerDetails(formData);
      onComplete({ customerId: result.customerId, country: formData.country });
    } catch (err: any) {
      setBackendError(err.message || 'Failed to save your details');
    } finally {
      setLoading(false);
    }
  };

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
          Your Details
        </h2>
        <p style={{ color: '#6B7280', fontSize: '15px', marginBottom: '32px' }}>
          Please provide your contact and address information to continue.
        </p>

        {backendError && (
          <div className="alert alert-danger border-0 mb-4" style={{ borderRadius: '12px', background: '#FEF2F2', color: '#DC2626', fontSize: '14px' }}>
            {backendError}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Country Selection */}
          <div className="mb-4">
            <label className="form-label d-flex align-items-center gap-2" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
              <Globe size={18} color="#5B4FFF" />
              Country
            </label>
            <select
              name="country"
              value={formData.country}
              onChange={(e) => handleCountryChange(e.target.value)}
              className={`form-select shadow-none ${errors.country ? 'is-invalid' : ''}`}
              style={{ 
                padding: '12px 16px', 
                border: `1px solid ${errors.country ? '#DC2626' : '#D1D5DB'}`,
                borderRadius: '12px',
                fontSize: '15px',
                background: '#FFFFFF'
              }}
              required
            >
              <option value="IN">🇮🇳 India</option>
              <option value="US">🇺🇸 United States</option>
              <option value="GB">🇬🇧 United Kingdom</option>
            </select>
            {errors.country && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.country}</div>}
          </div>

          {/* Phone */}
          <div className="mb-4">
            <label className="form-label d-flex align-items-center gap-2" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
              <Phone size={18} color="#5B4FFF" />
              Phone Number
            </label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              placeholder="Enter your phone number"
              className={`form-control shadow-none ${errors.phone ? 'is-invalid' : ''}`}
              style={{ 
                padding: '12px 16px', 
                border: `1px solid ${errors.phone ? '#DC2626' : '#D1D5DB'}`,
                borderRadius: '12px',
                fontSize: '15px'
              }}
              required
            />
            {errors.phone && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.phone}</div>}
          </div>

          {/* Address */}
          <div className="mb-4">
            <label className="form-label d-flex align-items-center gap-2" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
              <MapPin size={18} color="#5B4FFF" />
              Address
            </label>
            <input
              type="text"
              name="addressLine1"
              value={formData.addressLine1}
              onChange={handleChange}
              placeholder="Street address"
              className={`form-control shadow-none mb-3 ${errors.addressLine1 ? 'is-invalid' : ''}`}
              style={{ 
                padding: '12px 16px', 
                border: `1px solid ${errors.addressLine1 ? '#DC2626' : '#D1D5DB'}`,
                borderRadius: '12px',
                fontSize: '15px'
              }}
              required
            />
            {errors.addressLine1 && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500, marginBottom: '12px' }}>{errors.addressLine1}</div>}
            
            <div className="row g-3">
              <div className="col-md-6">
                <input
                  type="text"
                  name="city"
                  value={formData.city}
                  onChange={handleChange}
                  placeholder="City"
                  className={`form-control shadow-none ${errors.city ? 'is-invalid' : ''}`}
                  style={{ 
                    padding: '12px 16px', 
                    border: `1px solid ${errors.city ? '#DC2626' : '#D1D5DB'}`,
                    borderRadius: '12px',
                    fontSize: '15px'
                  }}
                  required
                />
                {errors.city && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.city}</div>}
              </div>
              <div className="col-md-6">
                <input
                  type="text"
                  name="state"
                  value={formData.state}
                  onChange={handleChange}
                  placeholder="State"
                  className={`form-control shadow-none ${errors.state ? 'is-invalid' : ''}`}
                  style={{ 
                    padding: '12px 16px', 
                    border: `1px solid ${errors.state ? '#DC2626' : '#D1D5DB'}`,
                    borderRadius: '12px',
                    fontSize: '15px'
                  }}
                  required
                />
                {errors.state && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.state}</div>}
              </div>
            </div>
          </div>

          {/* Postal Code */}
          <div className="mb-4">
            <label className="form-label" style={{ fontSize: '14px', fontWeight: 600, color: '#4B5563', marginBottom: '8px' }}>
              Postal Code
            </label>
            <input
              type="text"
              name="postalCode"
              value={formData.postalCode}
              onChange={handleChange}
              placeholder="Enter postal code"
              className={`form-control shadow-none ${errors.postalCode ? 'is-invalid' : ''}`}
              style={{ 
                padding: '12px 16px', 
                border: `1px solid ${errors.postalCode ? '#DC2626' : '#D1D5DB'}`,
                borderRadius: '12px',
                fontSize: '15px'
              }}
              required
            />
            {errors.postalCode && <div style={{ color: '#DC2626', fontSize: '12px', marginTop: '4px', fontWeight: 500 }}>{errors.postalCode}</div>}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="btn w-100 d-flex align-items-center justify-content-center gap-2"
            style={{
              background: '#5B4FFF',
              color: 'white',
              border: 'none',
              padding: '16px 24px',
              borderRadius: '9999px',
              fontSize: '15px',
              fontWeight: 600,
              transition: 'all 0.2s',
              opacity: loading ? 0.7 : 1,
              boxShadow: '0 4px 12px rgba(91, 79, 255, 0.2)'
            }}
          >
            {loading ? (
              <>
                <Loader2 size={20} className="spin" />
                Saving...
              </>
            ) : (
              'Continue to Payment'
            )}
          </button>
        </form>

        {/* Plan Summary */}
        <div
          className="mt-5 p-4"
          style={{
            background: '#F9FAFB',
            borderRadius: '16px',
            border: '1px solid #E5E7EB'
          }}
        >
          <h4 style={{ fontFamily: 'Outfit, sans-serif', fontSize: '14px', fontWeight: 700, color: '#111827', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Subscription Details
          </h4>
          <div className="d-flex justify-content-between align-items-center">
            <div>
              <p style={{ fontSize: '17px', fontWeight: 600, color: '#111827', margin: 0 }}>
                {plan.name}
              </p>
              <p style={{ fontSize: '14px', color: '#6B7280', margin: 0 }}>
                {plan.billingPeriod === 'YEARLY' ? 'Billed annually' : 'Billed monthly'}
              </p>
            </div>
            <div className="text-end">
              <p style={{ fontSize: '22px', fontWeight: 700, color: '#5B4FFF', margin: 0 }}>
                {(plan.defaultPriceMinor / 100).toFixed(0)} {plan.defaultCurrency}
              </p>
              {plan.trialDays > 0 && (
                <span style={{ fontSize: '12px', color: '#059669', fontWeight: 600, background: '#ECFDF5', padding: '2px 8px', borderRadius: '4px' }}>
                  {plan.trialDays} Days Free
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
