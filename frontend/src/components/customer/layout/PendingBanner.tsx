import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchWithSession } from '../../../services/auth/authService';
import { AlertTriangle, X, CreditCard } from 'lucide-react';

// Self-contained interface for notifications
export interface NotificationItem {
  notificationId: number;
  type: string;
  subject: string;
  body: string;
  channel: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  scheduledAt: string;
  sentAt: string | null;
  createdAt: string;
}

export const PendingBanner: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [dismissed, setDismissed] = useState(false);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    let mounted = true;

    const fetchNotifications = async () => {
      try {
        const response = await fetchWithSession('/notifications/me');
        if (!response.ok) return;
       
        const all: NotificationItem[] = await response.json();
        const pending = all.filter(n => n.type.startsWith('PENDING_PAYMENT'));
       
        if (mounted) {
          setNotifications(pending);
        }
      } catch (err) {
        console.error('Failed to fetch notifications:', err);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  if (loading || dismissed || notifications.length === 0) {
    return null;
  }

  const latest = notifications[0];

  return (
    <div
      id="pending-payment-banner"
      style={{
        width: '100%',
        background: 'linear-gradient(135deg, #FEF3C7 0%, #FDE68A 50%, #FCD34D 100%)',
        borderBottom: '1px solid #F59E0B',
        padding: '12px 40px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '16px',
        boxSizing: 'border-box',
        animation: 'slideDown 0.3s ease-out',
      }}
    >
      <style>{`
        @keyframes slideDown {
          from { opacity: 0; transform: translateY(-100%); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulse-icon {
          0%, 100% { transform: scale(1); }
          50% { transform: scale(1.15); }
        }
      `}</style>

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '36px',
          height: '36px',
          borderRadius: '10px',
          background: 'rgba(245, 158, 11, 0.2)',
          flexShrink: 0,
          animation: 'pulse-icon 2s ease-in-out infinite',
        }}>
          <AlertTriangle size={20} color="#B45309" />
        </div>

        <div style={{ flex: 1 }}>
          <p style={{ margin: 0, fontSize: '13.5px', fontWeight: 600, color: '#92400E' }}>
            {latest.subject}
          </p>
          <p style={{ margin: '2px 0 0 0', fontSize: '12.5px', fontWeight: 500, color: '#A16207' }}>
            {latest.body}
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
        <button
          onClick={() => navigate('/dashboard/payment-methods')}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: '8px 18px',
            background: '#B45309',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '12.5px',
            fontWeight: 600,
            cursor: 'pointer',
            boxShadow: '0 2px 6px rgba(180, 83, 9, 0.3)',
            transition: 'all 0.2s'
          }}
        >
          <CreditCard size={14} />
          Update Payment
        </button>

        <button
          onClick={() => setDismissed(true)}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '30px',
            height: '30px',
            background: 'rgba(146, 64, 14, 0.1)',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer'
          }}
        >
          <X size={16} color="#92400E" />
        </button>
      </div>
    </div>
  );
};
