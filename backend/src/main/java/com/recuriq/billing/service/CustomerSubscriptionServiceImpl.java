package com.recuriq.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.dto.customer.MeteredUsageDTO;
import com.recuriq.billing.dto.customer.SubscriptionCompletionRequest;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.customer.SubscriptionItemDTO;
import com.recuriq.billing.dto.customer.SubscriptionResponse;
import com.recuriq.billing.dto.customer.UsageRecordDTO;
import com.recuriq.billing.entity.AddOn;
import com.recuriq.billing.entity.CancellationRequest;
import com.recuriq.billing.entity.Coupon;
import com.recuriq.billing.entity.CreditNote;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.InvoiceLineItem;
import com.recuriq.billing.entity.MeteredComponent;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.entity.PriceBookEntry;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.entity.SubscriptionCoupon;
import com.recuriq.billing.entity.SubscriptionItem;
import com.recuriq.billing.entity.TaxRate;
import com.recuriq.billing.entity.UsageRecord;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.BillingReason;
import com.recuriq.billing.enums.CancellationRequestStatus;
import com.recuriq.billing.enums.CouponType;
import com.recuriq.billing.enums.Duration;
import com.recuriq.billing.enums.ItemType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.TaxMode;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.AddOnRepository;
import com.recuriq.billing.repository.CancellationRequestRepository;
import com.recuriq.billing.repository.CreditNoteRepository;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.repository.InvoiceLineItemRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.MeteredComponentRepository;
import com.recuriq.billing.repository.PaymentMethodRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.PlanRepository;
import com.recuriq.billing.repository.PriceBookEntryRepository;
import com.recuriq.billing.repository.SubscriptionCouponRepository;
import com.recuriq.billing.repository.SubscriptionItemRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import com.recuriq.billing.repository.TaxRateRepository;
import com.recuriq.billing.repository.UsageRecordRepository;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.request.CancellationRequestInput;
import com.recuriq.billing.request.CreateSubscriptionRequest;
import com.recuriq.billing.request.PauseSubscriptionRequest;
import com.recuriq.billing.request.UpgradeSubscriptionRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerSubscriptionServiceImpl implements CustomerSubscriptionService {

	private static final String NO_GATEWAY_TOKEN_MSG = "Payment method has no gateway token";
	private static final String STARTS_AFTER_TRIAL = " (Starts after trial)";
	private static final String ADDON_PREFIX = "Add-on: ";
	private static final String UNKNOWN = "Unknown";
	private static final String DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss";
	private static final String ENTITY_SUBSCRIPTION = "Subscription";

	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionItemRepository subscriptionItemRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final AddOnRepository addOnRepository;
	private final MeteredComponentRepository meteredComponentRepository;
	private final UsageRecordRepository usageRecordRepository;
	private final PaymentMethodRepository paymentMethodRepository;
	private final InvoiceRepository invoiceRepository;
	private final InvoiceLineItemRepository invoiceLineItemRepository;
	private final PaymentRepository paymentRepository;
	private final SubscriptionCouponRepository subscriptionCouponRepository;
	private final PriceBookEntryRepository priceBookEntryRepository;
	private final MockPaymentGateway mockPaymentGateway;
	private final TaxRateRepository taxRateRepository;
	private final CreditNoteRepository creditNoteRepository;
	private final SubscriptionFlowService subscriptionFlowService;
	private final CancellationRequestRepository cancellationRequestRepository;
	private final AuditLoggingService auditLoggingService;

	public SubscriptionDTO getCurrentSubscription(String email) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = subscriptionRepository
				.findByCustomer_IdAndStatusIn(customer.getId(),
						List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED, Status.ON_HOLD))
				.stream().findFirst().orElse(null);

		if (subscription == null)
			return null;
		return mapToSubscriptionDTO(subscription);
	}

	@Transactional
	public SubscriptionDTO createSubscription(String email, CreateSubscriptionRequest request) {
		Customer customer = getCustomerByEmail(email);

		if (!subscriptionRepository.findByCustomer_IdAndStatusIn(customer.getId(),
				List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE)).isEmpty()) {
			throw CustomException.conflict("Customer already has an active subscription");
		}

		Plan plan = planRepository.findById(request.getPlanId())
				.orElseThrow(() -> CustomException.notFound("Plan not found"));

		SubscriptionCompletionRequest compReq = new SubscriptionCompletionRequest();
		compReq.setPlanId(request.getPlanId());
		compReq.setPaymentMethodId(request.getPaymentMethodId());
		compReq.setBillingPeriod(plan.getBillingPeriod());
		compReq.setCouponCode(request.getCouponCode());

		SubscriptionResponse resp = subscriptionFlowService.completeSubscription(customer.getId(), compReq);

		Subscription subscription = subscriptionRepository.findById(resp.getSubscriptionId())
				.orElseThrow(() -> CustomException.notFound("Subscription not found after creation"));

		return mapToSubscriptionDTO(subscription);
	}

	@Transactional
	public SubscriptionDTO upgradeSubscription(String email, UpgradeSubscriptionRequest request) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());
		Plan oldPlan = subscription.getPlan();

		Plan newPlan = planRepository.findById(request.getPlanId())
				.orElseThrow(() -> CustomException.notFound("Plan not found"));

		if (oldPlan.getId().equals(newPlan.getId())) {
			throw CustomException.badRequest("Already on this plan");
		}

		boolean isTrialing = subscription.getStatus() == Status.TRIALING;
		Optional<SubscriptionCoupon> scOpt = findValidActiveSubscriptionCoupon(subscription.getId());
		List<SubscriptionItem> items = subscriptionItemRepository.findBySubscription_Id(subscription.getId());

		if (isTrialing) {
			return upgradeTrialingSubscription(
					new TrialingUpgradeContext(customer, subscription, oldPlan, newPlan, items, scOpt));
		} else {
			return upgradeActiveSubscription(
					new ActiveUpgradeContext(email, customer, subscription, oldPlan, newPlan, items, scOpt));
		}
	}

	private SubscriptionDTO upgradeTrialingSubscription(TrialingUpgradeContext ctx) {
		Customer customer = ctx.customer;
		Subscription subscription = ctx.subscription;
		Plan oldPlan = ctx.oldPlan;
		Plan newPlan = ctx.newPlan;
		List<SubscriptionItem> items = ctx.items;
		Optional<SubscriptionCoupon> scOpt = ctx.scOpt;

		LocalDate today = LocalDate.now();
		long daysUsed = ChronoUnit.DAYS.between(subscription.getStartDate(), today);
		if (daysUsed < 0)
			daysUsed = 0;

		long newTrialDays = Math.max(0, newPlan.getTrialDays() - daysUsed);
		LocalDate newTrialEndDate = today.plusDays(newTrialDays);

		subscription.setPlan(newPlan);
		subscription.setTrialEndDate(newTrialEndDate);
		if (newTrialDays == 0) {
			subscription.setStatus(Status.ACTIVE);
		}

		if (newPlan.getBillingPeriod() == BillingPeriod.MONTHLY) {
			subscription.setCurrentPeriodStart(newTrialEndDate);
			subscription.setCurrentPeriodEnd(newTrialEndDate.plusMonths(1));
		} else {
			subscription.setCurrentPeriodStart(newTrialEndDate);
			subscription.setCurrentPeriodEnd(newTrialEndDate.plusYears(1));
		}
		subscription.setUpdatedAt(LocalDateTime.now());
		subscriptionRepository.save(subscription);

		// Update the plan subscription item
		SubscriptionItem planItem = subscriptionItemRepository.findBySubscription_IdAndItemType(subscription.getId(),
				ItemType.PLAN);

		if (planItem == null) {
			planItem = new SubscriptionItem();
			planItem.setSubscription(subscription);
			planItem.setItemType(ItemType.PLAN);
			planItem.setQuantity(1);
			planItem.setTaxMode(newPlan.getTaxMode());
		}

		planItem.setPlan(newPlan);
		Long basePrice = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(newPlan.getId(), customer.getCountry(), subscription.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(newPlan.getDefaultPriceMinor() != null ? newPlan.getDefaultPriceMinor() : 0L);
		planItem.setUnitPriceMinor(basePrice);
		subscriptionItemRepository.save(planItem);

		// Update the OPEN trial invoice to show the new plan + tax + coupon + addons
		Optional<Invoice> openInvoiceOpt = invoiceRepository.findBySubscription_IdAndStatus(subscription.getId(),
				Status.OPEN);
		if (openInvoiceOpt.isPresent()) {
			rebuildTrialInvoice(openInvoiceOpt.get(), customer, subscription, newPlan, newTrialDays, items, scOpt);
		}

		auditLoggingService.logAction("UPGRADE_SUBSCRIPTION", ENTITY_SUBSCRIPTION, subscription.getId(), oldPlan,
				newPlan);
		return mapToSubscriptionDTO(subscription);
	}

	private static class ProrationLineItemsParam {
		Invoice invoice;
		Plan newPlan;
		long newPriceMinor;
		long unusedCredit;
		long remainingDays;
		long newTaxMinor;
		long newDiscountMinor;
		Coupon activeCoupon;
		SubscriptionCoupon activeSubscriptionCoupon;
		String oldPlanName;
	}

	private static class ProrationDetails {
		long newPriceMinor;
		long newDiscountMinor;
		Coupon activeCoupon;
		SubscriptionCoupon activeSubscriptionCoupon;
		@SuppressWarnings("unused")
		long newAddonTotal;
		long newTaxMinor;
		long prorationAmount;
		long headerSubtotal;
	}

	private static class TrialingUpgradeContext {
		final Customer customer;
		final Subscription subscription;
		final Plan oldPlan;
		final Plan newPlan;
		final List<SubscriptionItem> items;
		final Optional<SubscriptionCoupon> scOpt;

		TrialingUpgradeContext(Customer customer, Subscription subscription, Plan oldPlan, Plan newPlan,
				List<SubscriptionItem> items, Optional<SubscriptionCoupon> scOpt) {
			this.customer = customer;
			this.subscription = subscription;
			this.oldPlan = oldPlan;
			this.newPlan = newPlan;
			this.items = items;
			this.scOpt = scOpt;
		}
	}

	private static class ActiveUpgradeContext {
		final String email;
		final Customer customer;
		final Subscription subscription;
		final Plan oldPlan;
		final Plan newPlan;
		final List<SubscriptionItem> items;
		final Optional<SubscriptionCoupon> scOpt;

		ActiveUpgradeContext(String email, Customer customer, Subscription subscription, Plan oldPlan, Plan newPlan,
				List<SubscriptionItem> items, Optional<SubscriptionCoupon> scOpt) {
			this.email = email;
			this.customer = customer;
			this.subscription = subscription;
			this.oldPlan = oldPlan;
			this.newPlan = newPlan;
			this.items = items;
			this.scOpt = scOpt;
		}
	}

	private long resolveDiscountMinor(Optional<SubscriptionCoupon> scOpt, long basePrice, String subscriptionCurrency) {
		if (scOpt.isEmpty()) {
			return 0L;
		}
		Coupon coupon = scOpt.get().getCoupon();
		long discountMinor = 0L;
		if (coupon.getType() == CouponType.PERCENT) {
			discountMinor = basePrice * coupon.getAmount() / 100;
		} else if (coupon.getCurrency() == null || coupon.getCurrency().equals(subscriptionCurrency)) {
			discountMinor = coupon.getAmount();
		}
		return Math.min(discountMinor, basePrice);
	}

	private Optional<SubscriptionCoupon> findValidActiveSubscriptionCoupon(Long subscriptionId) {
		Optional<SubscriptionCoupon> scOpt = subscriptionCouponRepository.findBySubscription_IdAndStatus(subscriptionId,
				Status.ACTIVE);
		if (scOpt.isPresent()) {
			SubscriptionCoupon sc = scOpt.get();
			if (sc.getExpiresAt() != null && sc.getExpiresAt().isBefore(LocalDateTime.now())) {
				sc.setStatus(Status.CANCELED);
				subscriptionCouponRepository.save(sc);
				return Optional.empty();
			}
		}
		return scOpt;
	}

	private void consumeOneTimeSubscriptionCoupon(SubscriptionCoupon subscriptionCoupon) {
		if (subscriptionCoupon == null || subscriptionCoupon.getCoupon() == null) {
			return;
		}
		if (subscriptionCoupon.getCoupon().getDuration() == Duration.ONCE) {
			subscriptionCoupon.setStatus(Status.CANCELED);
			subscriptionCouponRepository.save(subscriptionCoupon);
		}
	}

	private long calculateUnusedCredit(Customer customer, Subscription subscription, Plan oldPlan,
			List<SubscriptionItem> items, Optional<SubscriptionCoupon> scOpt, LocalDate today) {
		LocalDate periodStart = subscription.getCurrentPeriodStart();
		LocalDate periodEnd = subscription.getCurrentPeriodEnd();

		long totalDaysInPeriod = ChronoUnit.DAYS.between(periodStart, periodEnd);
		long remainingDays = ChronoUnit.DAYS.between(today, periodEnd);

		if (totalDaysInPeriod <= 0)
			totalDaysInPeriod = 1;
		if (remainingDays <= 0)
			remainingDays = 0;

		long oldTotalPaid = calculateOldTotalPaid(customer, oldPlan, items, scOpt);
		return oldTotalPaid * remainingDays / totalDaysInPeriod;
	}

	private ProrationDetails calculateProrationDetails(Customer customer, Plan newPlan, List<SubscriptionItem> items,
			Optional<SubscriptionCoupon> scOpt, long unusedCredit) {
		long newPriceMinor = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(newPlan.getId(), customer.getCountry(), customer.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(newPlan.getDefaultPriceMinor() != null ? newPlan.getDefaultPriceMinor() : 0L);

		Optional<SubscriptionCoupon> applicableScOpt = scOpt.filter(sc -> 
			sc.getCoupon().getDuration() != Duration.ONCE
		);
		long newDiscountMinor = resolveDiscountMinor(applicableScOpt, newPriceMinor, customer.getCurrency());
		Coupon activeCoupon = applicableScOpt.map(SubscriptionCoupon::getCoupon).orElse(null);
		long newAddonTotal = items.stream().filter(i -> i.getItemType() == ItemType.ADDON)
				.mapToLong(i -> i.getUnitPriceMinor() * i.getQuantity()).sum();

		long newSubtotal = newPriceMinor + newAddonTotal - newDiscountMinor;
		if (newSubtotal < 0)
			newSubtotal = 0;
		long newTaxMinor = calculateTaxMinor(newSubtotal, customer.getCountry(), newPlan.getTaxMode());
		long newTotalCost = (newPlan.getTaxMode() == TaxMode.INCLUSIVE) ? newSubtotal : newSubtotal + newTaxMinor;

		long prorationAmount = newTotalCost - unusedCredit;

		long headerSubtotal = (newPlan.getTaxMode() == TaxMode.INCLUSIVE)
				? (newPriceMinor + newAddonTotal - unusedCredit - newTaxMinor)
				: (newPriceMinor + newAddonTotal - unusedCredit);

		ProrationDetails details = new ProrationDetails();
		details.newPriceMinor = newPriceMinor;
		details.newDiscountMinor = newDiscountMinor;
		details.activeCoupon = activeCoupon;
		details.activeSubscriptionCoupon = applicableScOpt.orElse(null);
		details.newAddonTotal = newAddonTotal;
		details.newTaxMinor = newTaxMinor;
		details.prorationAmount = prorationAmount;
		details.headerSubtotal = headerSubtotal;
		return details;
	}

	private SubscriptionDTO upgradeActiveSubscription(ActiveUpgradeContext ctx) {
		String email = ctx.email;
		Customer customer = ctx.customer;
		Subscription subscription = ctx.subscription;
		Plan oldPlan = ctx.oldPlan;
		Plan newPlan = ctx.newPlan;
		List<SubscriptionItem> items = ctx.items;
		Optional<SubscriptionCoupon> scOpt = ctx.scOpt;

		LocalDate today = LocalDate.now();
		long unusedCredit = calculateUnusedCredit(customer, subscription, oldPlan, items, scOpt, today);
		ProrationDetails details = calculateProrationDetails(customer, newPlan, items, scOpt, unusedCredit);

		Invoice invoice = createProrationInvoice(customer, subscription, today, details.headerSubtotal,
				details.newTaxMinor);

		if (details.prorationAmount > 0) {
			// Upgrade: charge the difference
			invoice.setStatus(Status.PAID);
			invoice.setDiscountMinor(0L);
			invoice.setTotalMinor(details.prorationAmount);
			invoice.setBalanceMinor(0L);
			invoiceRepository.save(invoice);
			chargeUpgradeAmount(customer, subscription, invoice, details.prorationAmount);
		} else if (details.prorationAmount < 0) {
			// Downgrade: credit the difference to customer account
			handleDowngradeCredit(email, oldPlan, newPlan, invoice, details.prorationAmount);
		} else {
			// Zero difference
			invoice.setStatus(Status.PAID);
			invoice.setDiscountMinor(0L);
			invoice.setTotalMinor(0L);
			invoice.setBalanceMinor(0L);
			invoiceRepository.save(invoice);
		}

		long remainingDays = ChronoUnit.DAYS.between(today, subscription.getCurrentPeriodEnd());
		if (remainingDays <= 0)
			remainingDays = 0;

		ProrationLineItemsParam lineItemsParam = new ProrationLineItemsParam();
		lineItemsParam.invoice = invoice;
		lineItemsParam.newPlan = newPlan;
		lineItemsParam.newPriceMinor = details.newPriceMinor;
		lineItemsParam.unusedCredit = unusedCredit;
		lineItemsParam.remainingDays = remainingDays;
		lineItemsParam.newTaxMinor = details.newTaxMinor;
		lineItemsParam.newDiscountMinor = details.newDiscountMinor;
		lineItemsParam.activeCoupon = details.activeCoupon;
		lineItemsParam.activeSubscriptionCoupon = details.activeSubscriptionCoupon;
		lineItemsParam.oldPlanName = oldPlan.getName();
		saveProratedLineItems(lineItemsParam);

		// Reset subscription period and save
		subscription.setPlan(newPlan);
		subscription.setCurrentPeriodStart(today);
		subscription.setCurrentPeriodEnd(
				newPlan.getBillingPeriod() == BillingPeriod.MONTHLY ? today.plusMonths(1) : today.plusYears(1));
		subscription.setUpdatedAt(LocalDateTime.now());
		subscriptionRepository.save(subscription);

		// Update the plan subscription item
		SubscriptionItem planItem = subscriptionItemRepository.findBySubscription_IdAndItemType(subscription.getId(),
				ItemType.PLAN);

		if (planItem == null) {
			planItem = new SubscriptionItem();
			planItem.setSubscription(subscription);
			planItem.setItemType(ItemType.PLAN);
			planItem.setQuantity(1);
			planItem.setTaxMode(newPlan.getTaxMode());
		}

		planItem.setPlan(newPlan);
		Long basePrice = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(newPlan.getId(), customer.getCountry(), subscription.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(newPlan.getDefaultPriceMinor() != null ? newPlan.getDefaultPriceMinor() : 0L);
		planItem.setUnitPriceMinor(basePrice);
		subscriptionItemRepository.save(planItem);

		auditLoggingService.logAction("UPGRADE_SUBSCRIPTION", ENTITY_SUBSCRIPTION, subscription.getId(), oldPlan,
				newPlan);
		scOpt.ifPresent(this::consumeOneTimeSubscriptionCoupon);
		return mapToSubscriptionDTO(subscription);
	}

	@Transactional
	public CancellationResponse cancelSubscription(String email, boolean atPeriodEnd) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());

		if (atPeriodEnd) {
			subscription.setCancelAtPeriodEnd(true);
			subscriptionRepository.save(subscription);

			auditLoggingService.logAction("CANCEL_SUBSCRIPTION", ENTITY_SUBSCRIPTION, subscription.getId(), null,
					subscription);
			return new CancellationResponse(false, 0, customer.getCurrency(), null, null,
					"Subscription will be canceled at the end of the current billing period.");
		} else {
			subscription.setStatus(Status.CANCELED);
			subscription.setCanceledAt(LocalDateTime.now());
			subscriptionRepository.save(subscription);

			List<Invoice> invoices = invoiceRepository.findByCustomer_IdOrderByIssueDateDesc(customer.getId());
			voidOpenInvoices(subscription.getId(), invoices);
			Invoice lastPaidInvoice = findLastPaidInvoice(subscription.getId(), invoices);
			CancellationResponse response = processRefundIfApplicable(email, customer, subscription, lastPaidInvoice);
			updateCustomerStatusIfNoActiveSubscriptions(customer);

			auditLoggingService.logAction("CANCEL_SUBSCRIPTION", ENTITY_SUBSCRIPTION, subscription.getId(), null,
					subscription);
			return response;
		}
	}

	private void voidOpenInvoices(Long subscriptionId, List<Invoice> invoices) {
		invoices.stream().filter(inv -> inv.getSubscription() != null
				&& inv.getSubscription().getId().equals(subscriptionId) && inv.getStatus() == Status.OPEN)
				.forEach(openInv -> {
					openInv.setStatus(Status.VOID);
					invoiceRepository.save(openInv);
				});
	}

	private Invoice findLastPaidInvoice(Long subscriptionId, List<Invoice> invoices) {
		return invoices
				.stream().filter(inv -> inv.getSubscription() != null
						&& inv.getSubscription().getId().equals(subscriptionId) && inv.getStatus() == Status.PAID)
				.findFirst().orElse(null);
	}

	private CancellationResponse processRefundIfApplicable(String email, Customer customer, Subscription subscription,
			Invoice lastPaidInvoice) {
		if (lastPaidInvoice == null) {
			return new CancellationResponse(false, 0, customer.getCurrency(), null, null,
					"Subscription canceled. No refund applicable.");
		}

		LocalDate today = LocalDate.now();
		long totalDays = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(),
				subscription.getCurrentPeriodEnd());
		long remainingDays = ChronoUnit.DAYS.between(today, subscription.getCurrentPeriodEnd());

		if (remainingDays <= 0 || totalDays <= 0) {
			return new CancellationResponse(false, 0, customer.getCurrency(), null, null,
					"Subscription canceled. No refund applicable.");
		}

		long refundAmountMinor = (lastPaidInvoice.getTotalMinor() * remainingDays) / totalDays;

		User currentUser = userRepository.findByEmail(email)
				.orElseThrow(() -> CustomException.notFound("User not found."));

		// Issue mock refund via payment gateway
		Payment originalPayment = paymentRepository.findByInvoice_Id(lastPaidInvoice.getId()).stream()
				.filter(p -> p.getStatus() == Status.SUCCESS).findFirst().orElse(null);
		String refundGatewayRef = mockPaymentGateway.refund(
				originalPayment != null ? originalPayment.getGatewayRef() : "unknown", refundAmountMinor,
				customer.getCurrency());

		// Create credit note for audit trail
		CreditNote creditNote = new CreditNote();
		String cnNumber = "CN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN));
		creditNote.setCreditNoteNumber(cnNumber);
		creditNote.setInvoice(lastPaidInvoice);
		creditNote.setAmountMinor(refundAmountMinor);
		creditNote.setReason("Mid-cycle cancellation refund for " + subscription.getPlan().getName());
		creditNote.setStatus(Status.APPLIED);
		creditNote.setCreatedBy(currentUser);
		creditNoteRepository.save(creditNote);

		return new CancellationResponse(true, refundAmountMinor, customer.getCurrency(), refundGatewayRef, cnNumber,
				"Refund of " + refundAmountMinor + " minor units processed successfully.");
	}

	private void updateCustomerStatusIfNoActiveSubscriptions(Customer customer) {
		List<Subscription> otherSubs = subscriptionRepository.findByCustomer_Id(customer.getId());
		boolean hasActive = otherSubs.stream().anyMatch(s -> s.getStatus() == Status.ACTIVE
				|| s.getStatus() == Status.TRIALING || s.getStatus() == Status.PAST_DUE);

		if (!hasActive) {
			customer.setStatus(Status.INACTIVE);
			customerRepository.save(customer);
		}
	}

	@Transactional
	public SubscriptionDTO pauseSubscription(String email, PauseSubscriptionRequest request) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());

		LocalDate pausedFrom = LocalDate.now();
		LocalDate pausedTo = LocalDate.parse(request.getPausedTo());
		if (!pausedTo.isAfter(pausedFrom)) {
			throw CustomException.badRequest("Pause end date must be in the future");
		}

		long pauseDays = ChronoUnit.DAYS.between(pausedFrom, pausedTo);
		if (pauseDays <= 0) {
			throw CustomException.badRequest("Pause duration must be at least one day");
		}

		if (subscription.getStatus() == Status.TRIALING) {
			if (subscription.getTrialEndDate() != null) {
				subscription.setTrialEndDate(subscription.getTrialEndDate().plusDays(pauseDays));
			}
			subscription.setCurrentPeriodStart(subscription.getCurrentPeriodStart().plusDays(pauseDays));
			subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(pauseDays));
		} else {
			subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(pauseDays));
		}

		subscription.setStatus(Status.PAUSED);
		subscription.setPausedFrom(pausedFrom);
		subscription.setPausedTo(pausedTo);
		subscriptionRepository.save(subscription);

		auditLoggingService.logAction("PAUSE_SUBSCRIPTION", ENTITY_SUBSCRIPTION, subscription.getId(), null,
			subscription);
		return mapToSubscriptionDTO(subscription);
	}

	@Transactional
	public SubscriptionDTO resumeSubscription(String email) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = subscriptionRepository.findByCustomer_IdAndStatus(customer.getId(), Status.PAUSED)
				.orElseThrow(() -> CustomException.notFound("No paused subscription found"));

		LocalDate pausedFrom = subscription.getPausedFrom();
		LocalDate pausedTo = subscription.getPausedTo();
		if (pausedFrom != null && pausedTo != null) {
			long plannedPauseDays = ChronoUnit.DAYS.between(pausedFrom, pausedTo);
			long actualPauseDays = ChronoUnit.DAYS.between(pausedFrom, LocalDate.now());
			long adjustmentDays = actualPauseDays - plannedPauseDays;
			if (adjustmentDays != 0) {
				adjustSubscriptionDates(subscription, pausedFrom, adjustmentDays);
			}
		}

		if (subscription.getTrialEndDate() != null && LocalDate.now().isBefore(subscription.getTrialEndDate())) {
			subscription.setStatus(Status.TRIALING);
		} else {
			subscription.setStatus(Status.ACTIVE);
		}

		subscription.setPausedFrom(null);
		subscription.setPausedTo(null);
		subscriptionRepository.save(subscription);

		return mapToSubscriptionDTO(subscription);
	}

	private void adjustSubscriptionDates(Subscription subscription, LocalDate pausedFrom, long adjustmentDays) {
		if (subscription.getStatus() == Status.PAUSED && subscription.getTrialEndDate() != null
			&& pausedFrom.isBefore(subscription.getTrialEndDate())) {
			subscription.setTrialEndDate(subscription.getTrialEndDate().plusDays(adjustmentDays));
			subscription.setCurrentPeriodStart(subscription.getCurrentPeriodStart().plusDays(adjustmentDays));
			subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(adjustmentDays));
		} else {
			subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(adjustmentDays));
		}
	}

	@Transactional
	public SubscriptionDTO addAddOn(String email, Long addonId) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());
		AddOn addOn = addOnRepository.findById(addonId).orElseThrow(() -> CustomException.notFound("Add-on not found"));

		// Compatibility check: Monthly add-ons only for monthly plans, etc.
		if (addOn.getBillingPeriod() != subscription.getPlan().getBillingPeriod()) {
			throw CustomException.badRequest("Add-on billing period (" + addOn.getBillingPeriod()
					+ ") must match your plan's billing period (" + subscription.getPlan().getBillingPeriod() + ")");
		}

		// Check if already added
		SubscriptionItem existing = subscriptionItemRepository.findBySubscription_IdAndAddOn_Id(subscription.getId(),
				addonId);
		if (existing != null) {
			throw CustomException.conflict("Add-on already active on this subscription");
		}

		SubscriptionItem item = new SubscriptionItem();
		item.setSubscription(subscription);
		item.setItemType(ItemType.ADDON);
		item.setAddOn(addOn);
		item.setUnitPriceMinor(addOn.getPriceMinor());
		item.setQuantity(1);
		item.setTaxMode(addOn.getTaxMode());
		subscriptionItemRepository.save(item);

		LocalDate today = LocalDate.now();
		boolean isTrialing = subscription.getStatus() == Status.TRIALING;

		if (isTrialing) {
			addAddOnToTrialingSubscription(customer, subscription, addOn);
		} else {
			addAddOnToActiveSubscription(customer, subscription, addOn, today);
		}

		subscriptionItemRepository.flush();
		invoiceRepository.flush();

		auditLoggingService.logAction("ADD_ADDON", ENTITY_SUBSCRIPTION, subscription.getId(), null, addOn);
		return mapToSubscriptionDTO(subscription);
	}

	private void rebuildTrialInvoice(Invoice invoice, Customer customer, Subscription subscription, Plan newPlan,
			long newTrialDays, List<SubscriptionItem> items, Optional<SubscriptionCoupon> scOpt) {
		// Delete old line items
		List<InvoiceLineItem> oldLines = invoiceLineItemRepository.findByInvoice_Id(invoice.getId());
		invoiceLineItemRepository.deleteAll(oldLines);
		invoiceLineItemRepository.flush();

		// Compute fresh pricing
		long priceMinor = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(newPlan.getId(), customer.getCountry(), subscription.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(newPlan.getDefaultPriceMinor() != null ? newPlan.getDefaultPriceMinor() : 0L);
		long discountMinor = resolveDiscountMinor(scOpt, priceMinor, customer.getCurrency());
		Coupon coupon = scOpt.map(SubscriptionCoupon::getCoupon).orElse(null);
		long addonTotal = items.stream().filter(i -> i.getItemType() == ItemType.ADDON)
				.mapToLong(i -> i.getUnitPriceMinor() * i.getQuantity()).sum();
		long subtotal = priceMinor + addonTotal - discountMinor;
		if (subtotal < 0)
			subtotal = 0;
		long taxMinor = calculateTaxMinor(subtotal, customer.getCountry(), newPlan.getTaxMode());
		long totalMinor = (newPlan.getTaxMode() == TaxMode.INCLUSIVE) ? subtotal : subtotal + taxMinor;
		long subtotalMinor = (newPlan.getTaxMode() == TaxMode.INCLUSIVE) ? subtotal - taxMinor : subtotal;

		// Update invoice headers
		invoice.setSubtotalMinor(subtotalMinor);
		invoice.setDiscountMinor(discountMinor);
		invoice.setTaxMinor(taxMinor);
		invoice.setTotalMinor(totalMinor);
		invoice.setBalanceMinor(totalMinor);
		invoice.setDueDate(subscription.getTrialEndDate());
		invoiceRepository.save(invoice);

		// Save fresh Plan Line item
		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setInvoice(invoice);
		planLine.setDescription(newPlan.getName() + " Subscription" + (newTrialDays > 0 ? STARTS_AFTER_TRIAL : ""));
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);
		planLine.setQuantity(1);
		planLine.setUnitPriceMinor(priceMinor);
		planLine.setAmountMinor(priceMinor);
		invoiceLineItemRepository.save(planLine);

		// Save Addon Lines
		for (SubscriptionItem item : items) {
			if (item.getItemType() == ItemType.ADDON) {
				InvoiceLineItem addonLine = new InvoiceLineItem();
				addonLine.setInvoice(invoice);
				addonLine.setDescription(ADDON_PREFIX + item.getAddOn().getName() + STARTS_AFTER_TRIAL);
				addonLine.setLineType(InvoiceLineItem.LineType.ADDON);
				addonLine.setQuantity(item.getQuantity());
				addonLine.setUnitPriceMinor(item.getUnitPriceMinor());
				addonLine.setAmountMinor(item.getUnitPriceMinor() * item.getQuantity());
				invoiceLineItemRepository.save(addonLine);
			}
		}

		// Save Discount Line
		if (discountMinor > 0 && coupon != null) {
			InvoiceLineItem discountLine = new InvoiceLineItem();
			discountLine.setInvoice(invoice);
			discountLine.setDescription("Coupon: " + coupon.getCode() + " (" + coupon.getName() + ")");
			discountLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);
			discountLine.setQuantity(1);
			discountLine.setUnitPriceMinor(-discountMinor);
			discountLine.setAmountMinor(-discountMinor);
			invoiceLineItemRepository.save(discountLine);
			consumeOneTimeSubscriptionCoupon(scOpt.orElse(null));
		}

		// Save Tax Line
		if (taxMinor > 0) {
			InvoiceLineItem taxLine = new InvoiceLineItem();
			taxLine.setInvoice(invoice);
			taxLine.setDescription(getTaxDescription(customer.getCountry()));
			taxLine.setLineType(InvoiceLineItem.LineType.TAX);
			taxLine.setQuantity(1);
			taxLine.setUnitPriceMinor(taxMinor);
			taxLine.setAmountMinor(taxMinor);
			invoiceLineItemRepository.save(taxLine);
		}
	}

	private long calculateOldTotalPaid(Customer customer, Plan oldPlan, List<SubscriptionItem> items,
			Optional<SubscriptionCoupon> scOpt) {
		long oldPriceMinor = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(oldPlan.getId(), customer.getCountry(), customer.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(oldPlan.getDefaultPriceMinor() != null ? oldPlan.getDefaultPriceMinor() : 0L);
		long oldDiscountMinor = resolveDiscountMinor(scOpt, oldPriceMinor, customer.getCurrency());
		long oldAddonTotal = items.stream().filter(i -> i.getItemType() == ItemType.ADDON)
				.mapToLong(i -> i.getUnitPriceMinor() * i.getQuantity()).sum();

		long oldSubtotal = oldPriceMinor + oldAddonTotal - oldDiscountMinor;
		if (oldSubtotal < 0)
			oldSubtotal = 0;
		long oldTaxMinor = calculateTaxMinor(oldSubtotal, customer.getCountry(), oldPlan.getTaxMode());
		return (oldPlan.getTaxMode() == TaxMode.INCLUSIVE) ? oldSubtotal : oldSubtotal + oldTaxMinor;
	}

	private Invoice createProrationInvoice(Customer customer, Subscription subscription, LocalDate today,
			long headerSubtotal, long newTaxMinor) {
		return Invoice.builder().customer(customer).subscription(subscription)
				.invoiceNumber("INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)))
				.billingReason(BillingReason.SUBSCRIPTION_UPDATE).issueDate(today).subtotalMinor(headerSubtotal)
				.taxMinor(newTaxMinor).currency(customer.getCurrency()).idempotencyKey(UUID.randomUUID().toString())
				.build();
	}

	private void handleDowngradeCredit(String email, Plan oldPlan, Plan newPlan, Invoice invoice,
			long prorationAmount) {
		long creditAmount = Math.abs(prorationAmount);
		Customer customer = invoice.getCustomer();
		customer.setCreditBalanceMinor(customer.getCreditBalanceMinor() + creditAmount);
		customerRepository.save(customer);

		invoice.setStatus(Status.PAID);
		invoice.setDiscountMinor(invoice.getSubtotalMinor() + invoice.getTaxMinor());
		invoice.setTotalMinor(0L);
		invoice.setBalanceMinor(0L);

		invoiceRepository.save(invoice);

		User currentUser = userRepository.findByEmail(email)
				.orElseThrow(() -> CustomException.notFound("User not found."));

		CreditNote creditNote = new CreditNote();
		creditNote.setCreditNoteNumber(
				"CN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)));
		creditNote.setInvoice(invoice);
		creditNote.setReason("Proration credit from plan downgrade: " + oldPlan.getName() + " → " + newPlan.getName());
		creditNote.setAmountMinor(creditAmount);
		creditNote.setStatus(Status.ISSUED);
		creditNote.setCreatedBy(currentUser);
		creditNoteRepository.save(creditNote);
	}

	private void chargeUpgradeAmount(Customer customer, Subscription subscription, Invoice invoice,
			long prorationAmount) {
		PaymentMethod pm = paymentMethodRepository.findById(subscription.getPaymentMethodId())
				.orElseThrow(() -> CustomException.notFound("Payment method not found"));
		if (pm.getGatewayToken() == null || pm.getGatewayToken().isBlank()) {
			throw CustomException.badRequest(NO_GATEWAY_TOKEN_MSG);
		}
		String gatewayRef = mockPaymentGateway.charge(pm.getGatewayToken(), prorationAmount, customer.getCurrency());

		Payment payment = new Payment();
		payment.setInvoice(invoice);
		payment.setPaymentMethod(pm);
		payment.setIdempotencyKey(UUID.randomUUID().toString());
		payment.setGatewayRef(gatewayRef);
		payment.setAmountMinor(prorationAmount);
		payment.setCurrency(customer.getCurrency());
		payment.setStatus(Status.SUCCESS);
		payment.setAttemptNo(1);
		paymentRepository.save(payment);
	}

	private void saveProratedLineItems(ProrationLineItemsParam param) {
		InvoiceLineItem planLine = new InvoiceLineItem();
		planLine.setInvoice(param.invoice);
		planLine.setDescription("Plan change to " + param.newPlan.getName() + " (Cycle starting today)");
		planLine.setLineType(InvoiceLineItem.LineType.PLAN);
		planLine.setQuantity(1);
		planLine.setUnitPriceMinor(param.newPriceMinor);
		planLine.setAmountMinor(param.newPriceMinor);
		planLine.setPeriodStart(LocalDate.now());
		planLine.setPeriodEnd(param.newPlan.getBillingPeriod() == BillingPeriod.MONTHLY ? LocalDate.now().plusMonths(1)
				: LocalDate.now().plusYears(1));
		invoiceLineItemRepository.save(planLine);

		InvoiceLineItem creditLine = new InvoiceLineItem();
		creditLine.setInvoice(param.invoice);
		creditLine.setDescription(
				"Unused credit from " + param.oldPlanName + " (" + param.remainingDays + " days remaining)");
		creditLine.setLineType(InvoiceLineItem.LineType.PRORATION);
		creditLine.setQuantity(1);
		creditLine.setUnitPriceMinor(-param.unusedCredit);
		creditLine.setAmountMinor(-param.unusedCredit);
		creditLine.setPeriodStart(LocalDate.now());
		creditLine.setPeriodEnd(param.invoice.getSubscription().getCurrentPeriodEnd());
		invoiceLineItemRepository.save(creditLine);

		if (param.newTaxMinor > 0) {
			InvoiceLineItem taxLine = new InvoiceLineItem();
			taxLine.setInvoice(param.invoice);
			taxLine.setDescription(getTaxDescription(param.invoice.getSubscription().getCustomer().getCountry()));
			taxLine.setLineType(InvoiceLineItem.LineType.TAX);
			taxLine.setQuantity(1);
			taxLine.setUnitPriceMinor(param.newTaxMinor);
			taxLine.setAmountMinor(param.newTaxMinor);
			invoiceLineItemRepository.save(taxLine);
		}

		List<SubscriptionItem> items = subscriptionItemRepository
				.findBySubscription_Id(param.invoice.getSubscription().getId());
		for (SubscriptionItem item : items) {
			if (item.getItemType() == ItemType.ADDON) {
				InvoiceLineItem addonLine = new InvoiceLineItem();
				addonLine.setInvoice(param.invoice);
				addonLine.setDescription(ADDON_PREFIX + item.getAddOn().getName());
				addonLine.setLineType(InvoiceLineItem.LineType.ADDON);
				addonLine.setQuantity(item.getQuantity());
				addonLine.setUnitPriceMinor(item.getUnitPriceMinor());
				addonLine.setAmountMinor(item.getUnitPriceMinor() * item.getQuantity());
				invoiceLineItemRepository.save(addonLine);
			}
		}

		if (param.newDiscountMinor > 0 && param.activeCoupon != null) {
			InvoiceLineItem discountLine = new InvoiceLineItem();
			discountLine.setInvoice(param.invoice);
			discountLine.setDescription("Coupon: " + param.activeCoupon.getCode());
			discountLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);
			discountLine.setQuantity(1);
			discountLine.setUnitPriceMinor(-param.newDiscountMinor);
			discountLine.setAmountMinor(-param.newDiscountMinor);
			invoiceLineItemRepository.save(discountLine);
			consumeOneTimeSubscriptionCoupon(param.activeSubscriptionCoupon);
		}
	}

	private void addAddOnToTrialingSubscription(Customer customer, Subscription subscription, AddOn addOn) {
		Optional<Invoice> openInvoiceOpt = invoiceRepository.findBySubscription_IdAndStatus(subscription.getId(),
				Status.OPEN);
		if (openInvoiceOpt.isPresent()) {
			Invoice invoice = openInvoiceOpt.get();

			InvoiceLineItem addonLine = new InvoiceLineItem();
			addonLine.setInvoice(invoice);
			addonLine.setDescription(ADDON_PREFIX + addOn.getName() + STARTS_AFTER_TRIAL);
			addonLine.setLineType(InvoiceLineItem.LineType.ADDON);
			addonLine.setQuantity(1);
			addonLine.setUnitPriceMinor(addOn.getPriceMinor());
			addonLine.setAmountMinor(addOn.getPriceMinor());
			invoiceLineItemRepository.save(addonLine);

			Long addonTax = calculateTaxMinor(addOn.getPriceMinor(), customer.getCountry(), addOn.getTaxMode());
			Long addonTotal = (addOn.getTaxMode() == TaxMode.INCLUSIVE) ? addOn.getPriceMinor()
					: addOn.getPriceMinor() + addonTax;
			Long addonSubtotal = (addOn.getTaxMode() == TaxMode.INCLUSIVE) ? addOn.getPriceMinor() - addonTax
					: addOn.getPriceMinor();

			InvoiceLineItem taxLine = new InvoiceLineItem();
			taxLine.setInvoice(invoice);
			taxLine.setDescription(getTaxDescription(customer.getCountry()) + " - " + addOn.getName());
			taxLine.setLineType(InvoiceLineItem.LineType.TAX);
			taxLine.setQuantity(1);
			taxLine.setUnitPriceMinor(addonTax);
			taxLine.setAmountMinor(addonTax);
			invoiceLineItemRepository.save(taxLine);

			invoice.setSubtotalMinor(invoice.getSubtotalMinor() + addonSubtotal);
			invoice.setTaxMinor(invoice.getTaxMinor() + addonTax);
			invoice.setTotalMinor(invoice.getTotalMinor() + addonTotal);
			invoice.setBalanceMinor(invoice.getTotalMinor());
			invoiceRepository.save(invoice);
		}
	}

	private void addAddOnToActiveSubscription(Customer customer, Subscription subscription, AddOn addOn,
			LocalDate today) {
		LocalDate periodEnd = subscription.getCurrentPeriodEnd();
		long totalDays = ChronoUnit.DAYS.between(subscription.getCurrentPeriodStart(), periodEnd);
		long remainingDays = ChronoUnit.DAYS.between(today, periodEnd);
		if (totalDays <= 0)
			totalDays = 1;
		if (remainingDays <= 0)
			remainingDays = 1;
		long proratedAmount = (addOn.getPriceMinor() * remainingDays) / totalDays;
		Long taxMinor = calculateTaxMinor(proratedAmount, customer.getCountry(), addOn.getTaxMode());
		Long totalMinor = (addOn.getTaxMode() == TaxMode.INCLUSIVE) ? proratedAmount : proratedAmount + taxMinor;
		Long subtotalMinor = (addOn.getTaxMode() == TaxMode.INCLUSIVE) ? proratedAmount - taxMinor : proratedAmount;

		Invoice invoice = Invoice.builder().customer(customer).subscription(subscription)
				.invoiceNumber(
						"INV-ADDON-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)))
				.status(Status.PAID).billingReason(BillingReason.SUBSCRIPTION_UPDATE).issueDate(today).dueDate(today)
				.subtotalMinor(subtotalMinor).taxMinor(taxMinor).discountMinor(0L).totalMinor(totalMinor)
				.balanceMinor(0L).currency(subscription.getCurrency()).idempotencyKey(UUID.randomUUID().toString())
				.build();
		invoiceRepository.save(invoice);

		InvoiceLineItem addonLine = new InvoiceLineItem();
		addonLine.setInvoice(invoice);
		addonLine.setDescription(ADDON_PREFIX + addOn.getName() + " (prorated)");
		addonLine.setLineType(InvoiceLineItem.LineType.ADDON);
		addonLine.setQuantity(1);
		addonLine.setUnitPriceMinor(proratedAmount);
		addonLine.setAmountMinor(proratedAmount);
		addonLine.setPeriodStart(today);
		addonLine.setPeriodEnd(periodEnd);
		invoiceLineItemRepository.save(addonLine);

		InvoiceLineItem taxLine = new InvoiceLineItem();
		taxLine.setInvoice(invoice);
		taxLine.setDescription(getTaxDescription(customer.getCountry()));
		taxLine.setLineType(InvoiceLineItem.LineType.TAX);
		taxLine.setQuantity(1);
		taxLine.setUnitPriceMinor(taxMinor);
		taxLine.setAmountMinor(taxMinor);
		invoiceLineItemRepository.save(taxLine);

		long amountToCharge = totalMinor;
		if (customer.getCreditBalanceMinor() > 0 && totalMinor > 0) {
			long creditApplied = Math.min(customer.getCreditBalanceMinor(), totalMinor);
			amountToCharge = totalMinor - creditApplied;

			InvoiceLineItem creditLine = new InvoiceLineItem();
			creditLine.setInvoice(invoice);
			creditLine.setDescription("Account Credit Applied");
			creditLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);
			creditLine.setQuantity(1);
			creditLine.setUnitPriceMinor(-creditApplied);
			creditLine.setAmountMinor(-creditApplied);
			invoiceLineItemRepository.save(creditLine);

			invoice.setTotalMinor(amountToCharge);
			invoice.setBalanceMinor(amountToCharge);
			invoiceRepository.save(invoice);

			customer.setCreditBalanceMinor(customer.getCreditBalanceMinor() - creditApplied);
			customerRepository.save(customer);
		}

		if (amountToCharge > 0) {
			PaymentMethod pm = paymentMethodRepository.findById(subscription.getPaymentMethodId())
					.orElseThrow(() -> CustomException.notFound("Payment method not found"));
			if (pm.getGatewayToken() == null || pm.getGatewayToken().isBlank()) {
				throw CustomException.badRequest(NO_GATEWAY_TOKEN_MSG);
			}
			String gatewayRef = mockPaymentGateway.charge(pm.getGatewayToken(), amountToCharge, invoice.getCurrency());

			Payment payment = new Payment();
			payment.setInvoice(invoice);
			payment.setPaymentMethod(pm);
			payment.setAmountMinor(amountToCharge);
			payment.setCurrency(invoice.getCurrency());
			payment.setStatus(Status.SUCCESS);
			payment.setIdempotencyKey(UUID.randomUUID().toString());
			payment.setGatewayRef(gatewayRef);
			payment.setAttemptNo(1);
			paymentRepository.save(payment);
		}
		paymentRepository.flush();
	}

	@Transactional
	public SubscriptionDTO removeAddOn(String email, Long addonId) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());

		SubscriptionItem item = subscriptionItemRepository.findBySubscription_IdAndAddOn_Id(subscription.getId(),
				addonId);
		if (item != null) {
			subscriptionItemRepository.delete(item);
		}

		auditLoggingService.logAction("REMOVE_ADDON", ENTITY_SUBSCRIPTION, subscription.getId(), item, null);
		return mapToSubscriptionDTO(subscription);
	}

	public List<UsageRecordDTO> getMeteredUsage(String email, String startDate, String endDate) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());

		LocalDate start = startDate != null ? LocalDate.parse(startDate) : subscription.getCurrentPeriodStart();
		LocalDate end = endDate != null ? LocalDate.parse(endDate) : subscription.getCurrentPeriodEnd();

		List<UsageRecord> usage = usageRecordRepository
				.findBySubscription_IdAndBillingPeriodStartGreaterThanEqualAndBillingPeriodEndLessThanEqual(
						subscription.getId(), start, end);

		return usage.stream().map(this::mapToUsageRecordDTO).toList();
	}

	// ==================== HELPER METHODS ====================

	private Customer getCustomerByEmail(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> CustomException.notFound("User not found"));
		return customerRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> CustomException.notFound("Customer not found"));
	}

	private Subscription getActiveSubscription(Long customerId) {
		return subscriptionRepository
				.findByCustomer_IdAndStatusIn(customerId,
						List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED, Status.ON_HOLD))
				.stream().findFirst().orElseThrow(() -> CustomException.notFound("No active subscription found"));
	}

	private SubscriptionDTO mapToSubscriptionDTO(Subscription subscription) {
		SubscriptionDTO dto = new SubscriptionDTO();
		dto.setSubscriptionId(subscription.getId());
		dto.setCustomerId(subscription.getCustomer().getId());
		dto.setPlanId(subscription.getPlan().getId());

		Plan plan = subscription.getPlan();
		dto.setPlanName(plan.getName());

		// Fetch region and currency specific plan price
		Long basePrice = priceBookEntryRepository
				.findByPlan_IdAndRegionAndCurrency(plan.getId(), subscription.getCustomer().getCountry(),
						subscription.getCurrency())
				.map(PriceBookEntry::getPriceMinor)
				.orElse(plan.getDefaultPriceMinor() != null ? plan.getDefaultPriceMinor() : 0L);

		dto.setPlanPriceMinor(basePrice);
		dto.setBillingPeriod(plan.getBillingPeriod() != null ? plan.getBillingPeriod().name() : "MONTHLY");

		dto.setStatus(subscription.getStatus());
		dto.setStartDate(subscription.getStartDate().toString());
		dto.setTrialEndDate(subscription.getTrialEndDate() != null ? subscription.getTrialEndDate().toString() : null);
		dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart().toString());
		dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().toString());
		dto.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
		dto.setCanceledAt(subscription.getCanceledAt() != null ? subscription.getCanceledAt().toString() : null);
		dto.setPausedFrom(subscription.getPausedFrom() != null ? subscription.getPausedFrom().toString() : null);
		dto.setPausedTo(subscription.getPausedTo() != null ? subscription.getPausedTo().toString() : null);
		dto.setCurrency(subscription.getCurrency());

		List<SubscriptionItem> items = subscriptionItemRepository.findBySubscription_Id(subscription.getId());
		dto.setAddOns(items.stream().filter(i -> i.getItemType() == ItemType.ADDON).map(this::mapToSubscriptionAddOnDTO)
				.toList());

		dto.setMeteredUsage(items.stream().filter(i -> i.getItemType() == ItemType.METERED)
				.map(this::mapToMeteredUsageDTO).toList());

		// Calculate discount if any
		Optional<SubscriptionCoupon> scOpt = findValidActiveSubscriptionCoupon(subscription.getId());
		long discountMinor = resolveDiscountMinor(scOpt, basePrice, subscription.getCurrency());
		dto.setDiscountMinor(discountMinor);

		// Calculate total due (Plan + Add-ons - Discount + dynamic tax)
		Long addOnTotal = items.stream().filter(i -> i.getItemType() == ItemType.ADDON)
				.mapToLong(i -> i.getUnitPriceMinor() != null ? i.getUnitPriceMinor() * i.getQuantity() : 0L).sum();

		long subtotal = basePrice + addOnTotal - discountMinor;
		if (subtotal < 0)
			subtotal = 0L;

		String country = subscription.getCustomer().getCountry();
		Long taxMinor = calculateTaxMinor(subtotal, country, subscription.getPlan().getTaxMode());
		dto.setTotalDueMinor(
				(subscription.getPlan().getTaxMode() == TaxMode.INCLUSIVE) ? subtotal : subtotal + taxMinor);
		dto.setCreditBalanceMinor(subscription.getCustomer().getCreditBalanceMinor());

		return dto;
	}

	private SubscriptionItemDTO mapToSubscriptionAddOnDTO(SubscriptionItem item) {
		SubscriptionItemDTO dto = new SubscriptionItemDTO();
		dto.setItemId(item.getId());
		dto.setAddonId(item.getAddOn().getId());
		AddOn addOn = addOnRepository.findById(item.getAddOn().getId()).orElse(null);
		dto.setAddonName(addOn != null ? addOn.getName() : UNKNOWN);
		dto.setUnitPriceMinor(item.getUnitPriceMinor());
		dto.setQuantity(item.getQuantity());
		return dto;
	}

	private MeteredUsageDTO mapToMeteredUsageDTO(SubscriptionItem item) {
		MeteredUsageDTO dto = new MeteredUsageDTO();
		dto.setComponentId(item.getComponent().getId());
		MeteredComponent comp = meteredComponentRepository.findById(item.getComponent().getId()).orElse(null);
		dto.setComponentName(comp != null ? comp.getName() : UNKNOWN);
		dto.setUnitName(comp != null ? comp.getUnitName() : "unit");
		dto.setPricePerUnitMinor(comp != null ? comp.getPricePerUnitMinor() : 0);
		dto.setFreeTierQuantity(comp != null ? comp.getFreeTierQuantity() : 0);
		Long usage = usageRecordRepository.sumQuantityBySubscription_IdAndComponent_Id(item.getSubscription().getId(),
				item.getComponent().getId());
		dto.setQuantityUsed(usage != null ? usage : 0);
		long billable = Math.max(0, dto.getQuantityUsed() - dto.getFreeTierQuantity());
		dto.setCostMinor(billable * dto.getPricePerUnitMinor());
		return dto;
	}

	private UsageRecordDTO mapToUsageRecordDTO(UsageRecord usage) {
		UsageRecordDTO dto = new UsageRecordDTO();
		dto.setUsageId(usage.getId());
		dto.setComponentId(usage.getComponent().getId());
		MeteredComponent comp = meteredComponentRepository.findById(usage.getComponent().getId()).orElse(null);
		dto.setComponentName(comp != null ? comp.getName() : UNKNOWN);
		dto.setQuantity(usage.getQuantity());
		dto.setUnitName(comp != null ? comp.getUnitName() : "unit");
		dto.setRecordedAt(usage.getRecordedAt().toString());
		dto.setBillingPeriodStart(usage.getBillingPeriodStart().toString());
		dto.setBillingPeriodEnd(usage.getBillingPeriodEnd().toString());
		return dto;
	}

	private long calculateTaxMinor(long amount, String country, TaxMode taxMode) {
		Optional<TaxRate> taxRateOpt = taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(country,
				LocalDate.now());
		if (taxRateOpt.isEmpty()) {
			return 0L;
		}
		BigDecimal rate = taxRateOpt.get().getRatePercent();

		if (taxMode == TaxMode.INCLUSIVE) {
			BigDecimal rateFactor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100)));
			long baseMinor = BigDecimal.valueOf(amount).divide(rateFactor, 0, java.math.RoundingMode.HALF_UP)
					.longValue();
			return amount - baseMinor;
		}

		return BigDecimal.valueOf(amount).multiply(rate)
				.divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).longValue();
	}

	private String getTaxDescription(String country) {
		Optional<TaxRate> taxRateOpt = taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(country,
				LocalDate.now());
		if (taxRateOpt.isEmpty()) {
			return "Tax";
		}
		BigDecimal rate = taxRateOpt.get().getRatePercent();
		return "Tax (" + rate.stripTrailingZeros().toPlainString() + "%)";
	}

	@Override
	public CancellationRequestDTO createCancellationRequest(String email, CancellationRequestInput input) {
		Customer customer = getCustomerByEmail(email);
		Subscription subscription = getActiveSubscription(customer.getId());

		Optional<CancellationRequest> existing = cancellationRequestRepository
				.findBySubscription_Customer_IdAndStatus(customer.getId(), CancellationRequestStatus.PENDING);
		if (existing.isPresent()) {
			throw CustomException.conflict("There is already a pending cancellation request for this customer");
		}

		CancellationRequest request = CancellationRequest.builder().subscription(subscription).reason(input.getReason())
				.status(CancellationRequestStatus.PENDING).atPeriodEnd(input.isAtPeriodEnd()).build();

		CancellationRequest saved = cancellationRequestRepository.save(request);
		return mapToCancellationRequestDTO(saved);
	}

	@Override
	public CancellationRequestDTO withdrawCancellationRequest(String email) {
		Customer customer = getCustomerByEmail(email);
		CancellationRequest request = cancellationRequestRepository
				.findBySubscription_Customer_IdAndStatus(customer.getId(), CancellationRequestStatus.PENDING)
				.orElseThrow(() -> CustomException.notFound("No pending cancellation request found"));

		request.setStatus(CancellationRequestStatus.WITHDRAWN);
		CancellationRequest saved = cancellationRequestRepository.save(request);
		return mapToCancellationRequestDTO(saved);
	}

	@Override
	public CancellationRequestDTO getPendingCancellationRequest(String email) {
		Customer customer = getCustomerByEmail(email);
		return cancellationRequestRepository
				.findBySubscription_Customer_IdAndStatus(customer.getId(), CancellationRequestStatus.PENDING)
				.map(this::mapToCancellationRequestDTO).orElse(null);
	}

	private CancellationRequestDTO mapToCancellationRequestDTO(CancellationRequest request) {
		CancellationRequestDTO dto = new CancellationRequestDTO();
		dto.setRequestId(request.getId());
		dto.setSubscriptionId(request.getSubscription().getId());
		dto.setPlanName(request.getSubscription().getPlan().getName());
		dto.setCustomerEmail(request.getSubscription().getCustomer().getUser().getEmail());
		dto.setCustomerName(request.getSubscription().getCustomer().getUser().getFullName());
		dto.setReason(request.getReason());
		dto.setStatus(request.getStatus());
		dto.setAtPeriodEnd(request.getAtPeriodEnd());
		dto.setCreatedAt(request.getCreatedAt() != null ? request.getCreatedAt().toString() : null);
		dto.setProcessedByEmail(request.getProcessedBy() != null ? request.getProcessedBy().getEmail() : null);
		dto.setProcessedAt(request.getProcessedAt() != null ? request.getProcessedAt().toString() : null);
		dto.setAgentNotes(request.getAgentNotes());
		return dto;
	}
}
