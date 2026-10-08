import React, { useState } from 'react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { DataTable } from '../../components/admin/shared/DataTable';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { searchCustomers, getCustomerDetails, type CustomerSearchResponse, type CustomerDetailResponse } from '../../services/support/supportService';
import { Search } from 'lucide-react';

export const SupportDashboardPage: React.FC = () => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<CustomerSearchResponse[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [activeTab, setActiveTab] = useState('subscriptions');

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    setSearching(true);
    try {
      const data = await searchCustomers(query);
      setResults(data);
      setSelectedCustomer(null); // Clear detail view on new search
    } catch (error) {
      console.error(error);
    } finally {
      setSearching(false);
    }
  };

  const handleSelectCustomer = async (id: number) => {
    setLoading(true);
    try {
      const data = await getCustomerDetails(id);
      setSelectedCustomer(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      key: 'fullName', header: 'Customer', render: (r: CustomerSearchResponse) => (
        <div>
          <div style={{ fontWeight: 600, color: '#1f2937' }}>{r.fullName}</div>
          <div style={{ fontSize: '13px', color: '#6b7280' }}>{r.email}</div>
        </div>
      )
    },
    { key: 'status', header: 'Status', render: (r: CustomerSearchResponse) => <StatusBadge status={r.status} /> },
    {
      key: 'actions', header: 'Actions', render: (r: CustomerSearchResponse) => (
        <button
          onClick={() => handleSelectCustomer(r.customerId)}
          style={{
            padding: '6px 12px',
            background: '#0f766e',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '12px',
            fontWeight: 600,
          }}
        >
          View Details
        </button>
      )
    },
  ];

  return (
    <>
      <PageHeader subtitle="Search for customers and view their billing details." />

      <div style={{ padding: '0 40px 40px 40px' }}>
        {/* Search Bar */}
        <form onSubmit={handleSearch} style={{ marginBottom: '24px', display: 'flex', gap: '12px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
            <input
              type="text"
              placeholder="Search by name or email..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '12px 16px 12px 48px',
                border: '1px solid #e2e8f0',
                borderRadius: '12px',
                fontSize: '14px',
                outline: 'none',
              }}
            />
          </div>
          <button
            type="submit"
            style={{
              padding: '12px 24px',
              background: '#0f766e',
              color: 'white',
              border: 'none',
              borderRadius: '12px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
            disabled={searching}
          >
            {searching ? 'Searching...' : 'Search'}
          </button>
        </form>

        <div style={{ display: 'grid', gridTemplateColumns: selectedCustomer ? '1fr 2fr' : '1fr', gap: '24px' }}>
          {/* Results Table */}
          <div className="data-panel" style={{ height: 'fit-content' }}>
            <DataTable columns={columns} data={results} emptyMessage="No customers found. Try a different search." />
          </div>

          {/* Detail View */}
          {selectedCustomer && (
            <div className="data-panel" style={{ padding: '24px' }}>
              {loading ? (
                <div>Loading details...</div>
              ) : (
                <div>
                  {/* Profile Header */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                    <div>
                      <h3 style={{ margin: 0, fontSize: '20px', fontWeight: 700, color: '#1f2937' }}>
                        {selectedCustomer.customerProfile.fullName}
                      </h3>
                      <div style={{ color: '#6b7280', fontSize: '14px' }}>Customer ID: {selectedCustomer.customerProfile.customerId}</div>
                    </div>
                    <StatusBadge status={selectedCustomer.customerProfile.status} />
                  </div>

                  {/* Tabs */}
                  <div style={{ display: 'flex', borderBottom: '1px solid #e2e8f0', marginBottom: '20px' }}>
                    {['subscriptions', 'invoices', 'usage', 'refunds'].map((tab) => (
                      <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        style={{
                          padding: '10px 20px',
                          background: 'transparent',
                          border: 'none',
                          borderBottom: activeTab === tab ? '2px solid #0f766e' : '2px solid transparent',
                          color: activeTab === tab ? '#0f766e' : '#64748b',
                          fontWeight: 600,
                          cursor: 'pointer',
                          textTransform: 'capitalize',
                        }}
                      >
                        {tab}
                      </button>
                    ))}
                  </div>

                  {/* Tab Content */}
                  {activeTab === 'subscriptions' && (
                    <div>
                      <h4 style={{ marginBottom: '12px' }}>Subscriptions</h4>
                      {selectedCustomer.subscriptions.length === 0 ? (
                        <p style={{ color: '#64748b' }}>No subscriptions found.</p>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {selectedCustomer.subscriptions.map((sub) => (
                            <div key={sub.subscriptionId} style={{ padding: '16px', border: '1px solid #e2e8f0', borderRadius: '8px' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ fontWeight: 600 }}>{sub.planName}</span>
                                <StatusBadge status={sub.status} />
                              </div>
                              <div style={{ fontSize: '13px', color: '#64748b', marginTop: '4px' }}>
                                Period: {sub.startDate} to {sub.currentPeriodEnd}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {activeTab === 'invoices' && (
                    <div>
                      <h4 style={{ marginBottom: '12px' }}>Invoices</h4>
                      {selectedCustomer.invoices.length === 0 ? (
                        <p style={{ color: '#64748b' }}>No invoices found.</p>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {selectedCustomer.invoices.map((inv) => (
                            <div key={inv.invoiceId} style={{ padding: '16px', border: '1px solid #e2e8f0', borderRadius: '8px', display: 'flex', justifyContent: 'space-between' }}>
                              <div>
                                <div style={{ fontWeight: 600 }}>{inv.invoiceNumber}</div>
                                <div style={{ fontSize: '13px', color: '#64748b' }}>Issued: {inv.issueDate}</div>
                              </div>
                              <div style={{ textAlign: 'right' }}>
                                <div style={{ fontWeight: 600 }}>{inv.currency} {(inv.totalMinor / 100).toFixed(2)}</div>
                                <StatusBadge status={inv.status} />
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {activeTab === 'usage' && (
                    <div>
                      <h4 style={{ marginBottom: '12px' }}>Metered Usage</h4>
                      {selectedCustomer.usageRecords.length === 0 ? (
                        <p style={{ color: '#64748b' }}>No usage records found.</p>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {selectedCustomer.usageRecords.map((ur) => (
                            <div key={ur.usageId} style={{ padding: '16px', border: '1px solid #e2e8f0', borderRadius: '8px', display: 'flex', justifyContent: 'space-between' }}>
                              <div>
                                <div style={{ fontWeight: 600 }}>{ur.componentName}</div>
                                <div style={{ fontSize: '13px', color: '#64748b' }}>Recorded: {ur.recordedAt}</div>
                              </div>
                              <div style={{ fontWeight: 600 }}>
                                {ur.quantity} {ur.unitName}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {activeTab === 'refunds' && (
                    <div>
                      <h4 style={{ marginBottom: '12px' }}>Refunds (Credit Notes)</h4>
                      {!selectedCustomer.creditNotes || selectedCustomer.creditNotes.length === 0 ? (
                        <p style={{ color: '#64748b' }}>No refunds found.</p>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {selectedCustomer.creditNotes.map((cn) => (
                            <div key={cn.creditNoteId} style={{ padding: '16px', border: '1px solid #e2e8f0', borderRadius: '8px', display: 'flex', justifyContent: 'space-between' }}>
                              <div>
                                <div style={{ fontWeight: 600 }}>{cn.creditNoteNumber}</div>
                                <div style={{ fontSize: '13px', color: '#64748b' }}>Reason: {cn.reason}</div>
                                <div style={{ fontSize: '13px', color: '#64748b' }}>Date: {cn.createdAt}</div>
                              </div>
                              <div style={{ textAlign: 'right' }}>
                                <div style={{ fontWeight: 600 }}>{selectedCustomer.customerProfile.currency} {(cn.amountMinor / 100).toFixed(2)}</div>
                                <StatusBadge status={cn.status} />
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  );
};
