package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.*;
import com.recuriq.billing.repository.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionFlowServiceImplTest {

        @Mock
        private CustomerRepository customerRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private PlanRepository planRepository;
        @Mock
        private PaymentMethodRepository paymentMethodRepository;
        @Mock
        private SubscriptionRepository subscriptionRepository;
        @Mock
        private InvoiceRepository invoiceRepository;
        @Mock
        private PaymentRepository paymentRepository;
        @Mock
        private PriceBookEntryRepository priceBookEntryRepository;
        @Mock
        private TaxRateRepository taxRateRepository;
        @Mock
        private SubscriptionItemRepository subscriptionItemRepository;
        @Mock
        private CouponRepository couponRepository;
        @Mock
        private SubscriptionCouponRepository subscriptionCouponRepository;
        @Mock
        private InvoiceLineItemRepository invoiceLineItemRepository;
        @Mock
        private MockPaymentGateway mockPaymentGateway;
        @Mock
        private AuditLoggingService auditLoggingService;

        @InjectMocks
        private SubscriptionFlowServiceImpl subscriptionFlowService;

        private Customer customer;
        private User user;
        private Plan plan;
        private PaymentMethod paymentMethod;

        @BeforeEach
        void setUp() {
                user = User.builder().id(1L).email("test@test.com").build();
                customer = Customer.builder().id(1L).user(user).country("IN").currency("INR")
                                .status(Status.ACTIVE).creditBalanceMinor(0L).build();
                plan = Plan.builder().id(1L).name("Basic").status(Status.ACTIVE)
                                .defaultPriceMinor(1000L).taxMode(TaxMode.EXCLUSIVE).trialDays(0).build();
                paymentMethod = new PaymentMethod();
                paymentMethod.setId(1L);
                paymentMethod.setCustomer(customer);
                paymentMethod.setPaymentType(PaymentType.CARD);
                paymentMethod.setIsDefault(true);
                paymentMethod.setGatewayToken("token123");
        }

        // ==================== REGISTER CUSTOMER ====================
        @Test
        void testRegisterCustomerDetails_NewCustomer() {
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
                when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
                CustomerRegistrationRequest req = new CustomerRegistrationRequest();
                req.setPhone("1234567890");
                req.setCountry("IN");
                Customer result = subscriptionFlowService.registerCustomerDetails("test@test.com", req);
                assertNotNull(result);
                assertEquals("1234567890", result.getPhone());
                verify(customerRepository).save(any(Customer.class));
        }

        @Test
        void testRegisterCustomerDetails_AlreadyExists() {
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
                when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
                CustomerRegistrationRequest req = new CustomerRegistrationRequest();
                Customer result = subscriptionFlowService.registerCustomerDetails("test@test.com", req);
                assertEquals(customer, result);
                verify(customerRepository, never()).save(any());
        }

        @Test
        void testRegisterCustomerDetails_UserNotFound() {
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
                CustomerRegistrationRequest req = new CustomerRegistrationRequest();
                assertThrows(RuntimeException.class, () -> subscriptionFlowService
                                .registerCustomerDetails("test@test.com", req));
        }

        // ==================== CREATE PAYMENT METHOD ====================
        @Test
        void testCreatePaymentMethod_Card_Visa() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                PaymentMethodRequest req = new PaymentMethodRequest();
                req.setPaymentType(PaymentType.CARD);
                req.setCardNumber("4111111111111111");
                req.setExpiryMonth("12");
                req.setExpiryYear("2030");
                PaymentMethod result = subscriptionFlowService.createPaymentMethod(1L, req);
                assertEquals("1111", result.getCardLast4());
                assertEquals("VISA", result.getCardBrand());
        }

        @Test
        void testCreatePaymentMethod_Card_Mastercard() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                PaymentMethodRequest req = new PaymentMethodRequest();
                req.setPaymentType(PaymentType.CARD);
                req.setCardNumber("5111111111111111");
                req.setExpiryMonth("06");
                req.setExpiryYear("2028");
                PaymentMethod result = subscriptionFlowService.createPaymentMethod(1L, req);
                assertEquals("MASTERCARD", result.getCardBrand());
        }

        @Test
        void testCreatePaymentMethod_Card_Amex() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                PaymentMethodRequest req = new PaymentMethodRequest();
                req.setPaymentType(PaymentType.CARD);
                req.setCardNumber("3411111111111111");
                req.setExpiryMonth("03");
                req.setExpiryYear("2029");
                PaymentMethod result = subscriptionFlowService.createPaymentMethod(1L, req);
                assertEquals("AMEX", result.getCardBrand());
        }

        @Test
        void testCreatePaymentMethod_Card_Unknown() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                PaymentMethodRequest req = new PaymentMethodRequest();
                req.setPaymentType(PaymentType.CARD);
                req.setCardNumber("9111111111111111");
                req.setExpiryMonth("03");
                req.setExpiryYear("2029");
                PaymentMethod result = subscriptionFlowService.createPaymentMethod(1L, req);
                assertEquals("UNKNOWN", result.getCardBrand());
        }

        @Test
        void testCreatePaymentMethod_UPI() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                PaymentMethodRequest req = new PaymentMethodRequest();
                req.setPaymentType(PaymentType.UPI);
                req.setUpiId("user@okicici");
                PaymentMethod result = subscriptionFlowService.createPaymentMethod(1L, req);
                assertEquals("user@okicici", result.getUpiId());
                assertTrue(result.getGatewayToken().contains("upi"));
        }

        @Test
        void testCreatePaymentMethod_NullCustomerId() {
                PaymentMethodRequest req = new PaymentMethodRequest();
                assertThrows(RuntimeException.class,
                                () -> subscriptionFlowService.createPaymentMethod(null, req));
        }

        @Test
        void testCreatePaymentMethod_CustomerNotFound() {
                when(customerRepository.findById(1L)).thenReturn(Optional.empty());
                PaymentMethodRequest req = new PaymentMethodRequest();
                assertThrows(RuntimeException.class,
                                () -> subscriptionFlowService.createPaymentMethod(1L, req));
        }

        // ==================== COMPLETE SUBSCRIPTION ====================
        @Test
        void testCompleteSubscription_NoTrial() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
                assertEquals("Subscription activated successfully", resp.getMessage());
        }

        @Test
        void testCompleteSubscription_WithTrial() {
                Plan trialPlan = Plan.builder().id(2L).name("Trial").status(Status.ACTIVE)
                                .defaultPriceMinor(2000L).taxMode(TaxMode.EXCLUSIVE).trialDays(14).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(2L)).thenReturn(Optional.of(trialPlan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(2L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
                assertNotNull(resp.getTrialEndDate());
                assertEquals("TRIALING", resp.getStatus());
        }

        @Test
        void testCompleteSubscription_YearlyPlan() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.YEARLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
        }

        @Test
        void testCompleteSubscription_WithPercentCoupon() {
                Coupon coupon = Coupon.builder().id(1L).code("SAVE10").name("Save 10%")
                                .type(CouponType.PERCENT).amount(10L).status(Status.ACTIVE)
                                .redeemedCount(0).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());
                when(couponRepository.findByCodeAndStatus("SAVE10", Status.ACTIVE)).thenReturn(Optional.of(coupon));

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);
                req.setCouponCode("SAVE10");

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
                verify(couponRepository).save(coupon);
        }

        @Test
        void testCompleteSubscription_WithFixedCoupon() {
                Coupon coupon = Coupon.builder().id(1L).code("FLAT500").name("Flat 500 Off")
                                .type(CouponType.FIXED).amount(500L).status(Status.ACTIVE)
                                .redeemedCount(0).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());
                when(couponRepository.findByCodeAndStatus("FLAT500", Status.ACTIVE)).thenReturn(Optional.of(coupon));

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);
                req.setCouponCode("FLAT500");

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
        }

        @Test
        void testCompleteSubscription_CouponExpired() {
                Coupon coupon = Coupon.builder().id(1L).code("OLD").name("Old")
                                .type(CouponType.PERCENT).amount(10L).status(Status.ACTIVE)
                                .validTo(LocalDate.now().minusDays(1)).redeemedCount(0).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());
                when(couponRepository.findByCodeAndStatus("OLD", Status.ACTIVE)).thenReturn(Optional.of(coupon));

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);
                req.setCouponCode("OLD");

                // Should succeed but coupon ignored (not applied)
                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
                verify(couponRepository, never()).save(coupon); // Coupon not saved
        }

        @Test
        void testCompleteSubscription_InactivePlan() {
                Plan inactivePlan = Plan.builder().id(3L).name("Old Plan").status(Status.INACTIVE)
                                .defaultPriceMinor(1000L).taxMode(TaxMode.EXCLUSIVE).trialDays(0).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(3L)).thenReturn(Optional.of(inactivePlan));

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(3L);
                req.setPaymentMethodId(1L);

                assertThrows(RuntimeException.class, () -> subscriptionFlowService.completeSubscription(1L, req));
        }

        @Test
        void testCompleteSubscription_InclusiveTax() {
                Plan inclusivePlan = Plan.builder().id(4L).name("Inclusive").status(Status.ACTIVE)
                                .defaultPriceMinor(1180L).taxMode(TaxMode.INCLUSIVE).trialDays(0).build();
                TaxRate tr = new TaxRate();
                tr.setRatePercent(new BigDecimal("18"));

                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(4L)).thenReturn(Optional.of(inclusivePlan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.of(tr));

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(4L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
        }

        @Test
        void testCompleteSubscription_WithCustomerCredit() {
                Customer creditCustomer = Customer.builder().id(1L).user(user).country("IN")
                                .currency("INR").status(Status.ACTIVE).creditBalanceMinor(500L).build();
                when(customerRepository.findById(1L)).thenReturn(Optional.of(creditCustomer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                                .thenReturn(Optional.empty());
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
                verify(customerRepository, atLeastOnce()).save(creditCustomer);
        }

        @Test
        void testCompleteSubscription_PlanNotFound() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.empty());
                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                assertThrows(RuntimeException.class, () -> subscriptionFlowService.completeSubscription(1L, req));
        }

        @Test
        void testCompleteSubscription_PaymentMethodNotFound() {
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());
                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                assertThrows(RuntimeException.class, () -> subscriptionFlowService.completeSubscription(1L, req));
        }

        @Test
        void testCompleteSubscription_WithRegionPrice() {
                PriceBookEntry pbe = new PriceBookEntry();
                pbe.setPriceMinor(800L);
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
                when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
                when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(1L, "IN", "INR"))
                                .thenReturn(Optional.of(pbe));
                when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                                .thenReturn(Optional.empty());

                SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
                req.setPlanId(1L);
                req.setPaymentMethodId(1L);
                req.setBillingPeriod(BillingPeriod.MONTHLY);

                SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
                assertNotNull(resp);
        }

        // ==================== CHECK CUSTOMER STATUS ====================
        @Test
        void testCheckCustomerStatus_UserNotFound() {
                when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
                CustomerStatusResponse resp = subscriptionFlowService.checkCustomerStatus("unknown@test.com");
                assertFalse(resp.isCustomer());
        }

        @Test
        void testCheckCustomerStatus_CustomerNotFound() {
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
                when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
                CustomerStatusResponse resp = subscriptionFlowService.checkCustomerStatus("test@test.com");
                assertFalse(resp.isCustomer());
        }

        @Test
        void testCheckCustomerStatus_ActiveSubscription() {
                Subscription activeSub = Subscription.builder().id(1L).status(Status.ACTIVE).build();
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
                when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
                when(subscriptionRepository.findByCustomer_IdAndStatusIn(1L,
                                List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED,
                                                Status.ON_HOLD, Status.CANCELED)))
                                .thenReturn(List.of(activeSub));
                when(subscriptionRepository.findByCustomer_IdAndStatusIn(1L, List.of(Status.DRAFT)))
                                .thenReturn(Collections.emptyList());

                CustomerStatusResponse resp = subscriptionFlowService.checkCustomerStatus("test@test.com");
                assertTrue(resp.isCustomer());
        }

        @Test
        void testCheckCustomerStatus_DraftOnly() {
                Subscription draftSub = Subscription.builder().id(1L).status(Status.DRAFT).build();
                when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
                when(customerRepository.findByUser_Id(1L)).thenReturn(Optional.of(customer));
                when(subscriptionRepository.findByCustomer_IdAndStatusIn(1L,
                                List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED,
                                                Status.ON_HOLD, Status.CANCELED)))
                                .thenReturn(Collections.emptyList());
                when(subscriptionRepository.findByCustomer_IdAndStatusIn(1L, List.of(Status.DRAFT)))
                                .thenReturn(List.of(draftSub));

                CustomerStatusResponse resp = subscriptionFlowService.checkCustomerStatus("test@test.com");
                assertTrue(resp.isHasDraftSubscription());
                assertFalse(resp.isCustomer());
        }

    @Test
    void testCompleteSubscription_CouponValidFromInFuture() {
        Coupon coupon = Coupon.builder().id(1L).code("FUTURE").name("Future")
                .type(CouponType.PERCENT).amount(10L).status(Status.ACTIVE)
                .validFrom(LocalDate.now().plusDays(1)).redeemedCount(0).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndStatus("FUTURE", Status.ACTIVE)).thenReturn(Optional.of(coupon));

        SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
        req.setPlanId(1L);
        req.setPaymentMethodId(1L);
        req.setBillingPeriod(BillingPeriod.MONTHLY);
        req.setCouponCode("FUTURE");

        SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
        assertNotNull(resp);
        verify(couponRepository, never()).save(coupon);
    }

    @Test
    void testCompleteSubscription_CouponMaxRedemptionsReached() {
        Coupon coupon = Coupon.builder().id(1L).code("MAXED").name("Maxed")
                .type(CouponType.PERCENT).amount(10L).status(Status.ACTIVE)
                .maxRedemptions(5).redeemedCount(5).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndStatus("MAXED", Status.ACTIVE)).thenReturn(Optional.of(coupon));

        SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
        req.setPlanId(1L);
        req.setPaymentMethodId(1L);
        req.setBillingPeriod(BillingPeriod.MONTHLY);
        req.setCouponCode("MAXED");

        SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
        assertNotNull(resp);
        verify(couponRepository, never()).save(coupon);
    }

    @Test
    void testCompleteSubscription_CouponDiscountExceedsPrice() {
        // Plan default price is 1000L.
        Coupon coupon = Coupon.builder().id(1L).code("OVER").name("Over Discount")
                .type(CouponType.FIXED).amount(2000L).status(Status.ACTIVE)
                .redeemedCount(0).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndStatus("OVER", Status.ACTIVE)).thenReturn(Optional.of(coupon));

        SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
        req.setPlanId(1L);
        req.setPaymentMethodId(1L);
        req.setBillingPeriod(BillingPeriod.MONTHLY);
        req.setCouponCode("OVER");

        SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
        assertNotNull(resp);
        assertEquals(0L, resp.getTotalAmountMinor()); // total should be 0, not negative
        verify(couponRepository).save(coupon);
    }

    @Test
    void testCompleteSubscription_ExclusiveTaxWithRate() {
        TaxRate tr = new TaxRate();
        tr.setRatePercent(new BigDecimal("10"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan)); // Plan is EXCLUSIVE tax mode, 1000L
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(priceBookEntryRepository.findByPlan_IdAndRegionAndCurrency(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(any(), any()))
                .thenReturn(Optional.of(tr));

        SubscriptionCompletionRequest req = new SubscriptionCompletionRequest();
        req.setPlanId(1L);
        req.setPaymentMethodId(1L);
        req.setBillingPeriod(BillingPeriod.MONTHLY);

        SubscriptionResponse resp = subscriptionFlowService.completeSubscription(1L, req);
        assertNotNull(resp);
        // Total = 1000 + 10% tax = 1100
        assertEquals(1100L, resp.getTotalAmountMinor());
    }
}
