package com.recuriq.billing.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.recuriq.billing.dto.customer.NotificationResponse;
import com.recuriq.billing.entity.Notification;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.Channel;
import com.recuriq.billing.enums.NotificationStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.NotificationRepository;
import com.recuriq.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

@Transactional

public class NotificationServiceImpl implements NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
	private static final String RENEWAL_REMINDER_SUBJECT = "Upcoming Subscription Renewal & Payment Reminder";

	private final NotificationRepository notificationRepository;

	private final SubscriptionRepository subscriptionRepository;

	@Override

	@Transactional(readOnly = true)

	public List<NotificationResponse> getCustomerNotifications(Long customerId) {
		List<Notification> notifications = notificationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
				customerId, com.recuriq.billing.enums.NotificationStatus.SENT);
		return notifications.stream().map(this::mapToResponse).toList();
	}

	@Override

	public void generateRenewalReminders() {
		LocalDate today = LocalDate.now();
		logger.debug(">>> TODAY={}", today);
		LocalDate sevenDaysLater = today.plusDays(7);
		LocalDate threeDaysLater = today.plusDays(3);
		LocalDate oneDayLater = today.plusDays(1);
		List<Subscription> subscriptions = subscriptionRepository.findByStatus(Status.ACTIVE);
		logger.debug(">>> ACTIVE SUBS={}", subscriptions.size());
		for (Subscription subscription : subscriptions) {
			if (subscription.getCurrentPeriodEnd() != null && subscription.getCustomer() != null) {
				LocalDate renewalDate = subscription.getCurrentPeriodEnd();
				if (renewalDate.equals(sevenDaysLater)) {
					createNotification(subscription,
							"UPCOMING_RENEWAL_AND_PAYMENT_7_DAYS",
							RENEWAL_REMINDER_SUBJECT,
							"Your subscription will automatically renew and payment will be processed in 7 days.");
				}
				if (renewalDate.equals(threeDaysLater)) {
					createNotification(subscription,
							"UPCOMING_RENEWAL_AND_PAYMENT_3_DAYS",
							RENEWAL_REMINDER_SUBJECT,
							"Your subscription will automatically renew and payment will be processed in 3 days.");
				}
				if (renewalDate.equals(oneDayLater)) {
					createNotification(subscription,
							"UPCOMING_RENEWAL_AND_PAYMENT_1_DAY",
							RENEWAL_REMINDER_SUBJECT,
							"Your subscription will automatically renew and payment will be processed in 1 day.");
				}
			}
		}
	}

	@Override
	public void generatePendingPaymentReminders() {
		List<Subscription> pastDueSubscriptions = subscriptionRepository
				.findByStatus(com.recuriq.billing.enums.Status.PAST_DUE);
		for (Subscription subscription : pastDueSubscriptions) {
			createNotification(subscription,
					"PAYMENT_FAILED_RETRY",
					"Payment Failed - Action Required",
					"Your automatic subscription payment failed. We will automatically retry the payment according to our dunning schedule.");
		}
	}

	@Override
	public void processPendingNotifications() {
		LocalDateTime now = LocalDateTime.now();
		List<Notification> notifications = notificationRepository.findByStatus(NotificationStatus.PENDING);
		for (Notification notification : notifications) {
			try {
				notification.setStatus(NotificationStatus.SENT);
				notification.setSentAt(now);
				notificationRepository.save(notification);
			} catch (Exception ex) {
				notification.setStatus(NotificationStatus.FAILED);
				notificationRepository.save(notification);
			}
		}
	}

	private void createNotification(
			Subscription subscription,
			String type,
			String subject,
			String body) {
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
		boolean alreadyExists = notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(
				subscription.getCustomer().getId(),
				type,
				startOfDay,
				endOfDay);
		if (alreadyExists) {
			return;
		}
		Notification notification = Notification.builder()
				.customer(subscription.getCustomer())
				.type(type)
				.subject(subject)
				.body(body)
				.channel(Channel.EMAIL)
				.status(com.recuriq.billing.enums.NotificationStatus.SENT)
				.scheduledAt(LocalDateTime.now())
				.sentAt(LocalDateTime.now())
				.build();
		notificationRepository.save(notification);
	}

	@Override
	public void markAsRead(Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));
		notification.setStatus(com.recuriq.billing.enums.NotificationStatus.READ);
		notificationRepository.save(notification);
	}

	@Override
	public void markAllAsRead(Long customerId) {
		List<Notification> notifications = notificationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
				customerId, com.recuriq.billing.enums.NotificationStatus.SENT);
		for (Notification notification : notifications) {
			notification.setStatus(com.recuriq.billing.enums.NotificationStatus.READ);
		}
		notificationRepository.saveAll(notifications);
	}

	private NotificationResponse mapToResponse(Notification notification) {
		return NotificationResponse.builder()
				.notificationId(notification.getNotificationId())
				.type(notification.getType())
				.subject(notification.getSubject())
				.body(notification.getBody())
				.channel(notification.getChannel())
				.status(notification.getStatus())
				.scheduledAt(notification.getScheduledAt())
				.sentAt(notification.getSentAt())
				.createdAt(notification.getCreatedAt())
				.build();
	}
}
