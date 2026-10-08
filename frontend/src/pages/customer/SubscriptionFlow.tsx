import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, CheckCircle, CreditCard, User, Check } from 'lucide-react';
import { CustomerDetailsStep } from '../../components/customer/subscription/CustomerDetailsStep';
import { PaymentMethodStep } from '../../components/customer/subscription/PaymentMethodStep';
import { MockPaymentStep } from '../../components/customer/subscription/MockPaymentStep';
import * as CustomerService from '../../services/customer/customerService';
import { useAuthContext } from '../../context/AuthContext';

export const SubscriptionFlow: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated, refreshUserStatus } = useAuthContext();
  
  const planId = parseInt(searchParams.get('planId') || '0');
  const currentStep = parseInt(searchParams.get('step') || '1');
  
  const [customerId, setCustomerId] = useState<number | null>(null);
  const [paymentMethodRequest, setPaymentMethodRequest] = useState<CustomerService.PaymentMethodRequest | null>(null);
  const [plan, setPlan] = useState<CustomerService.Plan | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Load plan details
  const loadPlan = useCallback(async (region?: string) => {
    try {
      setLoading(true);
      let resolvedRegion = region;
      
      if (!resolvedRegion) {
        if (customerId) {
          try {
            const profile = await CustomerService.getCustomerProfile();
            resolvedRegion = profile.country;
          } catch (e) {
            // ignore
          }
        } else {
          try {
            const status = await CustomerService.getCustomerStatus();
            if (status.isCustomer) {
              const profile = await CustomerService.getCustomerProfile();
              resolvedRegion = profile.country;
              setCustomerId(profile.customerId);
            }
          } catch (e) {
            // ignore
          }
        }
      }

      const plans = await CustomerService.getAllPlans(resolvedRegion);
      const selectedPlan = plans.find(p => p.planId === planId);
      if (selectedPlan) {
        setPlan(selectedPlan);
      } else {
        setError('Plan not found');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load plan');
    } finally {
      setLoading(false);
    }
  }, [planId, customerId]);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/register');
      return;
    }

    if (!planId) {
      navigate('/plans');
      return;
    }

    loadPlan();
  }, [isAuthenticated, planId, navigate, loadPlan]);

  const handleStepComplete = (step: number, data: any) => {
    if (step === 1) {
      setCustomerId(data.customerId);
      if (data.country) {
        loadPlan(data.country);
      }
      navigate(`/subscribe?planId=${planId}&step=2`);
    } else if (step === 2) {
      setPaymentMethodRequest(data.paymentMethodRequest);
      navigate(`/subscribe?planId=${planId}&step=3`);
    } else if (step === 3) {
      // Refresh user status so Navbar shows "Dashboard"
      refreshUserStatus().then(() => {
        // Redirect to landing page without query params
        navigate('/', { replace: true });
      });
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      navigate(`/subscribe?planId=${planId}&step=${currentStep - 1}`);
    } else {
      navigate('/plans');
    }
  };

  if (loading) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center" style={{ background: '#f8f9fa' }}>
        <div className="text-center">
          <div className="spinner-border" role="status" style={{ color: '#5b4fff' }}>
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3" style={{ color: '#6b7280' }}>Loading...</p>
        </div>
      </div>
    );
  }

  if (error || !plan) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center" style={{ background: '#f8f9fa' }}>
        <div className="text-center">
          <p style={{ color: '#5b4fff' }}>{error || 'Plan not found'}</p>
          <Link to="/plans" className="btn mt-3" style={{ background: '#5b4fff', color: 'white', border: 'none' }}>
            Back to Plans
          </Link>
        </div>
      </div>
    );
  }

  const steps = [
    { number: 1, title: 'Your Details', icon: User },
    { number: 2, title: 'Payment Method', icon: CreditCard },
    { number: 3, title: 'Confirm & Pay', icon: CheckCircle },
  ];

  return (
    <div style={{ background: '#F9FAFB', minHeight: '100vh', fontFamily: 'Inter, sans-serif' }}>
      {/* Header */}
      <header className="py-4 px-4 border-bottom" style={{ borderColor: '#e5e7eb', background: 'white' }}>
        <div className="mx-auto" style={{ maxWidth: '800px' }}>
          <div className="d-flex align-items-center justify-content-between">
            <button 
              onClick={handleBack}
              className="btn btn-link text-decoration-none d-flex align-items-center gap-2 p-0"
              style={{ color: '#4B5563', transition: 'color 0.2s' }}
              onMouseEnter={(e) => e.currentTarget.style.color = '#111827'}
              onMouseLeave={(e) => e.currentTarget.style.color = '#4B5563'}
            >
              <ArrowLeft size={20} />
              <span style={{ fontSize: '14px', fontWeight: 500 }}>
                {currentStep === 1 ? 'Back to Plans' : 'Previous Step'}
              </span>
            </button>
            
            <h1 style={{ 
              fontFamily: 'Outfit, sans-serif', 
              fontSize: '24px', 
              fontWeight: 700, 
              color: '#111827',
              margin: 0,
              letterSpacing: '-0.5px'
            }}>
              Complete Your Subscription
            </h1>
            
            <div style={{ width: '120px' }} />
          </div>
        </div>
      </header>

      {/* Step Indicator */}
      <div className="py-4 px-4" style={{ background: 'white', borderBottom: '1px solid #e5e7eb' }}>
        <div className="mx-auto" style={{ maxWidth: '600px' }}>
          <div className="d-flex justify-content-between position-relative">
            {/* Progress Bar Background */}
            <div 
              style={{
                position: 'absolute',
                top: '20px',
                left: '10%',
                right: '10%',
                height: '2px',
                background: '#E5E7EB',
                zIndex: 0
              }}
            />
            {/* Active Progress Bar */}
            <div 
              style={{
                position: 'absolute',
                top: '20px',
                left: '10%',
                width: currentStep === 1 ? '0%' : currentStep === 2 ? '40%' : '80%',
                height: '2px',
                background: '#5B4FFF',
                transition: 'width 0.3s ease',
                zIndex: 0
              }}
            />

            {steps.map((step) => {
              const Icon = step.icon;
              const isActive = currentStep === step.number;
              const isCompleted = currentStep > step.number;
              const isPending = currentStep < step.number;

              return (
                <div key={step.number} className="d-flex flex-column align-items-center" style={{ flex: 1, zIndex: 1 }}>
                  {/* Step Circle */}
                  <div
                    className="d-flex align-items-center justify-content-center rounded-circle"
                    style={{
                      width: '40px',
                      height: '40px',
                      background: isCompleted ? '#5B4FFF' : isActive ? '#111827' : 'white',
                      border: isPending ? '2px solid #E5E7EB' : 'none',
                      transition: 'all 0.3s ease',
                      boxShadow: isActive ? '0 0 0 4px rgba(91, 79, 255, 0.1)' : 'none'
                    }}
                  >
                    {isCompleted ? (
                      <Check size={20} color="white" />
                    ) : (
                      <Icon size={18} color={isActive ? 'white' : '#9CA3AF'} />
                    )}
                  </div>

                  {/* Step Title */}
                  <span 
                    className="mt-2"
                    style={{
                      fontSize: '13px',
                      fontWeight: isActive ? 600 : 500,
                      color: isActive ? '#111827' : '#9CA3AF',
                      transition: 'color 0.3s ease'
                    }}
                  >
                    {step.title}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Step Content */}
      <div className="py-5 px-4">
        <div className="mx-auto" style={{ maxWidth: '600px' }}>
          {currentStep === 1 && (
            <CustomerDetailsStep 
              plan={plan} 
              onComplete={(data) => handleStepComplete(1, data)} 
            />
          )}
          {currentStep === 2 && customerId && (
            <PaymentMethodStep 
              plan={plan}
              customerId={customerId}
              onComplete={(data) => handleStepComplete(2, data)} 
            />
          )}
          {currentStep === 3 && customerId && paymentMethodRequest && (
            <MockPaymentStep 
              plan={plan}
              customerId={customerId}
              paymentMethodRequest={paymentMethodRequest}
              onComplete={(data) => handleStepComplete(3, data)} 
            />
          )}
        </div>
      </div>
    </div>
  );
};
