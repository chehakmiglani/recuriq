package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.*;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionFlowServiceImpl implements SubscriptionFlowService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PriceBookEntryRepository priceBookEntryRepository;
    private final TaxRateRepository taxRateRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final CouponRepository couponRepository;
    private final SubscriptionCouponRepository subscriptionCouponRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final MockPaymentGateway mockPaymentGateway;
    private final AuditLoggingService auditLoggingService;

    private SubscriptionFlowService self;

    @Autowired
    public void setSelf(@Lazy SubscriptionFlowService self) {
        this.self = self;
    }

    /**
     * Step 1: Register customer details
     */
    @Transactional
    public Customer registerCustomerDetails(String email, CustomerRegistrationRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> CustomException.notFound("User not found"));

        // Check if customer already exists
        Optional<Customer> existingCustomer = customerRepository.findByUser_Id(user.getId());
        if (existingCustomer.isPresent()) {
            return existingCustomer.get();
        }

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setPhone(request.getPhone());
        customer.setCountry(request.getCountry());
        customer.setState(request.getState());
        customer.setCity(request.getCity());
        customer.setAddressLine1(request.getAddressLine1());
        customer.setPostalCode(request.getPostalCode());
        customer.setCurrency(request.getCurrency());
        customer.setStatus(Status.ACTIVE);

        customerRepository.save(customer);
        log.debug("Customer registered for user: {}, customerId: {}", email, customer.getId());

        return customer;
    }

    /**
     * Step 2: Create payment method
     */
    @Transactional
    public PaymentMethod createPaymentMethod(Long customerId, PaymentMethodRequest request) {
        Long cid = customerId;
        if (cid == null) throw CustomException.badRequest("Customer ID is required");
        Customer customer = customerRepository.findById(cid)
                .orElseThrow(() -> CustomException.notFound("Customer not found"));

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setCustomer(customer);
        paymentMethod.setPaymentType(request.getPaymentType());
        paymentMethod.setIsDefault(true);
        paymentMethod.setStatus(Status.ACTIVE);

        if (request.getPaymentType() == PaymentType.CARD) {
            // Validate and extract card details
            String cardNumber = request.getCardNumber();
            String last4 = cardNumber.substring(cardNumber.length() - 4);
            String brand = detectCardBrand(cardNumber);

            paymentMethod.setCardLast4(last4);
            paymentMethod.setCardBrand(brand);
            paymentMethod.setExpiryMonth(Integer.parseInt(request.getExpiryMonth()));
            paymentMethod.setExpiryYear(Integer.parseInt(request.getExpiryYear()));
            paymentMethod.setGatewayToken("mock_token_card_" + cardNumber);

        } else if (request.getPaymentType() == PaymentType.UPI) {
            paymentMethod.setUpiId(request.getUpiId());
            paymentMethod.setGatewayToken("mock_token_upi_" + request.getUpiId());
        }

        paymentMethodRepository.save(paymentMethod);
        log.debug("Payment method created for customer: {}, methodId: {}", customerId, paymentMethod.getId());

        return paymentMethod;
    }

    /**
     * Step 3: Complete subscription
     */
    @Transactional
    public SubscriptionResponse completeSubscription(Long customerId, SubscriptionCompletionRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> CustomException.notFound("Customer not found"));

        Long planId = request.getPlanId();
        if (planId == null) throw CustomException.badRequest("Plan ID is required");
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> CustomException.notFound("Plan not found"));

        if (plan.getStatus() == Status.INACTIVE) {
            throw CustomException.badRequest("This plan is no longer available for new subscriptions.");
        }

        PaymentMethod paymentMethod;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> CustomException.notFound("Payment method not found"));
        } else if (request.getPaymentMethod() != null) {
            paymentMethod = self.createPaymentMethod(customerId, request.getPaymentMethod());
        } else {
            throw CustomException.badRequest("Payment method ID or payment method details are required");
        }

        // Calculate price based on region
        Long priceMinor = getPriceForRegion(plan, customer.getCountry(), customer.getCurrency());

        // Apply coupon discount if provided
        CouponDiscountResult discountResult = calculateCouponDiscount(request.getCouponCode(), priceMinor);
        Long discountMinor = discountResult.discountMinor;
        Coupon appliedCoupon = discountResult.appliedCoupon;

        Long priceAfterDiscount = priceMinor - discountMinor;

        // Calculate tax on price after discount
        BigDecimal taxRate = getTaxRateForRegion(customer.getCountry());
        Long taxMinor = calculateTax(priceAfterDiscount, taxRate, plan.getTaxMode());
        
        BillingAmounts amounts = calculateBillingAmounts(plan, priceMinor, discountMinor, taxRate, taxMinor);
        Long subtotalMinor = amounts.subtotalMinor;
        Long headerDiscountMinor = amounts.headerDiscountMinor;
        Long totalMinor = amounts.totalMinor;

        // Check if trial is applicable
        boolean isTrial = plan.getTrialDays() > 0;
        LocalDate today = LocalDate.now();
        LocalDate periodEnd = calculatePeriodEnd(today, request.getBillingPeriod());

        // Create subscription
        Subscription subscription = createSubscription(customer, plan, paymentMethod, isTrial, today, periodEnd, request.getBillingPeriod());

        // Create SubscriptionItem for the plan
        createSubscriptionItem(subscription, plan, priceMinor);

        // Persist coupon linkage
        linkSubscriptionCoupon(subscription, appliedCoupon);

        // Create the invoice for the period
        Status invoiceStatus = isTrial ? Status.OPEN : Status.PAID;
        LocalDate dueDate = isTrial ? subscription.getTrialEndDate() : today;

        Invoice invoice = createInvoice(subscription, subtotalMinor, taxMinor, totalMinor, today,
                dueDate, invoiceStatus);
        invoice.setDiscountMinor(headerDiscountMinor);
        invoiceRepository.save(invoice);

        // Add line items to the invoice
        createInvoiceLineItems(invoice, subscription, priceMinor, discountMinor, appliedCoupon, taxMinor);

        if (!isTrial) {
            applyAccountCreditAndCharge(invoice, customer, paymentMethod, totalMinor);
        }

        SubscriptionResponse response = buildSubscriptionResponse(subscription, invoice, totalMinor);

        // Audit Log for Subscription Creation
        auditLoggingService.logAction("CREATE_SUBSCRIPTION", "Subscription", subscription.getId(), null, subscription);

        return response;
    }

    private static class CouponDiscountResult {
        final Long discountMinor;
        final Coupon appliedCoupon;
        CouponDiscountResult(Long discountMinor, Coupon appliedCoupon) {
            this.discountMinor = discountMinor;
            this.appliedCoupon = appliedCoupon;
        }
    }

    private static class BillingAmounts {
        final Long subtotalMinor;
        final Long headerDiscountMinor;
        final Long totalMinor;

        BillingAmounts(Long subtotalMinor, Long headerDiscountMinor, Long totalMinor) {
            this.subtotalMinor = subtotalMinor;
            this.headerDiscountMinor = headerDiscountMinor;
            this.totalMinor = totalMinor;
        }
    }

    private BillingAmounts calculateBillingAmounts(Plan plan, Long priceMinor, Long discountMinor, BigDecimal taxRate, Long taxMinor) {
        Long priceAfterDiscount = priceMinor - discountMinor;
        Long subtotalMinor;
        Long totalMinor;
        Long headerDiscountMinor;
        if (plan.getTaxMode() == TaxMode.INCLUSIVE) {
            Long taxOnPre = calculateTax(priceMinor, taxRate, plan.getTaxMode());
            Long taxOnDiscount = calculateTax(discountMinor, taxRate, plan.getTaxMode());
            subtotalMinor = priceMinor - taxOnPre;
            headerDiscountMinor = discountMinor - taxOnDiscount;
            totalMinor = priceAfterDiscount;
        } else {
            subtotalMinor = priceMinor;
            headerDiscountMinor = discountMinor;
            totalMinor = priceAfterDiscount + taxMinor;
        }
        return new BillingAmounts(subtotalMinor, headerDiscountMinor, totalMinor);
    }

    private CouponDiscountResult calculateCouponDiscount(String couponCode, long priceMinor) {
        if (couponCode == null || couponCode.isBlank()) {
            return new CouponDiscountResult(0L, null);
        }

        Coupon appliedCoupon = couponRepository.findByCodeAndStatus(couponCode, Status.ACTIVE)
                .orElse(null);

        if (appliedCoupon == null || !isValidCoupon(appliedCoupon, LocalDate.now())) {
            return new CouponDiscountResult(0L, null);
        }

        long discountMinor = appliedCoupon.getType() == CouponType.PERCENT
                ? priceMinor * appliedCoupon.getAmount() / 100
                : appliedCoupon.getAmount();

        return new CouponDiscountResult(Math.min(discountMinor, priceMinor), appliedCoupon);
    }

    private boolean isValidCoupon(Coupon coupon, LocalDate today) {
        return (coupon.getValidFrom() == null || !today.isBefore(coupon.getValidFrom()))
                && (coupon.getValidTo() == null || !today.isAfter(coupon.getValidTo()))
                && (coupon.getMaxRedemptions() == null || coupon.getRedeemedCount() < coupon.getMaxRedemptions());
    }

    private Subscription createSubscription(Customer customer, Plan plan, PaymentMethod paymentMethod, boolean isTrial, LocalDate today, LocalDate periodEnd, BillingPeriod billingPeriod) {
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setStatus(Status.ACTIVE);
        subscription.setStartDate(today);

        if (isTrial) {
            subscription.setStatus(Status.TRIALING);
            subscription.setTrialEndDate(today.plusDays(plan.getTrialDays()));
            subscription.setCurrentPeriodStart(subscription.getTrialEndDate());
            subscription.setCurrentPeriodEnd(
                    calculatePeriodEnd(subscription.getTrialEndDate(), billingPeriod));
        } else {
            subscription.setCurrentPeriodStart(today);
            subscription.setCurrentPeriodEnd(periodEnd);
        }

        subscription.setPaymentMethodId(paymentMethod.getId());
        subscription.setCurrency(customer.getCurrency());
        subscription.setCancelAtPeriodEnd(false);

        subscriptionRepository.save(subscription);
        log.debug("Subscription created: {}", subscription.getId());
        return subscription;
    }

    private void createSubscriptionItem(Subscription subscription, Plan plan, Long priceMinor) {
        SubscriptionItem planItem = new SubscriptionItem();
        planItem.setSubscription(subscription);
        planItem.setItemType(ItemType.PLAN);
        planItem.setPlan(plan);
        planItem.setUnitPriceMinor(priceMinor);
        planItem.setQuantity(1);
        planItem.setTaxMode(plan.getTaxMode());
        subscriptionItemRepository.save(planItem);
    }

    private void linkSubscriptionCoupon(Subscription subscription, Coupon appliedCoupon) {
        if (appliedCoupon != null) {
            SubscriptionCoupon sc = new SubscriptionCoupon();
            sc.setSubscription(subscription);
            sc.setCoupon(appliedCoupon);
            sc.setAppliedAt(LocalDateTime.now());
            sc.setStatus(Status.ACTIVE);
            sc.setExpiresAt(calculateSubscriptionCouponExpiry(appliedCoupon, subscription));
            subscriptionCouponRepository.save(sc);

            appliedCoupon.setRedeemedCount(appliedCoupon.getRedeemedCount() + 1);
            couponRepository.save(appliedCoupon);
        }
    }

    private LocalDateTime calculateSubscriptionCouponExpiry(Coupon coupon, Subscription subscription) {
        if (coupon.getDuration() == Duration.REPEATING && coupon.getDurationInMonths() != null) {
            return LocalDateTime.now().plusMonths(coupon.getDurationInMonths());
        }
        if (coupon.getDuration() == Duration.ONCE) {
            return subscription.getCurrentPeriodEnd() != null
                    ? subscription.getCurrentPeriodEnd().atStartOfDay()
                    : LocalDateTime.now().plusDays(1);
        }
        return null;
    }

    private SubscriptionResponse buildSubscriptionResponse(Subscription subscription, Invoice invoice, Long totalMinor) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setTotalAmountMinor(totalMinor);
        response.setSubscriptionId(subscription.getId());
        response.setStatus(subscription.getStatus().name());
        response.setMessage("Subscription activated successfully");
        if (subscription.getTrialEndDate() != null) {
            response.setTrialEndDate(subscription.getTrialEndDate().toString());
        }
        return response;
    }

    private void createInvoiceLineItems(Invoice invoice, Subscription subscription, Long priceMinor, Long discountMinor, Coupon appliedCoupon, Long taxMinor) {
        Plan plan = subscription.getPlan();
        Customer customer = subscription.getCustomer();
        LocalDate today = invoice.getIssueDate();
        // Add line items to the invoice
        InvoiceLineItem planLine = new InvoiceLineItem();
        planLine.setInvoice(invoice);
        planLine.setDescription(plan.getName() + " Subscription" + (subscription.getTrialEndDate() != null ? " (Starts after trial)" : ""));
        planLine.setLineType(InvoiceLineItem.LineType.PLAN);
        planLine.setQuantity(1);
        planLine.setUnitPriceMinor(priceMinor);
        planLine.setAmountMinor(priceMinor);
        planLine.setPeriodStart(subscription.getCurrentPeriodStart());
        planLine.setPeriodEnd(subscription.getCurrentPeriodEnd());
        invoiceLineItemRepository.save(planLine);

        // Add discount line item if coupon was applied
        if (discountMinor > 0 && appliedCoupon != null) {
            InvoiceLineItem discountLine = new InvoiceLineItem();
            discountLine.setInvoice(invoice);
            discountLine.setDescription("Coupon: " + appliedCoupon.getCode() + " (" + appliedCoupon.getName() + ")");
            discountLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);
            discountLine.setQuantity(1);
            discountLine.setUnitPriceMinor(-discountMinor);
            discountLine.setAmountMinor(-discountMinor);
            discountLine.setDiscount(appliedCoupon);
            invoiceLineItemRepository.save(discountLine);
        }

        // Add tax line item
        if (taxMinor > 0) {
            InvoiceLineItem taxLine = new InvoiceLineItem();
            taxLine.setInvoice(invoice);
            BigDecimal rate = getTaxRateForRegion(customer.getCountry());
            taxLine.setDescription("Tax (" + rate.stripTrailingZeros().toPlainString() + "%)");
            taxLine.setLineType(InvoiceLineItem.LineType.TAX);
            taxLine.setQuantity(1);
            taxLine.setUnitPriceMinor(taxMinor);
            taxLine.setAmountMinor(taxMinor);
            
            taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(customer.getCountry(), today)
                    .ifPresent(taxLine::setTaxRate);
                    
            invoiceLineItemRepository.save(taxLine);
        }
    }

    private void applyAccountCreditAndCharge(Invoice invoice, Customer customer, PaymentMethod paymentMethod, Long totalMinor) {
        long amountToCharge = totalMinor;
        if (customer.getCreditBalanceMinor() > 0 && totalMinor > 0) {
            long creditApplied = Math.min(customer.getCreditBalanceMinor(), totalMinor);
            amountToCharge = totalMinor - creditApplied;

            // Add credit line item to invoice
            InvoiceLineItem creditLine = new InvoiceLineItem();
            creditLine.setInvoice(invoice);
            creditLine.setDescription("Account Credit Applied");
            creditLine.setLineType(InvoiceLineItem.LineType.DISCOUNT);
            creditLine.setQuantity(1);
            creditLine.setUnitPriceMinor(-creditApplied);
            creditLine.setAmountMinor(-creditApplied);
            invoiceLineItemRepository.save(creditLine);

            // Update invoice totals
            invoice.setTotalMinor(amountToCharge);
            invoice.setBalanceMinor(amountToCharge);
            invoiceRepository.save(invoice);

            // Deduct from customer credit balance
            customer.setCreditBalanceMinor(customer.getCreditBalanceMinor() - creditApplied);
            customerRepository.save(customer);
        }

        // Create payment record for the remaining amount (skip if fully covered by credit)
        if (amountToCharge > 0) {
            createPayment(invoice, paymentMethod, amountToCharge, customer.getCurrency());
            log.debug("Payment created for invoice: {}", invoice.getId());
        }
    }

    /**
     * Check customer status
     */
    public CustomerStatusResponse checkCustomerStatus(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new CustomerStatusResponse(false, false, false, "User not found");
        }

        User user = userOpt.get();
        Optional<Customer> customerOpt = customerRepository.findByUser_Id(user.getId());

        if (customerOpt.isEmpty()) {
            return new CustomerStatusResponse(false, false, false, "Customer record not created");
        }

        Customer customer = customerOpt.get();
        
        Optional<Subscription> dashboardEligibleSub = subscriptionRepository
                .findByCustomer_IdAndStatusIn(customer.getId(),
                        java.util.List.of(Status.ACTIVE, Status.TRIALING, Status.PAST_DUE, Status.PAUSED, Status.ON_HOLD, Status.CANCELED))
                .stream().findAny();

        Optional<Subscription> draftSub = subscriptionRepository
                .findByCustomer_IdAndStatusIn(customer.getId(),
                        java.util.List.of(Status.DRAFT)).stream().findAny();

        boolean isDashboardEligible = dashboardEligibleSub.isPresent();
        boolean hasDraftSubscription = draftSub.isPresent();
        String message;
        if (isDashboardEligible) {
            message = "Active or relevant subscription found";
        } else if (hasDraftSubscription) {
            message = "Draft subscription found";
        } else {
            message = "Customer exists but no subscription";
        }
        return new CustomerStatusResponse(isDashboardEligible, isDashboardEligible, hasDraftSubscription, message);
    }

    // Helper methods

    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4"))
            return "VISA";
        if (cardNumber.startsWith("5"))
            return "MASTERCARD";
        if (cardNumber.startsWith("34") || cardNumber.startsWith("37"))
            return "AMEX";
        return "UNKNOWN";
    }

    private Long getPriceForRegion(Plan plan, String country, String currency) {
        // Try to find region-specific price
        Optional<PriceBookEntry> entry = priceBookEntryRepository
                .findByPlan_IdAndRegionAndCurrency(plan.getId(), country, currency);

        if (entry.isPresent()) {
            return entry.get().getPriceMinor();
        }

        // Fallback to default price
        return plan.getDefaultPriceMinor();
    }

    private BigDecimal getTaxRateForRegion(String country) {
        Optional<TaxRate> taxRate = taxRateRepository.findByRegionAndEffectiveToIsNullOrFuture(country,
                LocalDate.now());
        return taxRate.map(TaxRate::getRatePercent).orElse(BigDecimal.ZERO);
    }

    private Long calculateTax(Long amount, BigDecimal taxRate, TaxMode taxMode) {
        if (taxMode == TaxMode.INCLUSIVE) {
            // Price already includes tax — extract the tax portion
            // e.g., ₹499 incl 18% GST → base = 499 / 1.18 = 423.73, tax = 75.27
            if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
                return 0L;
            }
            BigDecimal rateFactor = BigDecimal.ONE.add(taxRate.divide(BigDecimal.valueOf(100)));
            long baseMinor = BigDecimal.valueOf(amount).divide(rateFactor, 0, java.math.RoundingMode.HALF_UP).longValue();
            return amount - baseMinor;
        }
        // Exclusive: add tax on top
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }
        return BigDecimal.valueOf(amount)
                .multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                .longValue();
    }

    private LocalDate calculatePeriodEnd(LocalDate start, BillingPeriod period) {
        return period == BillingPeriod.MONTHLY ? start.plusMonths(1) : start.plusYears(1);
    }

    private Invoice createInvoice(Subscription subscription,
            Long subtotal, Long tax, Long total, LocalDate today, LocalDate dueDate, Status status) {
        Customer customer = subscription.getCustomer();
        Invoice invoice = Invoice.builder()
                .customer(customer)
                .subscription(subscription)
                .invoiceNumber("INV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
                .status(status)
                .billingReason(BillingReason.SUBSCRIPTION_CREATE)
                .issueDate(today)
                .dueDate(dueDate)
                .subtotalMinor(subtotal)
                .taxMinor(tax)
                .discountMinor(0L)
                .totalMinor(total)
                .balanceMinor(status == Status.OPEN ? total : 0L)
                .currency(customer.getCurrency())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        invoiceRepository.save(invoice);
        return invoice;
    }

    private void createPayment(Invoice invoice, PaymentMethod paymentMethod, Long amount, String currency) {
        // Attempt charge via mock gateway — will throw CustomException on decline
        String gatewayRef = mockPaymentGateway.charge(paymentMethod.getGatewayToken(), amount, currency);

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setPaymentMethod(paymentMethod);
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setGatewayRef(gatewayRef);
        payment.setAmountMinor(amount);
        payment.setCurrency(currency);
        payment.setStatus(Status.SUCCESS);
        payment.setAttemptNo(1);

        paymentRepository.save(payment);
    }
}
