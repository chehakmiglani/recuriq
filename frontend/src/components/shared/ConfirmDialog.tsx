import React from 'react';
import { AlertCircle, X } from 'lucide-react';
import './ConfirmDialog.css';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string | React.ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
  confirmLabel?: string;
  cancelLabel?: string;
  isDanger?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  onConfirm,
  onCancel,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isDanger = false
}) => {
  if (!isOpen) return null;

  return (
    <div className="confirm-dialog-overlay" onClick={onCancel}>
      <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="confirm-dialog-header">
          <div className="confirm-dialog-title-container">
            {isDanger && <AlertCircle className="confirm-dialog-icon danger" size={20} />}
            <h3 className="confirm-dialog-title">{title}</h3>
          </div>
          <button className="confirm-dialog-close" onClick={onCancel}>
            <X size={20} />
          </button>
        </div>
        
        <div className="confirm-dialog-body">
          <div className="confirm-dialog-message">
            {typeof message === 'string' ? (
              message.split('\n').map((line, i) => (
                <React.Fragment key={i}>
                  {line}
                  {i < message.split('\n').length - 1 && <br />}
                </React.Fragment>
              ))
            ) : (
              message
            )}
          </div>
        </div>
        
        <div className="confirm-dialog-footer">
          {cancelLabel && (
            <button className="btn-dialog-cancel" onClick={onCancel}>
              {cancelLabel}
            </button>
          )}
          <button 
            className={`btn-dialog-confirm ${isDanger ? 'danger' : ''}`} 
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
