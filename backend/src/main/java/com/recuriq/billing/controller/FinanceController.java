package com.recuriq.billing.controller;

import com.recuriq.billing.dto.finance.*;
import com.recuriq.billing.service.ReportExportService;
import com.recuriq.billing.service.RevenueAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FINANCE')")
public class FinanceController {

    private final RevenueAnalyticsService revenueAnalyticsService;
    private final ReportExportService reportExportService;

    /**
     * 1. Dashboard
     * GET /api/finance/dashboard
     * Returns: MRR, ARR, ARPU, LTV, net churn %, active customers,
     *          revenue by plan (MRR), revenue by region (MRR),
     *          revenue trend (month, year, MRR for every snapshot month)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FinanceDashboardDTO> getDashboard() {
        return ResponseEntity.ok(revenueAnalyticsService.getFinanceDashboard());
    }

    /**
     * 2. MRR Report
     * GET /api/finance/reports/mrr
     * Returns: current MRR value,
     *          revenue trend (month, year, MRR for every snapshot month)
     */
    @GetMapping("/reports/mrr")
    public ResponseEntity<MrrReportDTO> getMrrReport() {
        return ResponseEntity.ok(revenueAnalyticsService.getMrrReport());
    }

    /**
     * 3. ARR Report
     * GET /api/finance/reports/arr
     * Returns: current ARR value,
     *          revenue by plan (ARR), revenue by region (ARR)
     */
    @GetMapping("/reports/arr")
    public ResponseEntity<ArrReportDTO> getArrReport() {
        return ResponseEntity.ok(revenueAnalyticsService.getArrReport());
    }

    /**
     * 4. Churn Report
     * GET /api/finance/reports/churn
     * Returns: net churn %, revenue churn %, churned revenue (INR paise),
     *          month-by-month churn trend
     */
    @GetMapping("/reports/churn")
    public ResponseEntity<ChurnReportDTO> getChurnReport() {
        return ResponseEntity.ok(revenueAnalyticsService.getChurnReport());
    }

    /**
     * 5. ARPU & LTV Report
     * GET /api/finance/reports/arpu-ltv
     * Returns: current ARPU, current LTV,
     *          ARPU trend (month, year, ARPU for every snapshot month)
     */
    @GetMapping("/reports/arpu-ltv")
    public ResponseEntity<ArpuLtvReportDTO> getArpuLtvReport() {
        return ResponseEntity.ok(revenueAnalyticsService.getArpuLtvReport());
    }

    /**
     * 6. Export
     * GET /api/finance/reports/export?format=csv   → CSV download
     * GET /api/finance/reports/export?format=pdf   → PDF download
     * Exports the full financial dashboard report including KPIs,
     * plan breakdown, region breakdown, and monthly trend.
     */
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "csv") String format) {

        FinanceDashboardDTO dashboard = revenueAnalyticsService.getFinanceDashboard();

        byte[] report;
        MediaType contentType;
        String filename;

        if ("pdf".equalsIgnoreCase(format)) {
            report      = reportExportService.exportDashboardAsPdf(dashboard);
            contentType = MediaType.APPLICATION_PDF;
            filename    = "financial_report.pdf";
        } else {
            report      = reportExportService.exportDashboardAsCsv(dashboard);
            contentType = MediaType.parseMediaType("text/csv");
            filename    = "financial_report.csv";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(report);
    }

    /**
     * 7. Invoices
     * GET /api/finance/invoices
     * Returns from invoice table:
     *   invoiceNumber  → INV-{year}-{invoice_id}
     *   customerId     → customer_id
     *   amount         → total_minor converted to INR rupees
     *   date           → created_at
     *   dueDate        → due_date
     *   status         → DRAFT / OPEN / PAID / VOID / UNCOLLECTIBLE
     */
    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceRecordDTO>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueAnalyticsService.getAllInvoiceRecords(PageRequest.of(page, size)));
    }

    /**
     * 7.1. Invoice Detail with Line Items
     * GET /api/finance/invoices/{invoiceId}
     * Returns invoice details including line items:
     *   invoiceNumber, customerId, amount, date, dueDate, status, lineItems[]
     */
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDetailDTO> getInvoiceDetail(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(revenueAnalyticsService.getInvoiceDetailById(invoiceId));
    }

    /**
     * 8. Payments
     * GET /api/finance/payments
     * Returns from payment table:
     *   paymentId      → payment_id
     *   invoiceNumber  → INV-{year}-{invoice_id} via invoice FK
     *   amount         → amount_minor converted to INR rupees
     *   paymentMethod  → "CARD ****1234" or "UPI user@upi" from payment_method table
     *   date           → created_at
     *   status         → PENDING / SUCCESS / FAILED / REFUNDED / PARTIALLY_REFUNDED
     */
    @GetMapping("/payments")
    public ResponseEntity<Page<PaymentRecordDTO>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueAnalyticsService.getAllPaymentRecords(PageRequest.of(page, size)));
    }

    /**
     * 9. Refunds & Credits
     * GET /api/finance/refunds
     * Returns from credit_note table:
     *   refundId       → REF-{credit_note_id}
     *   paymentId      → most recent payment_id on the linked invoice
     *   amount         → amount_minor converted to INR rupees
     *   reason         → reason (free text)
     *   date           → created_at
     *   status         → DRAFT / ISSUED / APPLIED / VOIDED
     */
    @GetMapping("/refunds")
    public ResponseEntity<Page<RefundCreditDTO>> getRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueAnalyticsService.getAllRefundCredits(PageRequest.of(page, size)));
    }

    /**
     * 10. Revenue Snapshots
     * GET /api/finance/snapshots
     * Returns from revenue_snapshot table:
     *   date              → snapshot_date
     *   totalRevenueMinor → total_revenue_minor
     *   mrrMinor          → mrr_minor
     *   arrMinor          → arr_minor
     *   activeCustomers   → active_customers
     *   newCustomers      → new_customers
     *   netChurnPercent   → net_churn_percent
     */
    @GetMapping("/snapshots")
    public ResponseEntity<Page<RevenueSnapshotDTO>> getSnapshots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(revenueAnalyticsService.getAllRevenueSnapshots(PageRequest.of(page, size)));
    }
}
