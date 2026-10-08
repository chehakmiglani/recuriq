import React, { useEffect, useState } from 'react';
import * as CustomerService from '../../services/customer/customerService';
import {
  User,
  Mail,
  Phone,
  MapPin,
  Building,
  Save,
  Check,
  Shield,
  Edit3
} from 'lucide-react';
import './ProfilePage.css';

export const ProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<CustomerService.CustomerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [formData, setFormData] = useState<Partial<CustomerService.CustomerProfile>>({});

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const data = await CustomerService.getCustomerProfile();
      setProfile(data);
      setFormData({
        phone: data.phone || '',
        state: data.state || '',
        city: data.city || '',
        addressLine1: data.addressLine1 || '',
        postalCode: data.postalCode || ''
      });
    } catch (error) {
      console.error('Failed to load profile:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaved(false);

    try {
      await CustomerService.updateCustomerProfile(formData);
      setSaved(true);
      await loadProfile();
      setTimeout(() => setSaved(false), 3000);
    } catch (error) {
      alert('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (field: keyof CustomerService.CustomerProfile, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  if (loading) {
    return (
      <div className="profile-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="profile-page">
      {saved && (
        <div className="success-banner">
          <Check size={20} />
          <span>Profile updated successfully!</span>
        </div>
      )}

      {/* Account Information (Read-only) */}
      <div className="profile-card">
        <div className="card-header">
          <Shield size={20} className="header-icon" />
          <h2 className="card-title">Account Information</h2>
        </div>
        <div className="info-grid">
          <div className="info-item">
            <div className="info-icon-wrapper">
              <User size={18} />
            </div>
            <div className="info-content">
              <span className="info-label">Full Name</span>
              <span className="info-value">{profile?.fullName}</span>
            </div>
          </div>
          <div className="info-item">
            <div className="info-icon-wrapper">
              <Mail size={18} />
            </div>
            <div className="info-content">
              <span className="info-label">Email Address</span>
              <span className="info-value">{profile?.email}</span>
            </div>
          </div>
          <div className="info-item">
            <div className="info-icon-wrapper">
              <Building size={18} />
            </div>
            <div className="info-content">
              <span className="info-label">Currency</span>
              <span className="info-value">{profile?.currency}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Editable Profile Form */}
      <form onSubmit={handleSubmit} className="profile-form-card">
        <div className="card-header">
          <Edit3 size={20} className="header-icon" />
          <h2 className="card-title">Contact & Address</h2>
        </div>

        <div className="form-grid">
          <div className="form-group">
            <label className="form-label">
              <Phone size={16} /> Phone Number
            </label>
            <input
              type="tel"
              className="form-input"
              value={formData.phone || ''}
              onChange={(e) => handleChange('phone', e.target.value)}
              placeholder="+91 98765 43210"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <MapPin size={16} /> State
            </label>
            <input
              type="text"
              className="form-input"
              value={formData.state || ''}
              onChange={(e) => handleChange('state', e.target.value)}
              placeholder="Maharashtra"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <Building size={16} /> City
            </label>
            <input
              type="text"
              className="form-input"
              value={formData.city || ''}
              onChange={(e) => handleChange('city', e.target.value)}
              placeholder="Mumbai"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Postal Code</label>
            <input
              type="text"
              className="form-input"
              value={formData.postalCode || ''}
              onChange={(e) => handleChange('postalCode', e.target.value)}
              placeholder="400001"
            />
          </div>
        </div>

        <div className="form-group full-width">
          <label className="form-label">
            <MapPin size={16} /> Address Line 1
          </label>
          <input
            type="text"
            className="form-input"
            value={formData.addressLine1 || ''}
            onChange={(e) => handleChange('addressLine1', e.target.value)}
            placeholder="123, Main Street, Building Name"
          />
        </div>

        <button
          type="submit"
          disabled={saving}
          className="btn-primary btn-save"
        >
          {saving ? (
            <>
              <div className="btn-spinner"></div> Saving...
            </>
          ) : (
            <>
              <Save size={20} /> Save Changes
            </>
          )}
        </button>
      </form>
    </div>
  );
};
