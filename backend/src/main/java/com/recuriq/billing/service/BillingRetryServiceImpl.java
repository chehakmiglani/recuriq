package com.recuriq.billing.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.entity.Invoice;
import com.recuriq.billing.entity.Payment;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.DunningStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.DunningRetryLogRepository;
import com.recuriq.billing.repository.InvoiceRepository;
import com.recuriq.billing.repository.PaymentMethodRepository;
import com.recuriq.billing.repository.PaymentRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class BillingRetryServiceImpl
		implements BillingRetryService {

	private final DunningRetryLogRepository dunningRetryLogRepository;

	private final PaymentRepository paymentRepository;

	private final PaymentMethodRepository paymentMethodRepository;

	private final InvoiceRepository invoiceRepository;

	private final SubscriptionRepository subscriptionRepository;

	private final MockPaymentGateway mockPaymentGateway;

	@Override

	@Transactional

	public void retryFailedPayments() {
		List<DunningRetryLog> retryLogs = dunningRetryLogRepository
				.findByStatusAndScheduledAtLessThanEqual(DunningStatus.SCHEDULED, LocalDateTime.now());
		for (DunningRetryLog retryLog : retryLogs) {
			try {
				retryFailedPayment(retryLog);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void retryFailedPayment(DunningRetryLog retryLog) {
		Invoice invoice = retryLog.getInvoice();
		if (invoice == null || invoice.getStatus() == Status.PAID) {
			return;
		}
		Subscription subscription = invoice.getSubscription();
		if (subscription == null || subscription.getStatus() == Status.CANCELED) {
			return;
		}

		// FULLY CREDIT-COVERED INVOICE
		if (invoice.getTotalMinor() == 0L) {
			handleCreditCoveredInvoice(retryLog, invoice, subscription);
			return;
		}

		PaymentMethod paymentMethod = paymentMethodRepository.findById(subscription.getPaymentMethodId())
				.orElse(null);
		if (paymentMethod == null || paymentMethod.getGatewayToken() == null
				|| paymentMethod.getGatewayToken().isBlank()) {
			handleInvalidPaymentMethod(retryLog);
			return;
		}

		// MARK RETRY ATTEMPTED
		retryLog.setStatus(DunningStatus.ATTEMPTED);
		retryLog.setAttemptedAt(LocalDateTime.now());
		dunningRetryLogRepository.save(retryLog);

		// CREATE RETRY PAYMENT
		Payment payment = createRetryPaymentEntity(retryLog, invoice, paymentMethod);

		// CHARGE PAYMENT
		executePaymentCharge(retryLog, invoice, subscription, paymentMethod, payment);
	}

	private void handleCreditCoveredInvoice(DunningRetryLog retryLog, Invoice invoice, Subscription subscription) {
		invoice.setStatus(Status.PAID);
		invoice.setBalanceMinor(0L);
		invoiceRepository.save(invoice);
		retryLog.setStatus(DunningStatus.SUCCESS);
		retryLog.setAttemptedAt(LocalDateTime.now());
		dunningRetryLogRepository.save(retryLog);
		advanceSubscription(subscription);
	}

	private void handleInvalidPaymentMethod(DunningRetryLog retryLog) {
		retryLog.setStatus(DunningStatus.FAILED);
		retryLog.setFailureReason("Invalid payment method");
		retryLog.setAttemptedAt(LocalDateTime.now());
		dunningRetryLogRepository.save(retryLog);
	}

	private Payment createRetryPaymentEntity(DunningRetryLog retryLog, Invoice invoice, PaymentMethod paymentMethod) {
		Payment payment = new Payment();
		payment.setInvoice(invoice);
		payment.setPaymentMethod(paymentMethod);
		payment.setAttemptNo(retryLog.getAttemptNo() + 1);
		payment.setAmountMinor(invoice.getTotalMinor());
		payment.setCurrency(invoice.getCurrency());
		payment.setStatus(Status.PENDING);
		payment.setResponseCode("PENDING");
		payment.setIdempotencyKey(
				"retry-" + invoice.getId() + "-" + retryLog.getAttemptNo() + "-" + System.currentTimeMillis());
		paymentRepository.save(payment);
		return payment;
	}

	private void executePaymentCharge(DunningRetryLog retryLog, Invoice invoice, Subscription subscription,
			PaymentMethod paymentMethod, Payment payment) {
		try {
			String gatewayRef = mockPaymentGateway.charge(
					paymentMethod.getGatewayToken(),
					invoice.getTotalMinor(),
					invoice.getCurrency()
			);
			payment.setGatewayRef(gatewayRef);
			payment.setStatus(Status.SUCCESS);
			payment.setResponseCode("SUCCESS");
			paymentRepository.save(payment);

			// MARK RETRY SUCCESS
			retryLog.setStatus(DunningStatus.SUCCESS);
			dunningRetryLogRepository.save(retryLog);

			// MARK INVOICE PAID
			invoice.setStatus(Status.PAID);
			invoice.setBalanceMinor(0L);
			invoiceRepository.save(invoice);

			// ADVANCE SUBSCRIPTION
			advanceSubscription(subscription);
		} catch (CustomException ex) {
			handleFailedCharge(retryLog, invoice, subscription, payment, ex);
		}
	}

	private void handleFailedCharge(DunningRetryLog retryLog, Invoice invoice, Subscription subscription, Payment payment, CustomException ex) {
		// PAYMENT FAILED
		payment.setStatus(Status.FAILED);
		payment.setResponseCode("FAILED");
		payment.setFailureReason(ex.getMessage());
		paymentRepository.save(payment);

		// MARK RETRY FAILED
		retryLog.setStatus(DunningStatus.FAILED);
		retryLog.setFailureReason(ex.getMessage());
		dunningRetryLogRepository.save(retryLog);

		// CANCEL AFTER FINAL ATTEMPT
		if (retryLog.getAttemptNo() >= 3) {
			subscription.setStatus(Status.CANCELED);
			subscriptionRepository.save(subscription);
			return;
		}

		// CREATE NEXT RETRY
		createNextRetryLog(invoice, payment, retryLog.getAttemptNo() + 1);
	}



	private void createNextRetryLog(
			Invoice invoice,
			Payment payment,
			int attemptNo
	) {
		DunningRetryLog log = new DunningRetryLog();
		log.setInvoice(invoice);
		log.setPayment(payment);
		log.setAttemptNo(attemptNo);
		switch (attemptNo) {
		case 2:
			log.setScheduledAt(LocalDateTime.now().plusDays(2));
			break;
		case 3:
			log.setScheduledAt(LocalDateTime.now().plusDays(4));
			break;
		default:
			log.setScheduledAt(LocalDateTime.now().plusDays(4));
		}
		log.setStatus(DunningStatus.SCHEDULED);
		dunningRetryLogRepository.save(log);
	}

	private void advanceSubscription(
			Subscription subscription
	) {
		LocalDate start = subscription.getCurrentPeriodEnd().plusDays(1);
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
			throw CustomException.badRequest(
					"Unsupported billing period");
		}
		subscription.setCurrentPeriodStart(start);
		subscription.setCurrentPeriodEnd(end);
		subscription.setStatus(Status.ACTIVE);
		subscriptionRepository.save(subscription);
	}
}
