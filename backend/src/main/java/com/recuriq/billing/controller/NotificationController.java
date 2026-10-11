package com.recuriq.billing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recuriq.billing.dto.customer.NotificationResponse;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.User;

import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.CustomerRepository;
import com.recuriq.billing.service.NotificationService;
import lombok.RequiredArgsConstructor;

@RestController

@RequestMapping("/api/notifications")
@RequiredArgsConstructor

public class NotificationController {

	private final NotificationService notificationService;
	private final CustomerRepository customerRepository;

	@GetMapping("/me")

	public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
		User user = (User) authentication.getPrincipal();

		Customer customer = customerRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> CustomException.notFound("Customer not found"));

		return ResponseEntity.ok(

				notificationService.getCustomerNotifications(customer.getId())

		);
	}

	@PutMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		Customer customer = customerRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> com.recuriq.billing.exception.CustomException.notFound("Customer not found"));
		notificationService.markAllAsRead(customer.getId());
		return ResponseEntity.ok().build();
	}

	// Staff-only: runs the notification schedulers on demand
	@GetMapping("/seed-test-data")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
	public ResponseEntity<String> seedTestData() {
		// Run the actual scheduler logic instantly
		notificationService.generateRenewalReminders();
		notificationService.generatePendingPaymentReminders();
		notificationService.processPendingNotifications();

		return ResponseEntity.ok("Triggered the real notification generation schedulers. Check your database/UI!");
	}
}