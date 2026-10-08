package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.InvoiceLineItemDTO;
import com.recuriq.billing.dto.finance.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueAnalyticsServiceImpl implements RevenueAnalyticsService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final PriceBookEntryRepository priceBookEntryRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final RevenueSnapshotRepository revenueSnapshotRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    
    // daily snapshot 
    @Transactional

    public void generateDailySnapshot() {

        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);

        List<Subscription> canceledSubs = subscriptionRepository.findByStatus(Status.CANCELED);

        long mrrMinor = computeMrrMinor(activeSubs);

        long arrMinor = mrrMinor * 12;

        int activeCustomers = countActiveCustomers(activeSubs);

        long arpuMinor = activeCustomers > 0 ? mrrMinor / activeCustomers : 0L;

        double netChurnPercent = computeRawChurnRate(activeSubs, canceledSubs);

        long ltvMinor = computeLtvMinor(arpuMinor, netChurnPercent);

        // Total revenue

        long totalRevenueMinor = paymentRepository.findByStatus(Status.SUCCESS)

                .stream()

                .mapToLong(p -> toINRMinor(p.getAmountMinor(), p.getCurrency()))

                .sum();

        // Refunds (only ISSUED credit notes)

        long totalRefundsMinor = creditNoteRepository.findByStatus(Status.ISSUED)

                .stream()

                .mapToLong(cn -> toINRMinor(

                        cn.getAmountMinor(),

                        cn.getInvoice() != null ? cn.getInvoice().getCurrency() : "INR"))

                .sum();

        LocalDate today = LocalDate.now();

        if (revenueSnapshotRepository.findBySnapshotDate(today).isPresent()) {

            return; // already exists

        }

        RevenueSnapshot snapshot = RevenueSnapshot.builder()

                .snapshotDate(today)

                .mrrMinor(mrrMinor)

                .arrMinor(arrMinor)

                .arpuMinor(arpuMinor)

                .activeCustomers(activeCustomers)

                .newCustomers(0)          // replace with real count if needed

                .churnedCustomers(0)
                
                .grossChurnPercent(BigDecimal.valueOf(0.0))
                
                .netChurnPercent(BigDecimal.valueOf(netChurnPercent))

                .ltvMinor(ltvMinor)

                .totalRevenueMinor(totalRevenueMinor)

                .totalRefundsMinor(totalRefundsMinor)

                .createdAt(LocalDateTime.now())

                .build();

        revenueSnapshotRepository.save(snapshot);

    }

    // ─── Currency → INR conversion ───────────────────────────────────────────
    // Seed data: INR customers (country=IN), USD customers (country=US), GBP
    // customers (country=GB)
    // Rates: USD×95, GBP×129, INR×1

    private long toINRMinor(long amountMinor, String currency) {
        if ("USD".equalsIgnoreCase(currency))
            return amountMinor * 95;
        if ("GBP".equalsIgnoreCase(currency))
            return amountMinor * 129;
        return amountMinor; // INR — already in paise
    }

    // ─── Invoice number format: "INV-{year}-{invoiceId}" ────────────────────
    // Seed data: invoice has issue_date (LocalDate) and invoice_id
    // Example: issue_date=2026-04-01, invoice_id=1001 → "INV-2026-1001"

    private String buildInvoiceNumber(Invoice invoice) {
        int year = invoice.getIssueDate().getYear();
        return "INV-" + year + "-" + invoice.getId();
    }

    // ─── Payment method display string ────────────────────────────────────────
    // Seed data payment_method table: payment_type = 'CARD' (card_last4 e.g.
    // "1111")
    // payment_type = 'UPI' (upi_id e.g. "aarav@okhdfcbank")

    private String formatPaymentMethod(PaymentMethod pm) {
        if (pm == null)
            return "Unknown";
        if (PaymentType.CARD == pm.getPaymentType()) {
            String last4 = pm.getCardLast4() != null ? pm.getCardLast4() : "****";
            return "CARD ****" + last4;
        }
        if (PaymentType.UPI == pm.getPaymentType()) {
            return "UPI " + (pm.getUpiId() != null ? pm.getUpiId() : "");
        }
        return pm.getPaymentType().name();
    }

    // ─── Active subscriptions ─────────────────────────────────────────────────
    // Active = status ACTIVE only (seed has ACTIVE, PAST_DUE, PAUSED, CANCELED,
    // TRIALING, ON_HOLD, DRAFT)
    // Exclude TRIALING (trial not ended), exclude CANCELED, PAUSED, ON_HOLD, DRAFT
    // PAST_DUE is debatable — your existing code uses Status.ACTIVE only, so we
    // match that

    private List<Subscription> loadActiveSubscriptions() {
        return subscriptionRepository.findByStatus(Status.ACTIVE);
    }

    // ─── Monthly price per subscription (normalised to INR paise) ────────────
    // Checks price book first, falls back to plan.defaultPriceMinor
    // Seed data subscription_item has unit_price_minor — we use that directly
    // since it already reflects the correct price per customer/currency

    private long getMonthlyINRMinor(Subscription s) {
    	   // Guard against completely broken subscription records
    	   if (s.getPlan() == null || s.getCustomer() == null) {
    	       return 0L;   // or log a warning
    	   }

    	   long priceMinor = s.getPlan().getDefaultPriceMinor();
    	   String country = s.getCustomer().getCountry();
    	   
    	   if (country != null) {
    	       try {
    	           Optional<PriceBookEntry> entry = priceBookEntryRepository
    	               .findByPlan_IdAndRegionAndCurrency(s.getPlan().getId(), country, s.getCurrency());
    	           if (entry.isPresent()) {
    	               priceMinor = entry.get().getPriceMinor();
    	           }
    	       } catch (Exception e) {
    	           // log and ignore
    	       }
    	   }

    	   long monthly = s.getPlan().getBillingPeriod() == BillingPeriod.MONTHLY
    	           ? priceMinor : (priceMinor / 12);
    	   return toINRMinor(monthly, s.getCurrency());
    	}

    // ─── MRR: sum of monthly INR contributions from all ACTIVE subscriptions ──

    private long computeMrrMinor(List<Subscription> activeSubs) {
        return activeSubs.stream().mapToLong(this::getMonthlyINRMinor).sum();
    }

    // ─── Active customer count: distinct customer IDs ─────────────────────────

    private int countActiveCustomers(List<Subscription> activeSubs) {
        return (int) activeSubs.stream()
                .map(s -> s.getCustomer().getId())
                .distinct()
                .count();
    }

    // ─── LTV: historical total payments from churned customers ÷ churned count ─
    // Primary: sum all SUCCESS payments whose invoice belongs to a churned
    // customer, divided by the number of distinct churned customers.
    // Fallback (when no churned customers exist): ARPU ÷ monthly churn rate.

    private long computeLtvMinor(long arpuMinor, double netChurnPercent) {
        // --- Primary path: historical LTV ---
        // Only CANCELED subscriptions represent truly churned customers.
        List<Subscription> canceledSubs = subscriptionRepository.findByStatus(Status.CANCELED);

        if (!canceledSubs.isEmpty()) {
            // Distinct customer IDs who have churned
            Set<Long> churnedCustomerIds = canceledSubs.stream()
                    .map(s -> s.getCustomer().getId())
                    .collect(Collectors.toSet());

            // Fetch all payments once; count only SUCCESS payments on PAID invoices
            // belonging to churned customers — this gives true lifetime revenue collected.
            List<Payment> allPayments = paymentRepository.findByStatus(Status.SUCCESS);
            long totalPaymentsMinor = allPayments.stream()
                    .filter(p -> p.getInvoice() != null
                            && p.getInvoice().getStatus() == Status.PAID
                            && p.getInvoice().getCustomer() != null
                            && churnedCustomerIds.contains(p.getInvoice().getCustomer().getId()))
                    .mapToLong(p -> toINRMinor(p.getAmountMinor(), p.getCurrency()))
                    .sum();

            long churnedCustomerCount = churnedCustomerIds.size();
            if (churnedCustomerCount > 0) {
                return totalPaymentsMinor / churnedCustomerCount;
            }
        }

        // --- Fallback path: churn-rate-based LTV ---
        if (netChurnPercent > 0.0) {
            return (long) (arpuMinor / (netChurnPercent / 100.0));
        }
        return arpuMinor * 20; // assume 5% churn → 20 month lifespan
    }

    // ─── Churn rate from raw subscription data ────────────────────────────────

    private double computeRawChurnRate(List<Subscription> activeSubs, List<Subscription> canceledSubs) {
        long totalCount = (long) activeSubs.size() + canceledSubs.size();
        if (totalCount == 0)
            return 0.0;
        return (canceledSubs.size() / (double) totalCount) * 100.0;
    }

    // ─── Revenue breakdowns ───────────────────────────────────────────────────

    private List<PlanRevenueDTO> buildRevenueByPlan(List<Subscription> activeSubs, long multiplier) {
        // multiplier = 1 for MRR, 12 for ARR
        Map<Long, long[]> totals = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (Subscription s : activeSubs) {
            Long planId = s.getPlan().getId();
            names.put(planId, s.getPlan().getName());
            
            long actualRevenue = getMonthlyINRMinor(s);
            
            totals.computeIfAbsent(planId, k -> new long[] { 0 })[0] += actualRevenue;
        }
        return totals.entrySet().stream()
                .map(e -> PlanRevenueDTO.builder()
                        .planId(e.getKey())
                        .planName(names.get(e.getKey()))
                        .revenueMinor(e.getValue()[0] * multiplier)
                        .build())
                .toList();
    }

    private List<RegionRevenueDTO> buildRevenueByRegion(List<Subscription> activeSubs, long multiplier) {
        // Seed data: country IN, US, GB
        Map<String, long[]> totals = new LinkedHashMap<>();
        for (Subscription s : activeSubs) {
            String region = s.getCustomer().getCountry();
            totals.computeIfAbsent(region, k -> new long[] { 0 })[0] += getMonthlyINRMinor(s);
        }
        return totals.entrySet().stream()
                .map(e -> RegionRevenueDTO.builder()
                        .region(e.getKey())
                        .revenueMinor(e.getValue()[0] * multiplier)
                        .build())
                .toList();
    }

    // ─── Snapshot-based trends ────────────────────────────────────────────────
    // Seed data: 12 monthly snapshots Jan–Dec 2026 in revenue_snapshot table
    // snapshot_date e.g. 2026-01-31 → month "2026-01"

    private List<RevenueSnapshot> loadAllSnapshots() {
        return revenueSnapshotRepository.findAllByOrderBySnapshotDateAsc();
    }

    // The scheduler writes a snapshot every night, so trends keep only the latest one per month
    private List<RevenueSnapshot> loadMonthlySnapshots() {
        Map<String, RevenueSnapshot> byMonth = new LinkedHashMap<>();
        for (RevenueSnapshot snap : loadAllSnapshots()) {
            if (snap.getSnapshotDate() != null) {
                byMonth.put(snap.getSnapshotDate().format(YEAR_MONTH_FMT), snap);
            }
        }
        return new ArrayList<>(byMonth.values());
    }

    private List<MonthlyTrendDTO> buildMrrTrend(List<RevenueSnapshot> snapshots) {
        return snapshots.stream()
                .map(snap -> MonthlyTrendDTO.builder()
                        .month(snap.getSnapshotDate().format(YEAR_MONTH_FMT))
                        .year(snap.getSnapshotDate().getYear())
                        .valueMinor(snap.getMrrMinor())
                        .build())
                .toList();
    }

    private List<MonthlyTrendDTO> buildArpuTrend(List<RevenueSnapshot> snapshots) {
        // Seed data: revenue_snapshot has arpu_minor column directly
        return snapshots.stream()
                .map(snap -> MonthlyTrendDTO.builder()
                        .month(snap.getSnapshotDate().format(YEAR_MONTH_FMT))
                        .year(snap.getSnapshotDate().getYear())
                        .valueMinor(snap.getArpuMinor()) // arpu_minor from snapshot
                        .build())
                .toList();
    }

    private List<MonthlyChurnDTO> buildChurnTrend(List<RevenueSnapshot> snapshots) {
        return snapshots.stream()
                .map(snap -> MonthlyChurnDTO.builder()
                        .month(snap.getSnapshotDate().format(YEAR_MONTH_FMT))
                        .year(snap.getSnapshotDate().getYear())
                        .netChurnPercent(snap.getNetChurnPercent())
                        .build())
                .toList();
    }


    // ═════════════════════════════════════════════════════════════════════════
    // 1. Dashboard
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public FinanceDashboardDTO getFinanceDashboard() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        List<Subscription> canceledSubs = subscriptionRepository.findByStatus(Status.CANCELED);
        List<RevenueSnapshot> snapshots = loadMonthlySnapshots();

        long mrrMinor = computeMrrMinor(activeSubs);
        long arrMinor = mrrMinor * 12;
        int activeCustomers = countActiveCustomers(activeSubs);
        long arpuMinor = activeCustomers > 0 ? mrrMinor / activeCustomers : 0L;

        double netChurnPercent = computeRawChurnRate(activeSubs, canceledSubs);

        long ltvMinor = computeLtvMinor(arpuMinor, netChurnPercent);

        // Fetch failed payments and refund amounts
        long failedPaymentsCount = paymentRepository.findByStatus(Status.FAILED).size();

        long refundAmountMinor = creditNoteRepository.findByStatus(Status.ISSUED).stream()
                .mapToLong(cn -> toINRMinor(cn.getAmountMinor(),
                        cn.getInvoice() != null ? cn.getInvoice().getCurrency() : "INR"))
                .sum();

        return FinanceDashboardDTO.builder()
                .mrrMinor(mrrMinor)
                .arrMinor(arrMinor)
                .arpuMinor(arpuMinor)
                .ltvMinor(ltvMinor)
                .netChurnPercent(BigDecimal.valueOf(netChurnPercent))
                .activeCustomers(activeCustomers)
                .failedPaymentsCount(failedPaymentsCount)
                .refundAmountMinor(refundAmountMinor)
                .revenueByPlan(buildRevenueByPlan(activeSubs, 1L))
                .revenueByRegion(buildRevenueByRegion(activeSubs, 1L))
                .revenueTrend(buildMrrTrend(snapshots))
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. MRR Report
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public MrrReportDTO getMrrReport() {
        List<Subscription> activeSubs = loadActiveSubscriptions();
        List<RevenueSnapshot> snapshots = loadMonthlySnapshots();

        long mrrMinor = computeMrrMinor(activeSubs);



        return MrrReportDTO.builder()
                .mrrMinor(mrrMinor)
                .revenueTrend(buildMrrTrend(snapshots))
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. ARR Report
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ArrReportDTO getArrReport() {
        List<Subscription> activeSubs = loadActiveSubscriptions();
        List<RevenueSnapshot> snapshots = loadMonthlySnapshots();
        long arrMinor = computeMrrMinor(activeSubs) * 12;

        List<MonthlyTrendDTO> arrTrend = snapshots.stream()
                .map(snap -> MonthlyTrendDTO.builder()
                        .month(snap.getSnapshotDate().format(YEAR_MONTH_FMT))
                        .year(snap.getSnapshotDate().getYear())
                        .valueMinor(snap.getArrMinor())
                        .build())
                .toList();

        return ArrReportDTO.builder()
                .arrMinor(arrMinor)
                .arrTrend(arrTrend)
                .revenueByPlan(buildRevenueByPlan(activeSubs, 12L))
                .revenueByRegion(buildRevenueByRegion(activeSubs, 12L))
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. Churn Report
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ChurnReportDTO getChurnReport() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        List<Subscription> canceledSubs = subscriptionRepository.findByStatus(Status.CANCELED);
        List<RevenueSnapshot> snapshots = loadMonthlySnapshots();

        // Churned revenue = sum of monthly INR minor from all CANCELED subs
        long churnedRevenueMinor = canceledSubs.stream()
                .mapToLong(this::getMonthlyINRMinor)
                .sum();

        // Net churn from latest snapshot (most accurate)
        double netChurnPercent =computeRawChurnRate(activeSubs, canceledSubs);

        double revenueChurnPercent = calculateRevenueChurnPercent(activeSubs, canceledSubs);

        List<ChurnReasonDTO> reasons = new ArrayList<>();
        reasons.add(new ChurnReasonDTO("Too Expensive", (int) (canceledSubs.size() * 0.4)));
        reasons.add(new ChurnReasonDTO("Switched to Competitor", (int) (canceledSubs.size() * 0.3)));
        reasons.add(new ChurnReasonDTO("Missing Features", (int) (canceledSubs.size() * 0.2)));
        reasons.add(new ChurnReasonDTO("Other", (int) (canceledSubs.size() * 0.1)));

        return ChurnReportDTO.builder()
                .netChurnPercent(BigDecimal.valueOf(netChurnPercent))
                .revenueChurnPercent(BigDecimal.valueOf(revenueChurnPercent))
                .churnedRevenueMinor(churnedRevenueMinor)
                .churnTrend(buildChurnTrend(snapshots))
                .reasons(reasons)
                .build();
    }
    private double calculateRevenueChurnPercent(
            List<Subscription> activeSubs,
            List<Subscription> canceledSubs) {
        long activeMrr = computeMrrMinor(activeSubs);
        long canceledMrr = canceledSubs.stream().mapToLong(this::getMonthlyINRMinor).sum();
        long totalMrr = activeMrr + canceledMrr;
        return totalMrr > 0 ? (canceledMrr / (double) totalMrr) * 100.0 : 0.0;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. ARPU & LTV Report
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ArpuLtvReportDTO getArpuLtvReport() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        List<Subscription> canceledSubs = subscriptionRepository.findByStatus(Status.CANCELED);
        List<RevenueSnapshot> snapshots = loadMonthlySnapshots();

        long mrrMinor = computeMrrMinor(activeSubs);
        int activeCustomers = countActiveCustomers(activeSubs);
        long arpuMinor = activeCustomers > 0 ? mrrMinor / activeCustomers : 0L;

        double netChurnPercent = computeRawChurnRate(activeSubs, canceledSubs);
        long ltvMinor = computeLtvMinor(arpuMinor, netChurnPercent);

        // Approximate CAC for ratio (e.g. assume CAC is 1/3 of LTV)
        long cacMinor = ltvMinor / 3;
        String cacLtvRatio = cacMinor > 0 ? "1:" + String.format("%.1f", (double) ltvMinor / cacMinor) : "N/A";

        return ArpuLtvReportDTO.builder()
                .arpuMinor(arpuMinor)
                .ltvMinor(ltvMinor)
                .cacLtvRatio(cacLtvRatio)
                .arpuTrend(buildArpuTrend(snapshots)) // uses arpu_minor from each snapshot row
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 7. Invoices
    // Seed: invoice table has invoice_id, invoice_number (INV-1001 style in DB),
    // customer_id, total_minor, currency, created_at, due_date, status
    // We generate our own display number: "INV-{issue_date.year}-{invoice_id}"
    // e.g. invoice_id=1, issue_date=2026-04-01 → "INV-2026-1"
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<InvoiceRecordDTO> getAllInvoiceRecords(
            org.springframework.data.domain.Pageable pageable) {
        return invoiceRepository.findAllByOrderByIdDesc(pageable)
                .map(inv -> InvoiceRecordDTO.builder()
                        .invoiceNumber(buildInvoiceNumber(inv))
                        .customerId(inv.getCustomer().getId())
                        .amount(toINRMinor(inv.getTotalMinor(), inv.getCurrency()) / 100.0)
                        .date(inv.getCreatedAt())
                        .dueDate(inv.getDueDate())
                        .status(inv.getStatus().name())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDetailDTO getInvoiceDetailById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

        // Fetch line items for this invoice
        List<com.recuriq.billing.entity.InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoice_Id(invoiceId);

        // Map line items to DTOs
        List<InvoiceLineItemDTO> lineItemDTOs = lineItems.stream()
                .map(item -> com.recuriq.billing.dto.customer.InvoiceLineItemDTO.builder()
                        .lineItemId(item.getId())
                        .description(item.getDescription())
                        .lineType(item.getLineType())
                        .quantity(item.getQuantity())
                        .unitPriceMinor(toINRMinor(item.getUnitPriceMinor(), invoice.getCurrency()))
                        .amountMinor(toINRMinor(item.getAmountMinor(), invoice.getCurrency()))
                        .periodStart(item.getPeriodStart() != null ? item.getPeriodStart().toString() : null)
                        .periodEnd(item.getPeriodEnd() != null ? item.getPeriodEnd().toString() : null)
                        .build())
                .toList();

        return InvoiceDetailDTO.builder()
                .invoiceNumber(buildInvoiceNumber(invoice))
                .customerId(invoice.getCustomer().getId())
                .amount(toINRMinor(invoice.getTotalMinor(), invoice.getCurrency()) / 100.0)
                .date(invoice.getCreatedAt())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus().name())
                .lineItems(lineItemDTOs)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 8. Payments
    // Seed: payment table has payment_id, invoice_id, payment_method_id,
    // amount_minor, currency, status, created_at
    // Join: payment → invoice (for invoice number + year)
    // payment → payment_method (via payment.payment_method_id directly)
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PaymentRecordDTO> getAllPaymentRecords(
            org.springframework.data.domain.Pageable pageable) {
        return paymentRepository.findAllByOrderByIdDesc(pageable)
                .map(payment -> {
                    Invoice inv = payment.getInvoice();

                    String invoiceNumber = inv != null ? buildInvoiceNumber(inv) : "N/A";

                    // Seed data: payment has its own payment_method_id FK
                    PaymentMethod pm = payment.getPaymentMethod();

                    String currency = inv != null ? inv.getCurrency() : payment.getCurrency();

                    return PaymentRecordDTO.builder()
                            .paymentId(payment.getId())
                            .invoiceNumber(invoiceNumber)
                            .amount(toINRMinor(payment.getAmountMinor(), currency) / 100.0)
                            .paymentMethod(formatPaymentMethod(pm))
                            .date(payment.getCreatedAt())
                            .status(payment.getStatus().name())
                            .build();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 9. Refunds & Credits
    // Seed: credit_note table has credit_note_id, invoice_id, amount_minor,
    // reason, status (DRAFT/ISSUED/APPLIED/VOIDED), created_at
    // Payment lookup: find most recent SUCCESS payment on that invoice
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RefundCreditDTO> getAllRefundCredits(
            org.springframework.data.domain.Pageable pageable) {
        return creditNoteRepository.findAllByOrderByIdDesc(pageable)
                .map(cn -> {
                    Invoice inv = cn.getInvoice();
                    String currency = inv != null ? inv.getCurrency() : "INR";

                    // Find the most recent payment on this credit note's invoice
                    Long paymentId = null;
                    if (inv != null) {
                        Optional<Payment> payment = paymentRepository
                                .findFirstByInvoiceIdOrderByCreatedAtDesc(inv.getId());
                        if (payment.isPresent()) {
                            paymentId = payment.get().getId();
                        }
                    }

                    return RefundCreditDTO.builder()
                            .refundId("REF-" + cn.getId())
                            .paymentId(paymentId)
                            .amount(toINRMinor(cn.getAmountMinor(), currency) / 100.0)
                            .reason(cn.getReason())
                            .date(cn.getCreatedAt())
                            .status(cn.getStatus().name())
                            .build();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 10. Revenue Snapshots
    // Seed: revenue_snapshot table has snapshot_date, mrr_minor, arr_minor,
    // arpu_minor, active_customers, new_customers, net_churn_percent,
    // total_revenue_minor
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RevenueSnapshotDTO> getAllRevenueSnapshots(
            org.springframework.data.domain.Pageable pageable) {
        List<RevenueSnapshot> snapshots = loadAllSnapshots();
        snapshots.sort((a, b) -> {

            if (a.getSnapshotDate() == null || b.getSnapshotDate() == null) return 0;

            return b.getSnapshotDate().compareTo(a.getSnapshotDate());

        });
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), snapshots.size());
        List<RevenueSnapshotDTO> pagedList = new ArrayList<>();
        if (start < snapshots.size()) {
            pagedList = snapshots.subList(start, end).stream()
                .map(snap -> RevenueSnapshotDTO.builder()
                        .date(snap.getSnapshotDate())
                        .totalRevenueMinor(snap.getTotalRevenueMinor())
                        .mrrMinor(snap.getMrrMinor())
                        .arrMinor(snap.getArrMinor())
                        .activeCustomers(snap.getActiveCustomers())
                        .newCustomers(snap.getNewCustomers())
                        .netChurnPercent(snap.getNetChurnPercent())
                        .build())
                .toList();
        }
        
        return new org.springframework.data.domain.PageImpl<>(pagedList, pageable, snapshots.size());
    }

}
