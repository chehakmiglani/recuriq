import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Check, Loader2 } from 'lucide-react';
import * as CustomerService from '../../services/customer/customerService';
import { useAuthContext } from '../../context/AuthContext';
import '../../styles/LandingPage.css'; // Reuse the excellent UI from Landing Page
type Region = 'IN' | 'US' | 'GB';
interface RegionConfig {
  code: Region;
  name: string;
  flag: string;
  currency: string;
  currencySymbol: string;
  exchangeRate: number; // Against INR
}
const REGIONS: RegionConfig[] = [
  { code: 'IN', name: 'India', flag: '🇮🇳', currency: 'INR', currencySymbol: '₹', exchangeRate: 1 },
  { code: 'US', name: 'United States', flag: '🇺🇸', currency: 'USD', currencySymbol: '$', exchangeRate: 84 },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧', currency: 'GBP', currencySymbol: '£', exchangeRate: 105 },
];
export const PlansPage: React.FC = () => {
  const [plans, setPlans] = useState<CustomerService.Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedRegion, setSelectedRegion] = useState<Region>('IN');
  const [isYearly, setIsYearly] = useState(false);
  const { isAuthenticated } = useAuthContext();
  const navigate = useNavigate();
  const currentRegion = REGIONS.find(r => r.code === selectedRegion) || REGIONS[0];
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/register');
      return;
    }
    const loadPlans = async () => {
      try {
        setLoading(true);
        const allPlans = await CustomerService.getAllPlans(selectedRegion);
        setPlans(allPlans);
      } catch (err: any) {
        setError(err.message || 'Failed to load plans');
      } finally {
        setLoading(false);
      }
    };
    loadPlans();
  }, [isAuthenticated, navigate, selectedRegion]);
  const formatPrice = (priceMinor: number) => {
    const amount = priceMinor / 100;
    if (currentRegion.code === 'IN') {
      return amount.toLocaleString('en-IN');
    }
    return amount.toFixed(2);
  };
  const getPlanDescription = (name: string) => {
    if (name.toLowerCase().includes('premium')) return "Our best video quality in 4K and HDR. Watch on 4 devices at once.";
    if (name.toLowerCase().includes('standard')) return "Great video quality in 1080p. Watch on 2 devices at once.";
    return "Good video quality in 720p. Watch on 1 device at a time.";
  };
  const getPlanFeatures = (planName: string): string[] => {
    if (planName.toLowerCase().includes('basic')) {
      return ['HD (720p) Streaming', '1 Screen at a time', 'Mobile + Tablet access', 'Download Storage (5GB)'];
    } else if (planName.toLowerCase().includes('standard')) {
      return ['Full HD (1080p) Streaming', '2 Screens simultaneously', 'All Devices supported', 'Download Storage (10GB)'];
    } else {
      return ['4K + HDR Streaming', '4 Screens simultaneously', 'All Devices supported', 'Priority Support', 'Download Storage (10GB)'];
    }
  };
  if (loading) {
    return (
      <div className="d-flex align-items-center justify-content-center" style={{ minHeight: '100vh', background: '#F9FAFB' }}>
        <div className="text-center">
          <Loader2 size={48} className="spin" color="#5B4FFF" />
          <p className="mt-3" style={{ color: '#4B5563', fontFamily: 'Inter, sans-serif' }}>Loading plans...</p>
        </div>
      </div>
    );
  }
  const currentBillingPeriod = isYearly ? 'YEARLY' : 'MONTHLY';
  const displayedPlans = plans.filter(p => p.billingPeriod === currentBillingPeriod)
    .sort((a, b) => a.defaultPriceMinor - b.defaultPriceMinor);
  return (
    <div style={{ background: '#F9FAFB', minHeight: '100vh', paddingBottom: '80px', fontFamily: 'Inter, sans-serif' }}>
      {/* Modern Header */}
      <header className="sf-nav" style={{ position: 'static' }}>
        <Link to="/" className="text-decoration-none d-flex align-items-center gap-2" style={{ color: '#4B5563', fontWeight: 500 }}>
          <ArrowLeft size={20} />
          <span>Back to Home</span>
        </Link>
        <Link to="/" className="sf-nav-logo">
          STREAM<span>FLIX</span>
        </Link>
        <div style={{ width: '120px' }}>{/* Spacer for flex balance */}</div>
      </header>
      {/* Pricing Section */}
      <section className="sf-pricing-section" style={{ paddingTop: '60px' }}>
        <div className="sf-pricing-header">
          <h2 className="sf-pricing-title">Choose Your Plan</h2>
          <p className="sf-faq-subtitle">Unlock unlimited access to the best streaming library.</p>

          {error && (
            <div className="alert alert-danger mt-4" role="alert" style={{ maxWidth: '600px', margin: '0 auto' }}>
              {error}
            </div>
          )}
          <div className="sf-pricing-controls" style={{ justifyContent: 'center' }}>
            <div className="sf-toggle-wrap">
              <span className={`sf-toggle-label ${!isYearly ? 'active' : ''}`}>Monthly</span>
              <div className={`sf-toggle ${isYearly ? 'active' : ''}`} onClick={() => setIsYearly(!isYearly)}>
                <div className="sf-toggle-thumb"></div>
              </div>
              <span className={`sf-toggle-label ${isYearly ? 'active' : ''}`}>Yearly</span>
            </div>

            <select
              className="sf-region-select"
              value={selectedRegion}
              onChange={(e) => setSelectedRegion(e.target.value as Region)}
            >
              {REGIONS.map(r => (
                <option key={r.code} value={r.code}>{r.flag} {r.name}</option>
              ))}
            </select>
          </div>
        </div>
        <div className="sf-pricing-grid">
          {displayedPlans.map(plan => {
            const baseName = plan.name.replace(' Monthly', '').replace(' Yearly', '');
            const isFeatured = baseName.toLowerCase() === 'standard';

            return (
              <div key={plan.planId} className={`sf-plan-card ${isFeatured ? 'featured' : ''}`}>
                {isFeatured && <div className="sf-plan-badge">Most Popular</div>}
                <div className="sf-plan-name">{baseName}</div>
                <div className="sf-plan-desc">{getPlanDescription(baseName)}</div>

                <div className="sf-plan-price">
                  {currentRegion.currencySymbol}{formatPrice(plan.defaultPriceMinor)}
                  <span>/{isYearly ? 'year' : 'month'}</span>
                </div>
                <div className="sf-plan-trial">{plan.trialDays}-day free trial</div>

                <ul className="sf-plan-features">
                  {getPlanFeatures(baseName).map((feat, i) => (
                    <li key={i}>
                      <Check size={18} strokeWidth={2.5} />
                      {feat}
                    </li>
                  ))}
                </ul>

                <Link
                  to={`/subscribe?planId=${plan.planId}&step=1`}
                  className={isFeatured ? 'sf-btn-primary' : 'sf-btn-secondary'}
                  style={{ width: '100%', textAlign: 'center', marginTop: 'auto' }}
                >
                  Subscribe Now
                </Link>
              </div>
            );
          })}
        </div>
        {/* No Plans Message */}
        {displayedPlans.length === 0 && !loading && (
          <div className="text-center py-5">
            <p style={{ color: '#6B7280' }}>No plans available for this billing period.</p>
            <button
              onClick={() => window.location.reload()}
              className="sf-btn-primary mt-2"
            >
              Refresh
            </button>
          </div>
        )}
      </section>
    </div>
  );
};
