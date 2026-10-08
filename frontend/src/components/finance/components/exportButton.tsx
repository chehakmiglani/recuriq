import React, { useState } from 'react';
import { Download, FileText, FileSpreadsheet } from 'lucide-react';
import { financeService } from '../../../services/finance/financeService';

interface Props {
  type: string;
}

export const ExportButton: React.FC<Props> = ({ type }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);
  const [btnHovered, setBtnHovered] = useState(false);

  const handleExport = async (format: 'CSV' | 'PDF') => {
    try {
      setIsExporting(true);
      setIsOpen(false);

      const blob = (await financeService.exportFinanceReport(type, format));
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      const ext = format.toLowerCase();
      a.download = `report-${type}.${ext}`;

      document.body.appendChild(a);
      a.click();

      setTimeout(() => {
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      });

    } catch (err) {
      alert('Export failed. Please try again.');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        onMouseEnter={() => setBtnHovered(true)}
        onMouseLeave={() => setBtnHovered(false)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: isExporting ? '#9ca3af' : btnHovered ? '#473bcc' : '#0f766e',
          color: '#ffffff',
          border: 'none',
          borderRadius: '8px',
          padding: '10px 18px',
          fontWeight: 600,
          fontSize: '13px',
          cursor: isExporting ? 'not-allowed' : 'pointer',
          boxShadow: '0 4px 12px rgba(15, 118, 110, 0.2)',
          transition: 'all 0.2s ease-in-out',
          transform: btnHovered && !isExporting ? 'translateY(-1px)' : 'none',
          outline: 'none'
        }}
        disabled={isExporting}
      >
        <Download size={16} />
        {isExporting ? 'Exporting...' : 'Export Data'}
      </button>

      {isOpen && (
        <>
          {/* Transparent click-outside overlay */}
          <div 
            onClick={() => setIsOpen(false)} 
            style={{ position: 'fixed', inset: 0, zIndex: 40, cursor: 'default' }} 
          />
          
          <div style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            background: '#ffffff',
            border: '1px solid #e2e8f0',
            borderRadius: '12px',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05)',
            zIndex: 50,
            minWidth: '180px',
            padding: '6px',
            display: 'flex',
            flexDirection: 'column',
            gap: '2px'
          }}>
            <button
              onClick={() => handleExport('CSV')}
              onMouseEnter={() => setHoveredIdx(0)}
              onMouseLeave={() => setHoveredIdx(null)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                width: '100%',
                padding: '10px 14px',
                background: hoveredIdx === 0 ? '#f0fdfa' : 'transparent',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer',
                textAlign: 'left',
                color: hoveredIdx === 0 ? '#0f766e' : '#4b5563',
                fontSize: '13px',
                fontWeight: 500,
                transition: 'all 0.15s ease-in-out',
                outline: 'none'
              }}
            >
              <FileSpreadsheet size={16} style={{ color: hoveredIdx === 0 ? '#0f766e' : '#9ca3af' }} />
              Export as CSV
            </button>
            
            <button
              onClick={() => handleExport('PDF')}
              onMouseEnter={() => setHoveredIdx(1)}
              onMouseLeave={() => setHoveredIdx(null)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                width: '100%',
                padding: '10px 14px',
                background: hoveredIdx === 1 ? '#f0fdfa' : 'transparent',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer',
                textAlign: 'left',
                color: hoveredIdx === 1 ? '#0f766e' : '#4b5563',
                fontSize: '13px',
                fontWeight: 500,
                transition: 'all 0.15s ease-in-out',
                outline: 'none'
              }}
            >
              <FileText size={16} style={{ color: hoveredIdx === 1 ? '#0f766e' : '#9ca3af' }} />
              Export as PDF
            </button>
          </div>
        </>
      )}
    </div>
  );
};
