import React from 'react';

interface FormFieldProps {
  label: string;
  value: string | number;
  onChange: (value: string) => void;
  type?: 'text' | 'number' | 'email' | 'date' | 'password';
  placeholder?: string;
  error?: string;
  required?: boolean;
  options?: { value: string; label: string }[];
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  error,
  required,
  options,
}) => {
  return (
    <div className="form-field">
      <label className="form-field-label">
        {label} {required && <span style={{ color: '#EF4444' }}>*</span>}
      </label>
      {options ? (
        <select
          className={`form-field-input ${error ? 'error' : ''}`}
          value={String(value)}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">Select {label.toLowerCase()}</option>
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ) : (
        <input
          className={`form-field-input ${error ? 'error' : ''}`}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
        />
      )}
      {error && <div className="form-field-error">{error}</div>}
    </div>
  );
};
