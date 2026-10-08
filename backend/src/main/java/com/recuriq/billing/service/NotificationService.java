package com.recuriq.billing.service;
import java.util.List;
import com.recuriq.billing.dto.customer.NotificationResponse;

public interface NotificationService {
	List<NotificationResponse> getCustomerNotifications(Long customerId);
	void generateRenewalReminders();
	void processPendingNotifications();
	void generatePendingPaymentReminders();
	void markAsRead(Long notificationId);
	void markAllAsRead(Long customerId);
}
