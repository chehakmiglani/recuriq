import React from 'react';
import { X } from 'lucide-react';
import '../../../styles/admin-modal.css';

interface AdminModalProps {
  isOpen: boolean;
  title: string;
  onClose: () => void;
  onSave: () => void;
  saveLabel?: string;
  saving?: boolean;
  children: React.ReactNode;
}

export const AdminModal: React.FC<AdminModalProps> = ({
  isOpen,
  title,
  onClose,
  onSave,
  saveLabel = 'Save',
  saving = false,
  children,
}) => {
  if (!isOpen) return null;

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <span className="admin-modal-title">{title}</span>
          <button className="admin-modal-close" onClick={onClose}>
            <X size={16} />
          </button>
        </div>
        <div className="admin-modal-body">{children}</div>
        <div className="admin-modal-footer">
          <button className="btn-admin-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button className="btn-admin-primary" onClick={onSave} disabled={saving}>
            {saving ? 'Saving...' : saveLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
