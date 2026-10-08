package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.DunningStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.DunningRetryLogRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.PaymentMethodRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class BillingRetryServiceImplTest {

        @Mock
        private DunningRetryLogRepository dunningRetryLogRepository;
        @Mock
        private PaymentRepository paymentRepository;
        @Mock
        private PaymentMethodRepository paymentMethodRepository;
        @Mock
        private InvoiceRepository invoiceRepository;
        @Mock
        private SubscriptionRepository subscriptionRepository;
        @Mock
        private MockPaymentGateway mockPaymentGateway;

        @InjectMocks
        private BillingRetryServiceImpl billingRetryService;

        private DunningRetryLog retryLog;
        private Invoice invoice;
        private Subscription subscription;
        private PaymentMethod paymentMethod;
        private Plan plan;

        @BeforeEach
        void setUp() {
                plan = Plan.builder()
                                .id(1L)
                                .billingPeriod(BillingPeriod.MONTHLY)
                                .build();

                subscription = Subscription.builder()
                                .id(1L)
                                .status(Status.ACTIVE)
                                .currentPeriodEnd(LocalDate.now())
                                .plan(plan)
                                .paymentMethodId(1L)
                                .build();

                invoice = Invoice.builder()
                                .id(1L)
                                .status(Status.PENDING)
                                .totalMinor(1000L)
                                .subscription(subscription)
                                .currency("USD")
                                .build();

                retryLog = new DunningRetryLog();
                retryLog.setId(1L);
                retryLog.setStatus(DunningStatus.SCHEDULED);
                retryLog.setInvoice(invoice);
                retryLog.setAttemptNo(1);

                paymentMethod = new PaymentMethod();
                paymentMethod.setId(1L);
                paymentMethod.setGatewayToken("token_123");
        }

        @Test
        void testRetryFailedPayments_Success() {
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gw_ref_123");

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.SUCCESS, retryLog.getStatus());
                assertEquals(Status.PAID, invoice.getStatus());
                assertEquals(Status.ACTIVE, subscription.getStatus());
                verify(paymentRepository, times(2)).save(any(Payment.class));
        }

        @Test
        void testRetryFailedPayments_InvoiceAlreadyPaid() {
                invoice.setStatus(Status.PAID);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.SCHEDULED, retryLog.getStatus());
        }

        @Test
        void testRetryFailedPayments_ZeroTotalInvoice() {
                invoice.setTotalMinor(0L);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.SUCCESS, retryLog.getStatus());
                assertEquals(Status.PAID, invoice.getStatus());
        }

        @Test
        void testRetryFailedPayments_InvalidPaymentMethod() {
                paymentMethod.setGatewayToken(null);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.FAILED, retryLog.getStatus());
        }

        @Test
        void testRetryFailedPayments_PaymentGatewayFailure() {
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString()))
                                .thenThrow(CustomException.badRequest("Card declined"));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.FAILED, retryLog.getStatus());
                verify(dunningRetryLogRepository, times(3)).save(any(DunningRetryLog.class));
        }

        @Test
        void testRetryFailedPayments_FinalAttemptFailure() {
                retryLog.setAttemptNo(3);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString()))
                                .thenThrow(CustomException.badRequest("Card declined"));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.FAILED, retryLog.getStatus());
                assertEquals(Status.CANCELED, subscription.getStatus());
                verify(subscriptionRepository, times(1)).save(subscription);
        }

        @Test
        void testRetryFailedPayments_NullInvoice() {
                retryLog.setInvoice(null);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));

                billingRetryService.retryFailedPayments();

                // Should return early - no payment processing
                verify(paymentMethodRepository, never()).findById(anyLong());
        }

        @Test
        void testRetryFailedPayments_CanceledSubscription() {
                subscription.setStatus(Status.CANCELED);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));

                billingRetryService.retryFailedPayments();

                verify(paymentMethodRepository, never()).findById(anyLong());
        }

        @Test
        void testRetryFailedPayments_NullPaymentMethod() {
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.FAILED, retryLog.getStatus());
                assertEquals("Invalid payment method", retryLog.getFailureReason());
        }

        @Test
        void testRetryFailedPayments_BlankGatewayToken() {
                paymentMethod.setGatewayToken("   ");
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

                billingRetryService.retryFailedPayments();

                assertEquals(DunningStatus.FAILED, retryLog.getStatus());
        }

        @Test
        void testRetryFailedPayments_YearlyAdvance() {
                plan.setBillingPeriod(BillingPeriod.YEARLY);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gw_ref_123");

                billingRetryService.retryFailedPayments();

                assertEquals(Status.ACTIVE, subscription.getStatus());
                // Yearly advance should add ~1 year
                assertTrue(subscription.getCurrentPeriodEnd().isAfter(LocalDate.now().plusMonths(11)));
        }

        @Test
        void testRetryFailedPayments_Attempt2Scheduling() {
                retryLog.setAttemptNo(1);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString()))
                                .thenThrow(CustomException.badRequest("Declined"));

                billingRetryService.retryFailedPayments();

                // Should create next retry log for attempt 2
                verify(dunningRetryLogRepository, times(3)).save(any(DunningRetryLog.class));
        }

        @Test
        void testRetryFailedPayments_Attempt3Scheduling() {
                retryLog.setAttemptNo(2);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(mockPaymentGateway.charge(anyString(), anyLong(), anyString()))
                                .thenThrow(CustomException.badRequest("Declined"));

                billingRetryService.retryFailedPayments();

                // Should create next retry log for attempt 3
                verify(dunningRetryLogRepository, times(3)).save(any(DunningRetryLog.class));
        }

        @Test
        void testRetryFailedPayments_NullSubscription() {
                invoice.setSubscription(null);
                when(dunningRetryLogRepository.findByStatusAndScheduledAtLessThanEqual(any(), any()))
                                .thenReturn(Arrays.asList(retryLog));

                billingRetryService.retryFailedPayments();

                verify(paymentMethodRepository, never()).findById(anyLong());
        }
}
