package com.recuriq.billing.service;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.recuriq.billing.exception.CustomException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.repository.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingEngineServiceImplTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;
	@Mock
	private InvoiceRepository invoiceRepository;
	@Mock
	private PaymentMethodRepository paymentMethodRepository;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private InvoiceLineItemRepository invoiceLineItemRepository;
	@Mock
	private SubscriptionItemRepository subscriptionItemRepository;
	@Mock
	private DunningRetryLogRepository dunningRetryLogRepository;
	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private MockPaymentGateway mockPaymentGateway;

	@InjectMocks
	private BillingEngineServiceImpl billingEngineService;

	private Subscription subscription;
	private Plan plan;
	private Customer customer;
	private PaymentMethod paymentMethod;

	@BeforeEach
	void setUp() {
		customer = Customer.builder().id(1L).creditBalanceMinor(0L).build();
		plan = Plan.builder().id(1L).name("Basic").defaultPriceMinor(1000L)
				.billingPeriod(BillingPeriod.MONTHLY).build();
		subscription = Subscription.builder().id(1L).customer(customer).plan(plan)
				.status(Status.ACTIVE).paymentMethodId(1L).currency("USD")
				.currentPeriodEnd(LocalDate.now()).build();
		paymentMethod = new PaymentMethod();
		paymentMethod.setId(1L);
		paymentMethod.setStatus(Status.ACTIVE);
		paymentMethod.setGatewayToken("token123");
	}

	// ==================== EXISTING TESTS ====================
	@Test
	void testProcessRenewals_Success() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		lenient().when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceRepository, times(3)).save(any(Invoice.class));
		verify(subscriptionRepository, times(1)).save(subscription);
	}

	@Test
	void testProcessRenewals_MissingPaymentMethod() {
		subscription.setPaymentMethodId(null);
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());

		billingEngineService.processRenewals();

		assertEquals(Status.PAST_DUE, subscription.getStatus());
		verify(subscriptionRepository, times(1)).save(subscription);
	}

	@Test
	void testProcessRenewals_PaymentFailed() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).currency("USD").build();
		when(invoiceRepository.save(any())).thenReturn(invoice);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

		InvoiceLineItem line = new InvoiceLineItem();
		line.setAmountMinor(1000L);
		line.setLineType(InvoiceLineItem.LineType.PLAN);
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(Arrays.asList(line));

		when(mockPaymentGateway.charge("token123", 1000L, "USD"))
				.thenThrow(CustomException.paymentFailed("Card declined"));

		billingEngineService.processRenewals();

		assertEquals(Status.PAST_DUE, subscription.getStatus());
		verify(dunningRetryLogRepository, times(1)).save(any());
	}

	@Test
	void testProcessRenewals_TrialExpired() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>());
		subscription.setStatus(Status.TRIALING);
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		lenient().when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		assertEquals(Status.ACTIVE, subscription.getStatus());
		verify(subscriptionRepository, times(1)).save(subscription);
	}

	// ==================== NEW TESTS ====================
	@Test
	void testProcessRenewals_AlreadyProcessed() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(true); // Already processed!

		billingEngineService.processRenewals();

		verify(invoiceRepository, never()).save(any()); // No invoice created
	}

	@Test
	void testProcessRenewals_InvalidPaymentMethod_InactiveStatus() {
		paymentMethod.setStatus(Status.INACTIVE);
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

		billingEngineService.processRenewals();

		assertEquals(Status.PAST_DUE, subscription.getStatus());
	}

	@Test
	void testProcessRenewals_InvalidPaymentMethod_BlankToken() {
		paymentMethod.setGatewayToken("");
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));

		billingEngineService.processRenewals();

		assertEquals(Status.PAST_DUE, subscription.getStatus());
	}

	@Test
	void testProcessRenewals_PaymentMethodNotFound() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

		billingEngineService.processRenewals();

		// Exception caught in outer try, subscription not changed
		verify(invoiceRepository, never()).save(any());
	}

	@Test
	void testProcessRenewals_WithAddons() {
		AddOn addon = AddOn.builder().id(1L).name("HD Streaming").priceMinor(500L).build();
		SubscriptionItem item = new SubscriptionItem();
		item.setAddOn(addon);
		item.setQuantity(2);

		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Arrays.asList(item));

		Invoice invoice = Invoice.builder().id(1L).totalMinor(2000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		// Plan line + addon line = 2 saves
		verify(invoiceLineItemRepository, times(2)).save(any(InvoiceLineItem.class));
	}

	@Test
	void testProcessRenewals_WithCredits_FullyCovered() {
		customer.setCreditBalanceMinor(5000L); // More than plan price

		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Collections.emptyList());

		Invoice invoice = Invoice.builder().id(1L).totalMinor(0L).customer(customer).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));

		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setAmountMinor(1000L);
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(Arrays.asList(planLine));

		billingEngineService.processRenewals();

		// Should NOT create a payment — fully covered by credits
		verify(mockPaymentGateway, never()).charge(anyString(), anyLong(), anyString());
		verify(customerRepository).save(customer); // Customer credit updated
	}

	@Test
	void testProcessRenewals_ZeroAmountPayment() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Collections.emptyList());

		Invoice invoice = Invoice.builder().id(1L).totalMinor(0L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

		billingEngineService.processRenewals();

		// Zero amount → ZERO_AMOUNT path, no gateway charge
		verify(mockPaymentGateway, never()).charge(anyString(), anyLong(), anyString());
	}

	@Test
	void testProcessRenewals_YearlyBillingPeriod() {
		plan.setBillingPeriod(BillingPeriod.YEARLY);
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		// Should advance subscription with yearly period
		verify(subscriptionRepository, times(1)).save(subscription);
	}

	@Test
	void testProcessRenewals_RecalculateTotals_AllLineTypes() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Collections.emptyList());

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).currency("USD").build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setAmountMinor(1000L);
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);

		InvoiceLineItem discountLine = new InvoiceLineItem();
		discountLine.setAmountMinor(-200L);
		discountLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);

		InvoiceLineItem taxLine = new InvoiceLineItem();
		taxLine.setAmountMinor(100L);
		taxLine.setLineType(InvoiceLineItem.LineType.TAX);

		when(invoiceLineItemRepository.findByInvoice_Id(anyLong()))
				.thenReturn(Arrays.asList(planLine, discountLine, taxLine));
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceRepository, atLeast(2)).save(any(Invoice.class));
	}

	@Test
	void testProcessRenewals_NoSubscriptionsDue() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>());
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());

		billingEngineService.processRenewals();

		verify(invoiceRepository, never()).save(any());
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void testProcessRenewals_AddonWithNullAddon() {
		SubscriptionItem item = new SubscriptionItem();
		item.setAddOn(null);

		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Arrays.asList(item));

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceLineItemRepository, times(1)).save(any(InvoiceLineItem.class));
	}

	@Test
	void testProcessRenewals_AddonWithZeroPrice() {
		AddOn addon = AddOn.builder().id(1L).name("Free").priceMinor(0L).build();
		SubscriptionItem item = new SubscriptionItem();
		item.setAddOn(addon);
		item.setQuantity(1);

		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Arrays.asList(item));

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceLineItemRepository, times(1)).save(any(InvoiceLineItem.class));
	}

	@Test
	void testProcessRenewals_AddonWithNullQuantity() {
		AddOn addon = AddOn.builder().id(1L).name("Premium").priceMinor(500L).build();
		SubscriptionItem item = new SubscriptionItem();
		item.setAddOn(addon);
		item.setQuantity(null);

		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Arrays.asList(item));

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1500L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(invoiceLineItemRepository.findByInvoice_Id(anyLong())).thenReturn(new ArrayList<>());
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceLineItemRepository, times(2)).save(any(InvoiceLineItem.class));
	}

	@Test
	void testProcessRenewals_RecalculateWithCreditProrationMetered() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Collections.emptyList());

		Invoice invoice = Invoice.builder().id(1L).totalMinor(1000L).currency("USD").build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setAmountMinor(1000L);
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);

		InvoiceLineItem prorationLine = new InvoiceLineItem();
		prorationLine.setAmountMinor(500L);
		prorationLine.setLineType(InvoiceLineItem.LineType.PRORATION);

		InvoiceLineItem meteredLine = new InvoiceLineItem();
		meteredLine.setAmountMinor(200L);
		meteredLine.setLineType(InvoiceLineItem.LineType.METERED);

		InvoiceLineItem creditLine = new InvoiceLineItem();
		creditLine.setAmountMinor(-300L);
		creditLine.setLineType(InvoiceLineItem.LineType.CREDIT);

		when(invoiceLineItemRepository.findByInvoice_Id(anyLong()))
				.thenReturn(Arrays.asList(planLine, prorationLine, meteredLine, creditLine));
		when(mockPaymentGateway.charge(anyString(), anyLong(), anyString())).thenReturn("gateway_ref");

		billingEngineService.processRenewals();

		verify(invoiceRepository, atLeast(2)).save(any(Invoice.class));
	}

	@Test
	void testProcessRenewals_NegativeTotal_ClampedToZero() {
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);
		when(subscriptionItemRepository.findBySubscription_Id(1L)).thenReturn(Collections.emptyList());

		Invoice invoice = Invoice.builder().id(1L).totalMinor(0L).currency("USD").build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setAmountMinor(100L);
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);

		InvoiceLineItem discountLine = new InvoiceLineItem();
		discountLine.setAmountMinor(-500L);
		discountLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);

		when(invoiceLineItemRepository.findByInvoice_Id(anyLong()))
				.thenReturn(Arrays.asList(planLine, discountLine));

		billingEngineService.processRenewals();

		verify(mockPaymentGateway, never()).charge(anyString(), anyLong(), anyString());
	}

	@Test
	void testProcessRenewals_InvalidPlanAmount() {
		plan.setDefaultPriceMinor(0L);
		when(subscriptionRepository.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(any(), any()))
				.thenReturn(new ArrayList<>(Arrays.asList(subscription)));
		when(subscriptionRepository.findByTrialEndDateAndStatus(any(), any()))
				.thenReturn(new ArrayList<>());
		when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
		when(invoiceRepository.existsBySubscriptionAndBillingReasonAndIssueDate(any(), any(), any()))
				.thenReturn(false);

		Invoice invoice = Invoice.builder().id(1L).totalMinor(0L).build();
		doReturn(invoice).when(invoiceRepository).save(any(Invoice.class));

		billingEngineService.processRenewals();

		verify(mockPaymentGateway, never()).charge(anyString(), anyLong(), anyString());
	}
}
