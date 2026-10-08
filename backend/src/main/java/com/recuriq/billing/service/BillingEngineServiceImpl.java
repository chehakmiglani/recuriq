package com.recuriq.billing.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.InvoiceLineItem;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.SubscriptionItem;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.BillingReason;
import com.recuriq.billing.enums.DunningStatus;
import com.recuriq.billing.enums.ItemType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.DunningRetryLogRepository;
import com.recuriq.billing.repository.InvoiceLineItemRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.PaymentMethodRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.SubscriptionItemRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class BillingEngineServiceImpl implements BillingEngineService {

	private final SubscriptionRepository subscriptionRepository;

	private final InvoiceRepository invoiceRepository;

	private final PaymentMethodRepository paymentMethodRepository;

	private final PaymentRepository paymentRepository;

	private final InvoiceLineItemRepository invoiceLineItemRepository;

	private final SubscriptionItemRepository subscriptionItemRepository;

	private final DunningRetryLogRepository dunningRetryLogRepository;

	private final CustomerRepository customerRepository; // ADDED

	private final MockPaymentGateway mockPaymentGateway;

	@Override

	@Transactional

	public void processRenewals() {
		LocalDate today = LocalDate.now();
		List<Subscription> subscriptions = subscriptionRepository
				.findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(today, Status.ACTIVE);
		List<Subscription> trialSubscriptions = subscriptionRepository
				.findByTrialEndDateAndStatus(today, Status.TRIALING);
		subscriptions.addAll(trialSubscriptions);
		for (Subscription subscription : subscriptions) {
			processRenewalForSubscription(subscription, today);
		}
	}

	private void processRenewalForSubscription(Subscription subscription, LocalDate today) {
		try {
			PaymentMethod paymentMethod = validateSubscription(subscription);
			boolean alreadyProcessed = invoiceRepository
					.existsBySubscriptionAndBillingReasonAndIssueDate(subscription,
							BillingReason.SUBSCRIPTION_CYCLE, today);
			if (alreadyProcessed) {
				return;
			}
			Invoice invoice = createInvoice(subscription, today);
			addPlanLine(invoice, subscription);
			addAddonLines(invoice, subscription);

			// Apply credits and check if invoice becomes zero
			boolean isFullyCoveredByCredits = applyCustomerCredits(invoice);
			recalculateInvoiceTotals(invoice);

			// If fully covered by credits, no payment needed
			if (isFullyCoveredByCredits || invoice.getTotalMinor() == 0L) {
				// Mark invoice as paid without payment
				invoice.setStatus(Status.PAID);
				invoice.setBalanceMinor(0L);
				invoiceRepository.save(invoice);

				// Advance subscription
				advanceSubscription(subscription);
				return; // Skip payment processing
			}
			Payment payment = createPayment(invoice, paymentMethod);
			processPayment(payment, paymentMethod, invoice, subscription);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private PaymentMethod validateSubscription(Subscription subscription) {
		if (subscription.getPaymentMethodId() == null) {
			subscription.setStatus(Status.PAST_DUE);
			subscriptionRepository.save(subscription);
			throw CustomException.badRequest("Payment method missing");
		}
		PaymentMethod paymentMethod = paymentMethodRepository
				.findById(subscription.getPaymentMethodId())
				.orElseThrow(() -> CustomException.notFound("Payment method not found"));
		if (paymentMethod.getStatus() != Status.ACTIVE ||
				paymentMethod.getGatewayToken() == null ||
				paymentMethod.getGatewayToken().isBlank()) {
			subscription.setStatus(Status.PAST_DUE);
			subscriptionRepository.save(subscription);
			throw CustomException.badRequest("Invalid payment method");
		}
		return paymentMethod;
	}

	private Invoice createInvoice(Subscription subscription, LocalDate today) {
		Invoice invoice = Invoice.builder()
				.invoiceNumber("INV-" + System.currentTimeMillis())
				.customer(subscription.getCustomer())
				.subscription(subscription)
				.status(Status.OPEN)
				.billingReason(BillingReason.SUBSCRIPTION_CYCLE)
				.issueDate(today)
				.dueDate(today)
				.subtotalMinor(0L)
				.discountMinor(0L)
				.taxMinor(0L)
				.totalMinor(0L)
				.balanceMinor(0L)
				.currency(subscription.getCurrency())
				.idempotencyKey("renewal-" + subscription.getId() + "-" + today)
				.build();
		return invoiceRepository.save(invoice);
	}

	private void addPlanLine(Invoice invoice, Subscription subscription) {
		SubscriptionItem planItem = subscriptionItemRepository.findBySubscription_IdAndItemType(subscription.getId(), ItemType.PLAN);
		Long amount = (planItem != null) ? planItem.getUnitPriceMinor() : null;
		if (amount == null || amount <= 0) {
			amount = subscription.getPlan().getDefaultPriceMinor();
		}
		if (amount == null || amount <= 0) {
			throw CustomException.badRequest("Invalid plan amount");
		}
		InvoiceLineItem line = new InvoiceLineItem();
		line.setInvoice(invoice);
		line.setDescription(subscription.getPlan().getName() + " Renewal");
		line.setQuantity(1);
		line.setUnitPriceMinor(amount);
		line.setAmountMinor(amount);
		line.setLineType(InvoiceLineItem.LineType.PLAN);
		invoiceLineItemRepository.save(line);
	}

	private void addAddonLines(Invoice invoice, Subscription subscription) {
		List<SubscriptionItem> items = subscriptionItemRepository
				.findBySubscription_Id(subscription.getId());
		if (items == null || items.isEmpty()) {
			return;
		}
		for (SubscriptionItem item : items) {
			if (item.getAddOn() != null) {
				Long price = item.getAddOn().getPriceMinor();
				if (price != null && price > 0) {
					int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
					long total = price * quantity;
					InvoiceLineItem line = new InvoiceLineItem();
					line.setInvoice(invoice);
					line.setDescription(item.getAddOn().getName());
					line.setQuantity(quantity);
					line.setUnitPriceMinor(price);
					line.setAmountMinor(total);
					line.setLineType(InvoiceLineItem.LineType.ADDON);
					invoiceLineItemRepository.save(line);
				}
			}
		}
	}

	// FIXED: Returns boolean indicating if invoice is fully covered by credits

	private boolean applyCustomerCredits(Invoice invoice) {
		Customer customer = invoice.getCustomer();
		if (customer == null || customer.getCreditBalanceMinor() == null || customer.getCreditBalanceMinor() <= 0) {
			return false;
		}
		List<InvoiceLineItem> lines = invoiceLineItemRepository.findByInvoice_Id(invoice.getId());
		long currentAmount = 0L;
		for (InvoiceLineItem line : lines) {
			currentAmount += line.getAmountMinor();
		}
		long usableCredit = Math.min(currentAmount, customer.getCreditBalanceMinor());
		if (usableCredit <= 0) {
			return false;
		}
		InvoiceLineItem creditLine = new InvoiceLineItem();
		creditLine.setInvoice(invoice);
		creditLine.setDescription("Customer Credit Applied");
		creditLine.setQuantity(1);
		creditLine.setUnitPriceMinor(-usableCredit);
		creditLine.setAmountMinor(-usableCredit);
		creditLine.setLineType(InvoiceLineItem.LineType.CREDIT);
		invoiceLineItemRepository.save(creditLine);

		// FIXED: Save customer with updated credit balance
		customer.setCreditBalanceMinor(customer.getCreditBalanceMinor() - usableCredit);
		customerRepository.save(customer); // CRITICAL FIX

		// Return true if invoice is fully paid by credits
		return (currentAmount - usableCredit) <= 0;
	}

	private void recalculateInvoiceTotals(Invoice invoice) {
		List<InvoiceLineItem> lines = invoiceLineItemRepository.findByInvoice_Id(invoice.getId());
		long subtotal = 0L;
		long discount = 0L;
		long tax = 0L;
		long credits = 0L;
		for (InvoiceLineItem line : lines) {
			switch (line.getLineType()) {
				case PLAN, ADDON, METERED, PRORATION -> subtotal += line.getAmountMinor();
				case DISCOUNT -> discount += Math.abs(line.getAmountMinor());
				case TAX -> tax += line.getAmountMinor();
				case CREDIT -> credits += Math.abs(line.getAmountMinor());
				default -> {
					// No actions needed for other line types (e.g. unknown or unsupported types)
				}
			}
		}
		long total = subtotal - discount - credits + tax;
		if (total < 0) {
			total = 0L;
		}
		invoice.setSubtotalMinor(subtotal);
		invoice.setDiscountMinor(0L); // FIXED: Don't add credits to discount
		invoice.setTaxMinor(tax);
		invoice.setTotalMinor(total);
		invoice.setBalanceMinor(total);
		invoiceRepository.save(invoice);
	}

	private Payment createPayment(Invoice invoice, PaymentMethod paymentMethod) {
		Payment payment = new Payment();
		payment.setInvoice(invoice);
		payment.setPaymentMethod(paymentMethod);
		payment.setIdempotencyKey("payment-" + invoice.getId());
		payment.setAmountMinor(invoice.getTotalMinor());
		payment.setCurrency(invoice.getCurrency());
		payment.setAttemptNo(1);
		payment.setStatus(Status.PENDING);
		payment.setResponseCode("PENDING");
		return paymentRepository.save(payment);
	}

	private void processPayment(Payment payment, PaymentMethod paymentMethod,
			Invoice invoice, Subscription subscription) {
		try {

			// FIXED: Guard against zero-amount charges
			if (payment.getAmountMinor() == 0L) {
				payment.setStatus(Status.SUCCESS);
				payment.setResponseCode("ZERO_AMOUNT");
				payment.setGatewayRef("NO_CHARGE_" + System.currentTimeMillis());
				paymentRepository.save(payment);
				invoice.setStatus(Status.PAID);
				invoice.setBalanceMinor(0L);
				invoiceRepository.save(invoice);
				advanceSubscription(subscription);
				return;
			}
			String gatewayRef = mockPaymentGateway.charge(
					paymentMethod.getGatewayToken(),
					payment.getAmountMinor(),
					invoice.getCurrency()
			);
			payment.setGatewayRef(gatewayRef);
			payment.setStatus(Status.SUCCESS);
			payment.setResponseCode("SUCCESS");
			paymentRepository.save(payment);
			invoice.setStatus(Status.PAID);
			invoice.setBalanceMinor(0L);
			invoiceRepository.save(invoice);
			advanceSubscription(subscription);
		} catch (CustomException ex) {
			payment.setStatus(Status.FAILED);
			payment.setResponseCode("FAILED");
			payment.setFailureReason(ex.getMessage());
			paymentRepository.save(payment);
			invoice.setStatus(Status.OPEN);
			invoice.setBalanceMinor(invoice.getTotalMinor());
			invoiceRepository.save(invoice);
			subscription.setStatus(Status.PAST_DUE);
			subscriptionRepository.save(subscription);
			createRetryLog(invoice, payment, 1);
		}
	}

	private void advanceSubscription(Subscription subscription) {
		LocalDate start;
		if (subscription.getStatus() == Status.TRIALING) {
			start = LocalDate.now();
		} else {
			start = subscription.getCurrentPeriodEnd().plusDays(1);
		}
		LocalDate end;
		BillingPeriod billingPeriod = subscription.getPlan().getBillingPeriod();
		switch (billingPeriod) {
		case MONTHLY:
			end = start.plusMonths(1).minusDays(1);
			break;
		case YEARLY:
			end = start.plusYears(1).minusDays(1);
			break;
		default:
			throw CustomException.badRequest("Unsupported billing period");
		}
		subscription.setCurrentPeriodStart(start);
		subscription.setCurrentPeriodEnd(end);
		subscription.setTrialEndDate(null);
		subscription.setStatus(Status.ACTIVE);
		subscriptionRepository.save(subscription);
	}

	private void createRetryLog(Invoice invoice, Payment payment, int attemptNo) {
		DunningRetryLog log = new DunningRetryLog();
		log.setInvoice(invoice);
		log.setPayment(payment);
		log.setAttemptNo(attemptNo);
		log.setScheduledAt(LocalDateTime.now().plusDays(1));
		log.setStatus(DunningStatus.SCHEDULED);
		dunningRetryLogRepository.save(log);
	}
}
