import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CustomerAuthPage } from './pages/auth/CustomerAuthPage';
import { ManagementAuthPage } from './pages/auth/ManagementAuthPage';
import { LandingPage } from './pages/public/LandingPage';
import { PlansPage } from './pages/public/PlansPage';
import { RoleGuard } from './routes/RoleGuard';
import { ROLES } from './constants/roles';

// Admin Layout & Pages
import { AdminLayout } from './components/admin/layout/AdminLayout';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { ProductPage } from './pages/admin/catalog/ProductPage';
import { PlansPage as AdminPlansPage } from './pages/admin/catalog/PlansPage';
import { AddOnsPage } from './pages/admin/catalog/AddOnsPage';
import { MeteredComponentsPage } from './pages/admin/catalog/MeteredComponentsPage';
import { PriceBooksPage } from './pages/admin/pricing/PriceBooksPage';
import { TaxRatesPage } from './pages/admin/pricing/TaxRatesPage';
import { CouponsPage } from './pages/admin/pricing/CouponsPage';
import { CustomersPage } from './pages/admin/users/CustomersPage';
import { StaffAccountsPage } from './pages/admin/users/StaffAccountsPage';
import { SubscriptionsPage } from './pages/admin/subscriptions/SubscriptionsPage';

// Customer Dashboard Layout & Pages
import { CustomerLayout } from './components/customer/layout/CustomerLayout';
import { OverviewPage } from './pages/customer/OverviewPage';
import { SubscriptionPage } from './pages/customer/SubscriptionPage';
import { BillingPage } from './pages/customer/BillingPage';
import { PaymentMethodsPage } from './pages/customer/PaymentMethodsPage';
import { SupportPage } from './pages/customer/SupportPage';
import { ProfilePage } from './pages/customer/ProfilePage';
import { SubscriptionFlow } from './pages/customer/SubscriptionFlow';
import { SubscriptionCheckoutPage } from './pages/customer/SubscriptionCheckoutPage';
import { CustomerProvider } from './context/CustomerContext';

// Finance Layout & Pages
import { FinanceLayout } from './components/finance/layout/FinanceLayout';
import { FinanceDashboard } from './pages/finance/FinanceDashboard';
import { MRRAnalyticsPage } from './pages/finance/mrr';
import { ARRAnalyticsPage } from './pages/finance/arr';
import { ChurnAnalyticsPage } from './pages/finance/churn';
import { ARPUAnalyticsPage } from './pages/finance/arpuLtv';
import { InvoicesPage } from './pages/finance/invoices';
import { PaymentsPage } from './pages/finance/payments';
import { CreditsPage } from './pages/finance/creditnotes';
import { SnapshotsPage } from './pages/finance/revenueSnapshots';
import { AiAssistantPage } from './pages/finance/AiAssistantPage';
import { ChurnRiskPage } from './pages/shared/ChurnRiskPage';

// Support Layout & Pages
import { SupportLayout } from './components/support/layout/SupportLayout';
import { SupportDashboardPage } from './pages/support/SupportDashboardPage';
import { AuditLogsPage } from './pages/support/AuditLogsPage';
import { SystemStatusPage } from './pages/support/SystemStatusPage';
import { PastDueSubscriptionsPage } from './pages/support/PastDueSubscriptionsPage';
import { CancellationRequestsPage } from './pages/support/CancellationRequestsPage';


function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Landing Page - Now inside AuthProvider */}
          <Route path="/" element={<LandingPage />} />
          
          {/* Public Auth Routes */}
          <Route path="/login" element={<CustomerAuthPage />} />
          <Route path="/register" element={<CustomerAuthPage />} />
          <Route path="/management/login" element={<ManagementAuthPage />} />

          {/* Protected Routes - Subscription Flow */}
          <Route
            path="/plans"
            element={
              <RoleGuard allowedRoles={[ROLES.CUSTOMER]} redirectTo="/register">
                <PlansPage />
              </RoleGuard>
            }
          />
          <Route
            path="/subscribe"
            element={
              <RoleGuard allowedRoles={[ROLES.CUSTOMER]} redirectTo="/register">
                <SubscriptionFlow />
              </RoleGuard>
            }
          />
          <Route
            path="/checkout"
            element={
              <RoleGuard allowedRoles={[ROLES.CUSTOMER]} redirectTo="/register">
                <SubscriptionCheckoutPage />
              </RoleGuard>
            }
          />

          {/* Customer Protected Routes — Nested under CustomerLayout */}
          <Route
            path="/dashboard"
            element={
              <RoleGuard allowedRoles={[ROLES.CUSTOMER]} redirectTo="/login">
                <CustomerProvider>
                  <CustomerLayout />
                </CustomerProvider>
              </RoleGuard>
            }
          >
            <Route index element={<OverviewPage />} />
            <Route path="subscription" element={<SubscriptionPage />} />
            <Route path="billing" element={<BillingPage />} />
            <Route path="payment-methods" element={<PaymentMethodsPage />} />
            <Route path="support" element={<SupportPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>

          {/* Admin Protected Routes — Nested under AdminLayout */}
          <Route
            path="/admin"
            element={
              <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/management/login">
                <AdminLayout />
              </RoleGuard>
            }
          >
            <Route index element={<AdminDashboardPage />} />
            <Route path="product" element={<ProductPage />} />
            <Route path="plans" element={<AdminPlansPage />} />
            <Route path="addons" element={<AddOnsPage />} />
            <Route path="metered" element={<MeteredComponentsPage />} />
            <Route path="pricebooks" element={<PriceBooksPage />} />
            <Route path="taxrates" element={<TaxRatesPage />} />
            <Route path="coupons" element={<CouponsPage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="staff" element={<StaffAccountsPage />} />
            <Route path="subscriptions" element={<SubscriptionsPage />} />
          </Route>

          {/* Finance Protected Routes — Nested under FinanceLayout */}
          <Route
            path="/finance"
            element={
              <RoleGuard allowedRoles={[ROLES.FINANCE]} redirectTo="/management/login">
                <FinanceLayout />
              </RoleGuard>
            }
          >
            <Route index element={<FinanceDashboard />} />
            <Route path="mrr" element={<MRRAnalyticsPage />} />
            <Route path="arr" element={<ARRAnalyticsPage />} />
            <Route path="churn" element={<ChurnAnalyticsPage />} />
            <Route path="arpu" element={<ARPUAnalyticsPage />} />
            <Route path="invoices" element={<InvoicesPage />} />
            <Route path="payments" element={<PaymentsPage />} />
            <Route path="credits" element={<CreditsPage />} />
            <Route path="snapshots" element={<SnapshotsPage />} />
            <Route path="assistant" element={<AiAssistantPage />} />
            <Route path="at-risk" element={<ChurnRiskPage />} />
          </Route>

          {/* Support Protected Routes — Nested under SupportLayout */}
          <Route
            path="/support"
            element={
              <RoleGuard allowedRoles={[ROLES.SUPPORT]} redirectTo="/management/login">
                <SupportLayout />
              </RoleGuard>
            }
          >
            <Route index element={<SupportDashboardPage />} />
            <Route path="cancellation-requests" element={<CancellationRequestsPage />} />
            <Route path="audit-logs" element={<AuditLogsPage />} />
            <Route path="status" element={<SystemStatusPage />} />
            <Route path="past-due" element={<PastDueSubscriptionsPage />} />
            <Route path="at-risk" element={<ChurnRiskPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
