import React from 'react';
import { CheckCircle, Clock, XCircle, AlertTriangle, MinusCircle, Info } from 'lucide-react';

interface Props {
  status: string;
}

const statusMap: Record<string, string> = {
  // Positive states  --green
  ACTIVE: 'active',
  PAID: 'active',
  SUCCESS: 'active',
  SUCCESSFUL: 'active',
  APPROVED: 'active',
  COMPLETED: 'active',
  APPLIED: 'active',

  // Pending/Warning states --yellow
  PENDING: 'past_due',
  PROCESSING: 'past_due',
  OPEN: 'past_due',
  ISSUED: 'past_due',
  PAST_DUE: 'past_due',
  

  // Failure/Cancel states --red
  FAILED: 'canceled',
  REJECTED: 'canceled',
  CANCELED: 'canceled',
  OVERDUE: 'canceled',
  UNCOLLECTIBLE: 'canceled',
  EXPIRED: 'canceled',
  DISABLED: 'canceled',

  // Refunded/Suspended/Inactive states --grey

  SUSPENDED: 'suspended',
  PAUSED: 'suspended',
  ON_HOLD: 'suspended',
  DRAFT: 'inactive',
  VOID: 'inactive',
  VOIDED: 'inactive',
  INACTIVE: 'inactive',

//neutral states --blue
  REFUNDED: 'info',
  TRIALING: 'info',
  PARTIALLY_REFUNDED: 'info',

};

const iconMap: Record<string, React.ReactNode> = {
  active: <CheckCircle size={12} />,
  past_due: <Clock size={12} />,
  trialing: <Info size={12} />,
  canceled: <XCircle size={12} />,
  suspended: <AlertTriangle size={12} />,
  inactive: <MinusCircle size={12} />,
};

export const StatusBadge: React.FC<Props> = ({ status }) => {
  const s = status?.toUpperCase() || '';
  const className = statusMap[s] || 'inactive';
  
  // Keep clean/humanized labels
  let label = status;
  if (s === 'SUCCESS') label = 'Success';
  else if (s === 'PAID') label = 'Paid';
  else if (s === 'PARTIALLY_REFUNDED') label = 'Partial Refund';
  else if (s === 'UNCOLLECTIBLE') label = 'Uncollectible';

  return (
    <span className={`status-badge ${className}`}>
      {iconMap[className] || iconMap['inactive']}
      {label}
    </span>
  );
};
