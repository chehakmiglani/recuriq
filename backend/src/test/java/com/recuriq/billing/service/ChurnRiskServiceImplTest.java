package com.recuriq.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.dto.ai.ChurnRiskDTO;
import com.recuriq.billing.dto.ai.ChurnRiskSummaryDTO;
import com.recuriq.billing.entity.CancellationRequest;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.UsageRecord;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.CancellationRequestStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.CancellationRequestRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.UsageRecordRepository;

@ExtendWith(MockitoExtension.class)
class ChurnRiskServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UsageRecordRepository usageRecordRepository;
    @Mock
    private CancellationRequestRepository cancellationRequestRepository;

    @InjectMocks
    private ChurnRiskServiceImpl churnRiskService;

    private final LocalDateTime now = LocalDateTime.now();
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = subscription(1L, Status.ACTIVE, LocalDate.now().minusDays(200));
    }

    @Test
    void healthyCustomer_isLowRisk() {
        ChurnRiskDTO result = churnRiskService.score(subscription, now, List.of(), List.of(), List.of(), false);

        assertEquals(0, result.getScore());
        assertEquals("LOW", result.getLevel());
        assertEquals(List.of("No risk signals"), result.getReasons());
    }

    @Test
    void pastDueWithFailedPayments_isMediumRisk() {
        subscription.setStatus(Status.PAST_DUE);
        List<Payment> payments = List.of(payment(Status.FAILED, 5), payment(Status.FAILED, 20));

        ChurnRiskDTO result = churnRiskService.score(subscription, now, payments, List.of(), List.of(), false);

        assertEquals(54, result.getScore());   // 30 past due + 2 x 12 failed payments
        assertEquals("MEDIUM", result.getLevel());
        assertTrue(result.getReasons().contains("2 failed payments in the last 90 days"));
    }

    @Test
    void pendingCancellationAndOverdueInvoice_isHighRisk() {
        Invoice overdue = Invoice.builder().status(Status.OPEN).dueDate(LocalDate.now().minusDays(3)).build();

        ChurnRiskDTO result = churnRiskService.score(subscription, now, List.of(), List.of(overdue), List.of(), true);

        assertEquals(50, result.getScore());
        subscription.setCancelAtPeriodEnd(true);
        ChurnRiskDTO worse = churnRiskService.score(subscription, now, List.of(), List.of(overdue), List.of(), true);
        assertEquals(85, worse.getScore());
        assertEquals("HIGH", worse.getLevel());
    }

    @Test
    void failedPaymentPoints_areCapped() {
        List<Payment> payments = List.of(payment(Status.FAILED, 1), payment(Status.FAILED, 2),
                payment(Status.FAILED, 3), payment(Status.FAILED, 4), payment(Status.FAILED, 5));

        ChurnRiskDTO result = churnRiskService.score(subscription, now, payments, List.of(), List.of(), false);

        assertEquals(36, result.getScore());
    }

    @Test
    void oldFailedPayments_areIgnored() {
        ChurnRiskDTO result = churnRiskService.score(subscription, now,
                List.of(payment(Status.FAILED, 120)), List.of(), List.of(), false);

        assertEquals(0, result.getScore());
    }

    @Test
    void usageDrop_addsRisk() {
        List<UsageRecord> usage = List.of(usage(100L, 45), usage(20L, 10));

        ChurnRiskDTO result = churnRiskService.score(subscription, now, List.of(), List.of(), usage, false);

        assertEquals(15, result.getScore());
        assertTrue(result.getReasons().contains("Usage dropped 80% compared with the previous 30 days"));
    }

    @Test
    void loyalCustomerWithGoodPaymentHistory_neverGoesBelowZero() {
        subscription.setStartDate(LocalDate.now().minusYears(2));
        List<Payment> payments = List.of(payment(Status.SUCCESS, 10), payment(Status.SUCCESS, 40), payment(Status.SUCCESS, 70));

        ChurnRiskDTO result = churnRiskService.score(subscription, now, payments, List.of(), List.of(), false);

        assertEquals(0, result.getScore());
        assertTrue(result.getReasons().contains("Loyal customer (over a year)"));
        assertTrue(result.getReasons().contains("Consistent on-time payments"));
    }

    @Test
    void yearlyPlan_isNormalisedToMonthlyValue() {
        subscription.getPlan().setBillingPeriod(BillingPeriod.YEARLY);
        subscription.getPlan().setDefaultPriceMinor(120000L);

        ChurnRiskDTO result = churnRiskService.score(subscription, now, List.of(), List.of(), List.of(), false);

        assertEquals(10000L, result.getMonthlyValueMinor());
    }

    @Test
    void getChurnRisk_sortsByScoreAndSummarisesLevels() {
        Subscription risky = subscription(2L, Status.PAST_DUE, LocalDate.now().minusDays(200));
        risky.setCancelAtPeriodEnd(true);
        CancellationRequest request = new CancellationRequest();
        request.setSubscription(risky);

        when(subscriptionRepository.findByStatusIn(anyList())).thenReturn(List.of(subscription, risky));
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findAll()).thenReturn(List.of());
        when(usageRecordRepository.findAll()).thenReturn(List.of());
        when(cancellationRequestRepository.findByStatus(CancellationRequestStatus.PENDING)).thenReturn(List.of(request));

        ChurnRiskSummaryDTO summary = churnRiskService.getChurnRisk(null);

        assertEquals(1, summary.getHighRisk());
        assertEquals(1, summary.getLowRisk());
        assertEquals(2L, summary.getCustomers().get(0).getCustomerId());
        assertEquals(49900L, summary.getMonthlyRevenueAtRiskMinor());

        ChurnRiskSummaryDTO highOnly = churnRiskService.getChurnRisk("high");
        assertEquals(1, highOnly.getCustomers().size());
    }

    // ─── Builders ────────────────────────────────────────────────────────────

    private Subscription subscription(Long id, Status status, LocalDate start) {
        User user = new User();
        user.setFullName("Customer " + id);
        user.setEmail("customer" + id + "@example.com");
        Customer customer = new Customer();
        customer.setId(id);
        customer.setUser(user);
        customer.setCountry("IN");
        Plan plan = new Plan();
        plan.setName("Premium");
        plan.setBillingPeriod(BillingPeriod.MONTHLY);
        plan.setDefaultPriceMinor(49900L);
        plan.setDefaultCurrency("INR");
        return Subscription.builder()
                .id(id)
                .customer(customer)
                .plan(plan)
                .status(status)
                .startDate(start)
                .build();
    }

    private Payment payment(Status status, int daysAgo) {
        Payment payment = new Payment();
        payment.setStatus(status);
        payment.setCreatedAt(now.minusDays(daysAgo));
        return payment;
    }

    private UsageRecord usage(long quantity, int daysAgo) {
        UsageRecord record = new UsageRecord();
        record.setQuantity(quantity);
        record.setRecordedAt(now.minusDays(daysAgo));
        return record;
    }
}
