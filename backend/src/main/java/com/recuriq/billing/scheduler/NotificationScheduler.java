package com.recuriq.billing.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.recuriq.billing.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor

public class NotificationScheduler {

	private final NotificationService notificationService;

	// @Scheduled(fixedRate=50000)
	@Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Kolkata")

	public void scheduleRenewalReminderGeneration() {
		notificationService.generateRenewalReminders();
	}

	// @Scheduled(fixedRate=50000)
	@Scheduled(cron = "0 0 * * * ?", zone = "Asia/Kolkata")

	public void schedulePendingNotificationProcessing() {
		notificationService.processPendingNotifications();
	}

	// @Scheduled(fixedRate = 10000)
	@Scheduled(cron = "0 0 * * * ?", zone = "Asia/Kolkata")

	public void schedulePendendingPaymentReminderGeneration() {
		notificationService.generatePendingPaymentReminders();
	}
}
