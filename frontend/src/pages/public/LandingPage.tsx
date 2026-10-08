import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  CreditCard,
  Zap,
  Globe,
  Check,
  Tv
} from 'lucide-react';
import '../../styles/LandingPage.css';
import { useAuthContext } from '../../context/AuthContext';
import { publicService } from '../../services/public/publicService';
import type { PlanInfo } from '../../services/public/publicService';
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
// Features based on Streaming Platform
const FEATURES = [
  { icon: Tv, title: 'Ultra HD Experience', desc: 'Stream in stunning 4K resolution with HDR and Dolby Atmos for a truly immersive cinematic experience.' },
  { icon: Globe, title: 'Watch Anywhere', desc: 'Compatible with Smart TVs, consoles, laptops, and mobile devices. Seamlessly sync your progress across all screens.' },
  { icon: Zap, title: 'Ad-Free Entertainment', desc: 'Enjoy uninterrupted viewing. Upgrade your plan with ad-free add-ons to remove commercial breaks entirely.' },
  { icon: CreditCard, title: 'Transparent Billing', desc: 'No hidden fees or long-term contracts. Manage your subscription with smart proration and instant plan changes.' },
];
// FAQ data
const FAQS = [
  { q: 'What is StreamFlix?', a: 'StreamFlix is a streaming service that offers a wide variety of award-winning TV shows, movies, anime, documentaries, and more on thousands of internet-connected devices.' },
  { q: 'How much does StreamFlix cost?', a: 'Watch StreamFlix on your smartphone, tablet, Smart TV, laptop, or streaming device, all for one fixed monthly fee. Plans range from ₹199 to ₹499 a month. No extra costs, no contracts.' },
  { q: 'Where can I watch?', a: 'Watch anywhere, anytime. Sign in with your StreamFlix account to watch instantly on the web at streamflix.com from your personal computer or on any internet-connected device.' },
  { q: 'How do I cancel?', a: 'StreamFlix is flexible. There are no pesky contracts and no commitments. You can easily cancel your account online in two clicks. There are no cancellation fees – start or stop your account anytime.' },
  { q: 'What can I watch on StreamFlix?', a: 'StreamFlix has an extensive library of feature films, documentaries, TV shows, anime, award-winning StreamFlix originals, and more. Watch as much as you want, anytime you want.' },
];
// Helper to determine hardcoded features based on plan base name
const getPlanFeatures = (planName: string) => {
  if (planName.includes('Basic')) return ['HD (720p)', '1 Screen', 'Mobile + Tablet', '7-day Trial'];
  if (planName.includes('Standard')) return ['Full HD (1080p)', '2 Screens', 'All Devices', 'Downloads'];
  if (planName.includes('Premium')) return ['4K + HDR', '4 Screens', 'All Devices', 'Downloads', 'Spatial Audio'];
  return ['Unlimited Content'];
};
const getPlanDescription = (planName: string) => {
  if (planName.includes('Basic')) return 'A great way to enjoy our content on your phone or tablet.';
  if (planName.includes('Standard')) return 'Great video quality in 1080p. Perfect for couples.';
  if (planName.includes('Premium')) return 'Our best video quality in 4K and HDR.';
  return 'The perfect streaming plan for you.';
};
export const LandingPage: React.FC = () => {
  const { user, isAuthenticated, logout, isCustomer, hasDraftSubscription } = useAuthContext();
  const [isYearly, setIsYearly] = useState(false);
  const [selectedRegion, setSelectedRegion] = useState<Region>('IN');
  const [expandedFaq, setExpandedFaq] = useState<number | null>(null);
  const [plans, setPlans] = useState<PlanInfo[]>([]);
  const currentRegion = REGIONS.find(r => r.code === selectedRegion) || REGIONS[0];
  useEffect(() => {
    publicService.getPublicPlans(selectedRegion).then(setPlans);
  }, [selectedRegion]);

  const formatPrice = (priceMinor: number) => {
    const amount = priceMinor / 100;
    if (currentRegion.code === 'IN') {
      return amount.toLocaleString('en-IN');
    }
    return amount.toFixed(2);
  };
const currentBillingPeriod = isYearly ? 'YEARLY' : 'MONTHLY';
// Sort plans so Basic is first, Standard is middle, Premium is last
const displayedPlans = [...plans]
  .filter(p => p.billingPeriod === currentBillingPeriod)
  .sort((a, b) => a.defaultPriceMinor - b.defaultPriceMinor);
return (
  <div className="sf-root">
    {/* Navbar */}
    <nav className="sf-nav">
      <div className="sf-nav-logo">Stream<span>Flix</span></div>
      <div className="sf-nav-right">
        {isAuthenticated ? (
          <>
            {isCustomer === true ? (
              <Link to="/dashboard" className="sf-nav-login">Customer Dashboard</Link>
            ) : user?.role === 'FINANCE' ? (
              <Link to="/finance" className="sf-nav-login">Finance Dashboard</Link>
            ) : user?.role === 'ADMIN' ? (
              <Link to="/admin" className="sf-nav-login">Admin Dashboard</Link>
            ) : hasDraftSubscription ? (
              <Link to="/plans" className="sf-nav-login">Complete Setup</Link>
            ) : (
              <></>
            )}
            <button onClick={logout} className="sf-nav-login" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login" className="sf-nav-login">Sign in</Link>
            <Link to="/register" className="sf-nav-signup">Get Started</Link>
          </>
        )}
      </div>
    </nav>
    {/* Modern Streaming Hero with Blue Gradient Blob and Poster Carousel */}
    <section className="sf-hero-section">
      <div className="sf-hero-blob"></div>
      <div className="sf-hero-content">
        <div className="sf-hero-eyebrow">
          <span className="sf-eyebrow-badge">New</span> Discover the Series Streaming Experience
        </div>
        <h1 className="sf-hero-title">Unlimited movies,<br />TV shows, and more.</h1>
        <p className="sf-hero-subtitle">
          Our expert admins prepare amazing and trending series for you to watch online. Watch anywhere. Cancel anytime.
        </p>
        <div className="sf-hero-actions">
          {isAuthenticated ? (
            isCustomer ? (
              <Link to="/dashboard" className="sf-btn-primary">Go to Dashboard</Link>
            ) : user?.role === 'FINANCE' ? (
              <Link to="/finance" className="sf-btn-primary">Go to Dashboard</Link>
            ) : user?.role === 'ADMIN' ? (
              <Link to="/admin" className="sf-btn-primary">Go to Dashboard</Link>
            ) : hasDraftSubscription ? (
              <Link to="/plans" className="sf-btn-primary">Complete Setup</Link>
            ) : (
              <Link to="/plans" className="sf-btn-primary">Explore Plans</Link>
            )
          ) : (
            <>
              <Link to="/register" className="sf-btn-primary">Get started</Link>
              <button onClick={() => document.getElementById('pricing')?.scrollIntoView({ behavior: 'smooth' })} className="sf-btn-secondary">View plans</button>
            </>
          )}
        </div>
      </div>
      {/* Curved 3D Poster Carousel */}
      <div className="sf-hero-carousel-container">
        <div className="sf-hero-carousel">
          <div className="sf-carousel-card left">
            <img src="\assets\image2.jpg" alt="Cinematic Movie Scene" />
          </div>
          <div className="sf-carousel-card center">
            <img src="\assets\image1.jpg" alt="Family Movie Scene" />
          </div>
          <div className="sf-carousel-card right">
            <img src="\assets\image3.jpg" alt="Animated Movie Scene" />
          </div>
        </div>
      </div>
    </section>
    {/* Features Strip */}
    <section className="sf-features-strip">
      {FEATURES.map((feat, idx) => (
        <div key={idx} className="sf-feat-item">
          <div className="sf-feat-icon">
            <feat.icon size={24} strokeWidth={2} />
          </div>
          <div className="sf-feat-title">{feat.title}</div>
          <div className="sf-feat-desc">{feat.desc}</div>
        </div>
      ))}
    </section>
    {/* Pricing Section */}
    <section id="pricing" className="sf-pricing-section">
      <div className="sf-pricing-header">
        <h2 className="sf-pricing-title">Simple, transparent pricing</h2>
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
      <div className="sf-pricing-grid" style={{ minHeight: '494px', overflowAnchor: 'none' }}>
        {displayedPlans.length === 0 ? (
          <p>Loading plans...</p>
        ) : (
          displayedPlans.map(plan => {
            const isPopular = plan.name.includes('Standard');
            const isFeatured = plan.name.includes('Premium');
            const baseName = plan.name.replace(' Monthly', '').replace(' Yearly', '');
            const planLink = isAuthenticated
              ? (user?.role === 'ADMIN'
                ? '/admin'
                : user?.role === 'FINANCE'
                  ? '/finance'
                  : isCustomer
                    ? '/dashboard/subscription'
                    : `/subscribe?planId=${plan.planId}&step=1`)
              : `/register?planId=${plan.planId}`;
            return (
              <div key={plan.planId} className={`sf-plan-card ${isFeatured ? 'featured' : ''}`}>
                {isPopular && <div className="sf-plan-badge" style={{ backgroundColor: '#4B5563' }}>Popular</div>}
                {isFeatured && <div className="sf-plan-badge">Best Value</div>}
                <div className="sf-plan-name">{baseName}</div>
                <div className="sf-plan-desc">{getPlanDescription(baseName)}</div>
                <div className="sf-plan-price">
                  {currentRegion.currencySymbol}{formatPrice(plan.defaultPriceMinor)}
                  <span>/{isYearly ? 'year' : 'month'}</span>
                </div>
                <div className="sf-plan-trial">{plan.trialDays}-day free trial</div>
                <ul className="sf-plan-features">
                  {getPlanFeatures(baseName).map((feat, idx) => (
                    <li key={idx}><Check size={16} /> {feat}</li>
                  ))}
                </ul>
                <Link to={planLink} className="sf-plan-btn">Choose {baseName}</Link>
              </div>
            );
          })
        )}
      </div>
    </section>
    {/* FAQ Section */}
    <section className="sf-faq-section">
      <div className="sf-faq-header">
        <h2 className="sf-faq-title">Frequently Asked Questions</h2>
        <p className="sf-faq-subtitle">Everything you need to know about StreamFlix and billing.</p>
      </div>
      <div className="sf-faq-grid">
        {FAQS.map((faq, idx) => (
          <div
            key={idx}
            className={`sf-faq-item ${expandedFaq === idx ? 'expanded' : ''}`}
            onClick={() => setExpandedFaq(expandedFaq === idx ? null : idx)}
          >
            <div className="sf-faq-q">
              {faq.q}
              <div className="sf-faq-icon"></div>
            </div>
            <div className="sf-faq-a">
              <div className="sf-faq-a-inner">{faq.a}</div>
            </div>
          </div>
        ))}
      </div>
    </section>
    {/* Footer */}
    <footer className="sf-footer">
      <div className="sf-footer-logo">Stream<span>Flix</span></div>
      <div className="sf-footer-links">
        <Link to="/privacy" className="sf-footer-link">Privacy</Link>
        <Link to="/terms" className="sf-footer-link">Terms</Link>
        <Link to="/help" className="sf-footer-link">Help</Link>
        <Link to="/careers" className="sf-footer-link">Careers</Link>
        {!isAuthenticated && (
          <Link to="/management/login" className="sf-footer-link sf-staff-login">
            Staff Login →
          </Link>
        )}
      </div>
    </footer>
  </div>
);
};
export default LandingPage;
