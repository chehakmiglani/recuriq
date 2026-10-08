package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.recuriq.billing.dto.customer.InvoiceLineItemDTO;
import com.recuriq.billing.dto.finance.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.repository.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevenueAnalyticsServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private RevenueSnapshotRepository snapshotRepository;
    @Mock private PriceBookEntryRepository priceBookEntryRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;

    @InjectMocks
    private RevenueAnalyticsServiceImpl revenueAnalyticsService;

    private Subscription subscription;
    private Customer customer;
    private Plan plan;
    private RevenueSnapshot snapshot;
    private RevenueSnapshot snapshot2;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).country("IN").build();
        plan = Plan.builder().id(1L).name("Basic").defaultPriceMinor(1000L).billingPeriod(BillingPeriod.MONTHLY).build();
        subscription = Subscription.builder()
                .id(1L).customer(customer).plan(plan).status(Status.ACTIVE).currency("INR")
                .startDate(LocalDate.of(2026, 1, 1)).build();
        snapshot = RevenueSnapshot.builder()
                .id(1L).snapshotDate(LocalDate.of(2026, 4, 30)).mrrMinor(10000L).arrMinor(120000L)
                .arpuMinor(5000L).netChurnPercent(new BigDecimal("2.5"))
                .activeCustomers(2).newCustomers(1).totalRevenueMinor(50000L).build();
        snapshot2 = RevenueSnapshot.builder()
                .id(2L).snapshotDate(LocalDate.of(2026, 5, 31)).mrrMinor(9000L).arrMinor(108000L)
                .arpuMinor(4500L).netChurnPercent(new BigDecimal("3.0"))
                .activeCustomers(2).newCustomers(0).totalRevenueMinor(45000L).build();
    }

    // ==================== FINANCE DASHBOARD ====================
    @Test
    void testGetFinanceDashboard() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(1000L, dto.getMrrMinor());
        assertEquals(12000L, dto.getArrMinor());
    }

    @Test
    void testGetFinanceDashboard_SnapshotsEmpty() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Collections.emptyList());
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(0.0, dto.getNetChurnPercent().doubleValue());
    }

    @Test
    void testGetFinanceDashboard_UsdCurrency() {
        subscription.setCurrency("USD");
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertEquals(1000L * 95, dto.getMrrMinor());
    }

    @Test
    void testGetFinanceDashboard_GbpCurrency() {
        subscription.setCurrency("GBP");
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertEquals(1000L * 129, dto.getMrrMinor());
    }

    @Test
    void testGetFinanceDashboard_AnnualPlan() {
        Plan yearlyPlan = Plan.builder().id(2L).name("Annual").defaultPriceMinor(12000L)
                .billingPeriod(BillingPeriod.YEARLY).build();
        Subscription yearlySub = Subscription.builder().id(2L).customer(customer)
                .plan(yearlyPlan).status(Status.ACTIVE).currency("INR").build();

        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(yearlySub));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertEquals(1000L, dto.getMrrMinor()); // 12000/12 = 1000
    }

    @Test
    void testGetFinanceDashboard_WithCanceledSubs() {
        Subscription canceledSub = Subscription.builder().id(2L).customer(customer)
                .plan(plan).status(Status.CANCELED).currency("INR").build();
        Payment successPayment = new Payment();
        successPayment.setStatus(Status.SUCCESS);
        successPayment.setAmountMinor(5000L);
        successPayment.setCurrency("INR");
        Invoice paidInvoice = Invoice.builder().status(Status.PAID).customer(customer).build();
        successPayment.setInvoice(paidInvoice);

        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Arrays.asList(canceledSub));
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByStatus(Status.SUCCESS)).thenReturn(Arrays.asList(successPayment));
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertEquals(5000L, dto.getLtvMinor());
    }

    @Test
    void testGetFinanceDashboard_LtvFallback_ZeroChurn() {
        RevenueSnapshot zeroChurnSnap = RevenueSnapshot.builder().id(1L)
                .snapshotDate(LocalDate.now()).mrrMinor(10000L).arrMinor(120000L)
                .netChurnPercent(BigDecimal.ZERO).build();

        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(zeroChurnSnap));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        // LTV = arpu * 20 when churn is 0
        assertEquals(1000L * 20, dto.getLtvMinor());
    }

    // ==================== MRR REPORT ====================
    @Test
    void testGetMrrReport() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        MrrReportDTO dto = revenueAnalyticsService.getMrrReport();
        assertNotNull(dto);
        assertTrue(dto.getMrrMinor() > 0);
        assertNotNull(dto.getRevenueTrend());
    }

    // ==================== ARR REPORT ====================
    @Test
    void testGetArrReport() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        ArrReportDTO dto = revenueAnalyticsService.getArrReport();
        assertNotNull(dto);
        assertNotNull(dto.getArrTrend());
        assertNotNull(dto.getRevenueByPlan());
        assertNotNull(dto.getRevenueByRegion());
    }

    // ==================== CHURN REPORT ====================
    @Test
    void testGetChurnReport_WithTwoSnapshots() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot, snapshot2));

        ChurnReportDTO dto = revenueAnalyticsService.getChurnReport();
        assertNotNull(dto);
        assertTrue(dto.getRevenueChurnPercent().doubleValue() >= 0);
        assertNotNull(dto.getChurnTrend());
        assertNotNull(dto.getReasons());
    }

    @Test
    void testGetChurnReport_WithOneSnapshot() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));

        ChurnReportDTO dto = revenueAnalyticsService.getChurnReport();
        assertNotNull(dto);
        assertNotNull(dto.getChurnTrend());
    }

    @Test
    void testGetChurnReport_NoSnapshots() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        Subscription canceledSub = Subscription.builder().id(2L).customer(customer)
                .plan(plan).status(Status.CANCELED).currency("INR")
                .startDate(LocalDate.of(2026, 1, 1))
                .canceledAt(LocalDateTime.of(2026, 6, 15, 12, 0)).build();
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Arrays.asList(canceledSub));
        when(subscriptionRepository.findAll()).thenReturn(Arrays.asList(subscription, canceledSub));

        ChurnReportDTO dto = revenueAnalyticsService.getChurnReport();
        assertNotNull(dto);
        assertTrue(dto.getNetChurnPercent().doubleValue() > 0);
    }

    // ==================== ARPU/LTV REPORT ====================
    @Test
    void testGetArpuLtvReport() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));

        ArpuLtvReportDTO dto = revenueAnalyticsService.getArpuLtvReport();
        assertNotNull(dto);
        assertTrue(dto.getArpuMinor() > 0);
        assertTrue(dto.getLtvMinor() > 0);
        assertNotNull(dto.getCacLtvRatio());
        assertNotNull(dto.getArpuTrend());
    }

    @Test
    void testGetArpuLtvReport_NoActiveCustomers() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Collections.emptyList());

        ArpuLtvReportDTO dto = revenueAnalyticsService.getArpuLtvReport();
        assertNotNull(dto);
        assertEquals(0L, dto.getArpuMinor());
    }

    // ==================== INVOICE RECORDS ====================
    @Test
    void testGetAllInvoiceRecords() {
        Invoice invoice = Invoice.builder().id(1L).customer(customer).totalMinor(5000L)
                .currency("INR").issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(7))
                .status(Status.PAID).createdAt(LocalDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(invoice)));

        Page<InvoiceRecordDTO> result = revenueAnalyticsService.getAllInvoiceRecords(pageable);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== PAYMENT RECORDS ====================
    @Test
    void testGetAllPaymentRecords() {
        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentType(PaymentType.CARD);
        pm.setCardLast4("1234");
        Invoice invoice = Invoice.builder().id(1L).customer(customer).currency("INR")
                .issueDate(LocalDate.now()).build();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(invoice);
        payment.setPaymentMethod(pm);
        payment.setAmountMinor(5000L);
        payment.setCurrency("INR");
        payment.setStatus(Status.SUCCESS);
        payment.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(payment)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllPaymentRecords_NullInvoice() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(null);
        payment.setPaymentMethod(null);
        payment.setAmountMinor(5000L);
        payment.setCurrency("INR");
        payment.setStatus(Status.FAILED);
        payment.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(payment)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("N/A", result.getContent().get(0).getInvoiceNumber());
    }

    @Test
    void testGetAllPaymentRecords_UpiPaymentMethod() {
        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentType(PaymentType.UPI);
        pm.setUpiId("user@okbank");
        Invoice invoice = Invoice.builder().id(1L).customer(customer).currency("INR")
                .issueDate(LocalDate.now()).build();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(invoice);
        payment.setPaymentMethod(pm);
        payment.setAmountMinor(3000L);
        payment.setCurrency("INR");
        payment.setStatus(Status.SUCCESS);
        payment.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(payment)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertTrue(result.getContent().get(0).getPaymentMethod().contains("UPI"));
    }

    // ==================== REFUND CREDITS ====================
    @Test
    void testGetAllRefundCredits() {
        Invoice invoice = Invoice.builder().id(1L).customer(customer).currency("INR")
                .issueDate(LocalDate.now()).build();
        CreditNote cn = new CreditNote();
        cn.setId(1L);
        cn.setInvoice(invoice);
        cn.setAmountMinor(2000L);
        cn.setReason("Proration credit");
        cn.setStatus(Status.ISSUED);
        cn.setCreatedAt(LocalDateTime.now());

        Payment relatedPayment = new Payment();
        relatedPayment.setId(10L);

        Pageable pageable = PageRequest.of(0, 10);
        when(creditNoteRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(cn)));
        when(paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(relatedPayment));

        Page<RefundCreditDTO> result = revenueAnalyticsService.getAllRefundCredits(pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getPaymentId());
    }

    @Test
    void testGetAllRefundCredits_NullInvoice() {
        CreditNote cn = new CreditNote();
        cn.setId(1L);
        cn.setInvoice(null);
        cn.setAmountMinor(1000L);
        cn.setReason("Manual credit");
        cn.setStatus(Status.ISSUED);
        cn.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(creditNoteRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(cn)));

        Page<RefundCreditDTO> result = revenueAnalyticsService.getAllRefundCredits(pageable);
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getPaymentId());
    }

    // ==================== REVENUE SNAPSHOTS ====================
    @Test
    void testGetAllRevenueSnapshots() {
        Pageable pageable = PageRequest.of(0, 10);
        when(snapshotRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(snapshot)));

        Page<RevenueSnapshotDTO> result = revenueAnalyticsService.getAllRevenueSnapshots(pageable);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== ADDITIONAL COVERAGE TESTS ====================

    @Test
    void testGetInvoiceDetailById_Success() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .customer(customer)
                .totalMinor(5000L)
                .currency("INR")
                .issueDate(LocalDate.of(2026, 4, 1))
                .dueDate(LocalDate.of(2026, 4, 8))
                .status(Status.PAID)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        InvoiceLineItem item1 = new InvoiceLineItem();
        item1.setId(1L);
        item1.setDescription("Basic Plan");
        item1.setLineType(InvoiceLineItem.LineType.PLAN);
        item1.setQuantity(1);
        item1.setUnitPriceMinor(4000L);
        item1.setAmountMinor(4000L);
        item1.setPeriodStart(LocalDate.of(2026, 4, 1));
        item1.setPeriodEnd(LocalDate.of(2026, 5, 1));

        InvoiceLineItem item2 = new InvoiceLineItem();
        item2.setId(2L);
        item2.setDescription("HD Addon");
        item2.setLineType(InvoiceLineItem.LineType.ADDON);
        item2.setQuantity(1);
        item2.setUnitPriceMinor(1000L);
        item2.setAmountMinor(1000L);
        item2.setPeriodStart(null);
        item2.setPeriodEnd(null);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceLineItemRepository.findByInvoice_Id(1L)).thenReturn(Arrays.asList(item1, item2));

        InvoiceDetailDTO dto = revenueAnalyticsService.getInvoiceDetailById(1L);

        assertNotNull(dto);
        assertEquals("INV-2026-1", dto.getInvoiceNumber());
        assertEquals(1L, dto.getCustomerId());
        assertEquals(50.0, dto.getAmount());
        assertEquals(2, dto.getLineItems().size());

        InvoiceLineItemDTO dtoItem1 = dto.getLineItems().get(0);
        assertEquals(1L, dtoItem1.getLineItemId());
        assertEquals("Basic Plan", dtoItem1.getDescription());
        assertEquals(InvoiceLineItem.LineType.PLAN, dtoItem1.getLineType());
        assertEquals(1, dtoItem1.getQuantity());
        assertEquals(4000L, dtoItem1.getUnitPriceMinor());
        assertEquals(4000L, dtoItem1.getAmountMinor());
        assertEquals("2026-04-01", dtoItem1.getPeriodStart());
        assertEquals("2026-05-01", dtoItem1.getPeriodEnd());

        InvoiceLineItemDTO dtoItem2 = dto.getLineItems().get(1);
        assertNull(dtoItem2.getPeriodStart());
        assertNull(dtoItem2.getPeriodEnd());
    }

    @Test
    void testGetInvoiceDetailById_NotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> revenueAnalyticsService.getInvoiceDetailById(99L));
    }

    @Test
    void testGetMonthlyINRMinor_PriceBookEntryPresent() {
        PriceBookEntry entry = new PriceBookEntry();
        entry.setPriceMinor(5000L);
        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.of(entry));
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(5000L, dto.getMrrMinor()); // uses price book price 5000 paise
    }

    @Test
    void testLtvFallback_ChurnGreaterThanZero() {
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        RevenueSnapshot churnSnap = RevenueSnapshot.builder()
                .id(1L)
                .snapshotDate(LocalDate.now())
                .mrrMinor(10000L)
                .arrMinor(120000L)
                .netChurnPercent(new BigDecimal("5.0"))
                .build();
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(churnSnap));

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(20000L, dto.getLtvMinor());
    }

    @Test
    void testLoadAllSnapshots_SubscriptionStatusesAndEdges() {
        Customer c = Customer.builder().id(2L).country("US").build();
        Plan p = Plan.builder().id(2L).name("Gold").defaultPriceMinor(2000L).billingPeriod(BillingPeriod.MONTHLY).build();

        Subscription s1 = Subscription.builder()
                .id(10L).customer(c).plan(p).status(Status.ACTIVE).currency("INR")
                .startDate(LocalDate.of(2026, 1, 15)).build();

        Subscription s2 = Subscription.builder()
                .id(11L).customer(c).plan(p).status(Status.ACTIVE).currency("INR")
                .startDate(LocalDate.of(2026, 1, 15))
                .canceledAt(LocalDateTime.of(2026, 6, 15, 12, 0)).build();

        Subscription s3 = Subscription.builder()
                .id(12L).customer(c).plan(p).status(Status.CANCELED).currency("INR")
                .startDate(LocalDate.of(2026, 1, 15))
                .canceledAt(LocalDateTime.of(2026, 6, 15, 12, 0)).build();

        Subscription s4 = Subscription.builder()
                .id(13L).customer(c).plan(p).status(Status.CANCELED).currency("INR")
                .startDate(LocalDate.of(2026, 1, 15))
                .canceledAt(LocalDateTime.of(2026, 2, 15, 12, 0)).build();

        Subscription s5 = Subscription.builder()
                .id(14L).customer(c).plan(p).status(Status.TRIALING).currency("INR")
                .startDate(LocalDate.of(2026, 1, 15)).build();

        when(subscriptionRepository.findAll()).thenReturn(Arrays.asList(s1, s2, s3, s4, s5));

        Payment p1 = new Payment();
        p1.setStatus(Status.SUCCESS);
        p1.setAmountMinor(3000L);
        p1.setCurrency("INR");
        p1.setCreatedAt(LocalDateTime.of(2026, 1, 20, 10, 0));

        Payment p2 = new Payment();
        p2.setStatus(Status.SUCCESS);
        p2.setAmountMinor(100L);
        p2.setCurrency("USD");
        p2.setCreatedAt(LocalDateTime.of(2026, 2, 20, 10, 0));

        Payment p3 = new Payment();
        p3.setStatus(Status.FAILED);
        p3.setAmountMinor(5000L);
        p3.setCurrency("INR");
        p3.setCreatedAt(LocalDateTime.of(2026, 1, 25, 10, 0));

        when(paymentRepository.findAll()).thenReturn(Arrays.asList(p1, p2, p3));

        CreditNote cn1 = new CreditNote();
        cn1.setAmountMinor(500L);
        cn1.setCreatedAt(LocalDateTime.of(2026, 1, 25, 10, 0));
        Invoice inv1 = Invoice.builder().currency("INR").build();
        cn1.setInvoice(inv1);

        CreditNote cn2 = new CreditNote();
        cn2.setAmountMinor(100L);
        cn2.setCreatedAt(LocalDateTime.of(2026, 2, 25, 10, 0));
        Invoice inv2 = Invoice.builder().currency("USD").build();
        cn2.setInvoice(inv2);

        when(creditNoteRepository.findAll()).thenReturn(Arrays.asList(cn1, cn2));

        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(s1));
        
        MrrReportDTO dto = revenueAnalyticsService.getMrrReport();
        assertNotNull(dto);
        assertEquals(0, dto.getRevenueTrend().size());
    }

    @Test
    void testGetAllRevenueSnapshots_PaginationEdge() {
        when(subscriptionRepository.findAll()).thenReturn(Collections.emptyList());
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findAll()).thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(2, 10);
        Page<RevenueSnapshotDTO> result = revenueAnalyticsService.getAllRevenueSnapshots(pageable);
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test

    void testGenerateDailySnapshot_NewSnapshotSaved() {

        when(subscriptionRepository.findByStatus(Status.ACTIVE))

                .thenReturn(Arrays.asList(subscription));

        when(subscriptionRepository.findByStatus(Status.CANCELED))

                .thenReturn(Collections.emptyList());

        when(paymentRepository.findByStatus(Status.SUCCESS))

                .thenReturn(Collections.emptyList());

        when(creditNoteRepository.findByStatus(Status.ISSUED))

                .thenReturn(Collections.emptyList());

        when(snapshotRepository.findBySnapshotDate(any(LocalDate.class)))

                .thenReturn(Optional.empty());

        revenueAnalyticsService.generateDailySnapshot();

        verify(snapshotRepository, times(1))

                .save(any(RevenueSnapshot.class));

    }

    @Test

    void testGenerateDailySnapshot_AlreadyExists() {

        when(subscriptionRepository.findByStatus(Status.ACTIVE))

                .thenReturn(Arrays.asList(subscription));

        when(subscriptionRepository.findByStatus(Status.CANCELED))

                .thenReturn(Collections.emptyList());

        when(snapshotRepository.findBySnapshotDate(any(LocalDate.class)))

                .thenReturn(Optional.of(snapshot));

        revenueAnalyticsService.generateDailySnapshot();

        verify(snapshotRepository, never())

                .save(any());

    }

   

    

    @Test

    void testGetMonthlyINRMinor_PriceBookThrowsException() {

        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(

                anyLong(), anyString(), anyString()))

                .thenThrow(new RuntimeException());

        when(subscriptionRepository.findByStatus(Status.ACTIVE))

                .thenReturn(Arrays.asList(subscription));

        when(subscriptionRepository.findByStatus(Status.CANCELED))

                .thenReturn(Collections.emptyList());

        when(snapshotRepository.findAllByOrderBySnapshotDateAsc())

                .thenReturn(Collections.emptyList());

        when(paymentRepository.findByStatus(Status.FAILED))

                .thenReturn(Collections.emptyList());

        when(creditNoteRepository.findByStatus(Status.ISSUED))

                .thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto =

                revenueAnalyticsService.getFinanceDashboard();

        assertNotNull(dto);

    }

    @Test

    void testFormatPaymentMethod_OtherType() {

        PaymentMethod pm = new PaymentMethod();

        pm.setPaymentType(PaymentType.CARD);

        Invoice invoice = Invoice.builder()

                .id(1L)

                .customer(customer)

                .currency("INR")

                .issueDate(LocalDate.now())

                .build();

        Payment payment = new Payment();

        payment.setId(1L);

        payment.setInvoice(invoice);

        payment.setPaymentMethod(pm);

        payment.setAmountMinor(5000L);

        payment.setCurrency("INR");

        payment.setStatus(Status.SUCCESS);

        payment.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);

        when(paymentRepository.findAllByOrderByIdDesc(pageable))

                .thenReturn(new PageImpl<>(Arrays.asList(payment)));

        Page<PaymentRecordDTO> result =

                revenueAnalyticsService.getAllPaymentRecords(pageable);

        assertEquals("CARD ********",

                result.getContent().get(0).getPaymentMethod());

    }

    @Test

    void testGetAllRefundCredits_NoPaymentFound() {

        Invoice invoice = Invoice.builder()

                .id(1L)

                .customer(customer)

                .currency("INR")

                .issueDate(LocalDate.now())

                .build();

        CreditNote cn = new CreditNote();
        cn.setId(1L);

        cn.setInvoice(invoice);

        cn.setAmountMinor(1000L);

        cn.setReason("Refund");

        cn.setStatus(Status.ISSUED);

        cn.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);

        when(creditNoteRepository.findAllByOrderByIdDesc(pageable))

                .thenReturn(new PageImpl<>(Arrays.asList(cn)));

        when(paymentRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(1L))

                .thenReturn(Optional.empty());

        Page<RefundCreditDTO> result =

                revenueAnalyticsService.getAllRefundCredits(pageable);

        assertNull(result.getContent().get(0).getPaymentId());

    }

    @Test

    void testGetAllRevenueSnapshots_WithData() {

        when(snapshotRepository.findAllByOrderBySnapshotDateAsc())

                .thenReturn(Arrays.asList(snapshot, snapshot2));

        Pageable pageable = PageRequest.of(0, 10);

        Page<RevenueSnapshotDTO> result =

                revenueAnalyticsService.getAllRevenueSnapshots(pageable);

        assertEquals(2, result.getContent().size());

    }

    @Test

    void testGetAllInvoiceRecords_UsdConversion() {

        Invoice invoice = Invoice.builder()

                .id(1L)

                .customer(customer)

                .totalMinor(100L)

                .currency("USD")

                .issueDate(LocalDate.now())

                .dueDate(LocalDate.now().plusDays(7))

                .status(Status.PAID)

                .createdAt(LocalDateTime.now())

                .build();

        Pageable pageable = PageRequest.of(0, 10);

        when(invoiceRepository.findAllByOrderByIdDesc(pageable))

                .thenReturn(new PageImpl<>(Arrays.asList(invoice)));

        Page<InvoiceRecordDTO> result =

                revenueAnalyticsService.getAllInvoiceRecords(pageable);

        assertEquals(95.0,

                result.getContent().get(0).getAmount());

    }

    @Test

    void testGetInvoiceDetailById_UsdConversion() {

        Invoice invoice = Invoice.builder()

                .id(1L)

                .customer(customer)

                .totalMinor(100L)

                .currency("USD")

                .issueDate(LocalDate.of(2026, 1, 1))

                .status(Status.PAID)

                .createdAt(LocalDateTime.now())

                .build();

        InvoiceLineItem item = new InvoiceLineItem();

        item.setId(1L);

        item.setDescription("Test");

        item.setQuantity(1);

        item.setUnitPriceMinor(100L);

        item.setAmountMinor(100L);

        when(invoiceRepository.findById(1L))

                .thenReturn(Optional.of(invoice));

        when(invoiceLineItemRepository.findByInvoice_Id(1L))

                .thenReturn(Arrays.asList(item));

        InvoiceDetailDTO dto =
                revenueAnalyticsService.getInvoiceDetailById(1L);

        assertEquals(9500L,
                dto.getLineItems().get(0).getUnitPriceMinor());

    }

    // ==================== NEW CORNER CASE TESTS ====================

    @Test
    void testGetMonthlyINRMinor_GuardClauses() {
        Subscription sNullPlan = Subscription.builder()
                .id(20L)
                .plan(null)
                .customer(customer)
                .status(Status.ACTIVE)
                .currency("INR")
                .build();
        
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(sNullPlan));
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Collections.emptyList());

        MrrReportDTO dto = revenueAnalyticsService.getMrrReport();
        assertNotNull(dto);
        assertEquals(0L, dto.getMrrMinor());

        Subscription sNullCustomer = Subscription.builder()
                .id(21L)
                .plan(plan)
                .customer(null)
                .status(Status.ACTIVE)
                .currency("INR")
                .build();
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(sNullCustomer));
        dto = revenueAnalyticsService.getMrrReport();
        assertNotNull(dto);
        assertEquals(0L, dto.getMrrMinor());
    }

    @Test
    void testGetMonthlyINRMinor_CountryNull() {
        Customer cNullCountry = Customer.builder().id(2L).country(null).build();
        Subscription sNullCountry = Subscription.builder()
                .id(22L)
                .plan(plan)
                .customer(cNullCountry)
                .status(Status.ACTIVE)
                .currency("INR")
                .build();

        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(sNullCountry));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Collections.emptyList());
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByStatus(Status.ISSUED)).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(1000L, dto.getMrrMinor());
        verify(priceBookEntryRepository, never()).findByPlan_IdAndRegionAndCurrency(anyLong(), anyString(), anyString());
    }

    @Test
    void testGetMonthlyINRMinor_PriceBookEntryAbsent() {
        Customer cUs = Customer.builder().id(2L).country("US").build();
        Subscription sUs = Subscription.builder()
                .id(23L)
                .plan(plan)
                .customer(cUs)
                .status(Status.ACTIVE)
                .currency("USD")
                .build();

        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(1L, "US", "USD"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(sUs));
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Collections.emptyList());
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByStatus(Status.ISSUED)).thenReturn(Collections.emptyList());

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(95000L, dto.getMrrMinor());
    }

    @Test
    void testFormatPaymentMethod_NullPaymentMethod() {
        Payment p = new Payment();
        p.setId(100L);
        p.setInvoice(null);
        p.setPaymentMethod(null);
        p.setAmountMinor(5000L);
        p.setCurrency("INR");
        p.setStatus(Status.SUCCESS);
        p.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(p)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Unknown", result.getContent().get(0).getPaymentMethod());
    }

    @Test
    void testFormatPaymentMethod_CardNullLast4() {
        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentType(PaymentType.CARD);
        pm.setCardLast4(null);

        Payment p = new Payment();
        p.setId(101L);
        p.setInvoice(null);
        p.setPaymentMethod(pm);
        p.setAmountMinor(5000L);
        p.setCurrency("INR");
        p.setStatus(Status.SUCCESS);
        p.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(p)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertNotNull(result);
        assertEquals("CARD ********", result.getContent().get(0).getPaymentMethod());
    }

    @Test
    void testFormatPaymentMethod_UpiNullUpiId() {
        PaymentMethod pm = new PaymentMethod();
        pm.setPaymentType(PaymentType.UPI);
        pm.setUpiId(null);

        Payment p = new Payment();
        p.setId(102L);
        p.setInvoice(null);
        p.setPaymentMethod(pm);
        p.setAmountMinor(5000L);
        p.setCurrency("INR");
        p.setStatus(Status.SUCCESS);
        p.setCreatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(p)));

        Page<PaymentRecordDTO> result = revenueAnalyticsService.getAllPaymentRecords(pageable);
        assertNotNull(result);
        assertEquals("UPI ", result.getContent().get(0).getPaymentMethod());
    }

    @Test
    void testToINRMinor_UnsupportedCurrency() {
        Invoice invoice = Invoice.builder()
                .id(5L)
                .customer(customer)
                .totalMinor(4500L)
                .currency("EUR")
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .status(Status.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(invoiceRepository.findAllByOrderByIdDesc(pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(invoice)));

        Page<InvoiceRecordDTO> result = revenueAnalyticsService.getAllInvoiceRecords(pageable);
        assertNotNull(result);
        assertEquals(45.0, result.getContent().get(0).getAmount());
    }

    @Test
    void testComputeLtvMinor_EmptyCanceledSubs() {
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByStatus(Status.ISSUED)).thenReturn(Collections.emptyList());

        RevenueSnapshot positiveChurnSnap = RevenueSnapshot.builder()
                .id(1L)
                .snapshotDate(LocalDate.now())
                .mrrMinor(10000L)
                .arrMinor(120000L)
                .netChurnPercent(new BigDecimal("5.0"))
                .build();
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(positiveChurnSnap));

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(20000L, dto.getLtvMinor());
    }

    @Test
    void testComputeLtvMinor_FallbackZeroChurn() {
        when(subscriptionRepository.findByStatus(Status.CANCELED)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(paymentRepository.findByStatus(Status.FAILED)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findByStatus(Status.ISSUED)).thenReturn(Collections.emptyList());

        RevenueSnapshot zeroChurnSnap = RevenueSnapshot.builder()
                .id(1L)
                .snapshotDate(LocalDate.now())
                .mrrMinor(10000L)
                .arrMinor(120000L)
                .netChurnPercent(BigDecimal.ZERO)
                .build();
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(zeroChurnSnap));

        FinanceDashboardDTO dto = revenueAnalyticsService.getFinanceDashboard();
        assertNotNull(dto);
        assertEquals(20000L, dto.getLtvMinor());
    }

    @Test
    void testGetAllRevenueSnapshots_SortingWithNullDates() {
        RevenueSnapshot snapNull1 = RevenueSnapshot.builder()
                .id(10L)
                .snapshotDate(null)
                .mrrMinor(0L)
                .arrMinor(0L)
                .arpuMinor(0L)
                .activeCustomers(0)
                .newCustomers(0)
                .churnedCustomers(0)
                .grossChurnPercent(BigDecimal.ZERO)
                .netChurnPercent(BigDecimal.ZERO)
                .ltvMinor(0L)
                .totalRevenueMinor(0L)
                .totalRefundsMinor(0L)
                .createdAt(LocalDateTime.now())
                .build();
        RevenueSnapshot snapNull2 = RevenueSnapshot.builder()
                .id(11L)
                .snapshotDate(null)
                .mrrMinor(0L)
                .arrMinor(0L)
                .arpuMinor(0L)
                .activeCustomers(0)
                .newCustomers(0)
                .churnedCustomers(0)
                .grossChurnPercent(BigDecimal.ZERO)
                .netChurnPercent(BigDecimal.ZERO)
                .ltvMinor(0L)
                .totalRevenueMinor(0L)
                .totalRefundsMinor(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapNull1, snapNull2));

        Pageable pageable = PageRequest.of(0, 10);
        Page<RevenueSnapshotDTO> result = revenueAnalyticsService.getAllRevenueSnapshots(pageable);
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testGetAllRevenueSnapshots_PaginationOffsetOutOfBounds() {
        when(snapshotRepository.findAllByOrderBySnapshotDateAsc()).thenReturn(Arrays.asList(snapshot));

        Pageable pageable = PageRequest.of(5, 10);
        Page<RevenueSnapshotDTO> result = revenueAnalyticsService.getAllRevenueSnapshots(pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

}
