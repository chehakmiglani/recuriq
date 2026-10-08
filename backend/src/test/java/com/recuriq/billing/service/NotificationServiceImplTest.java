package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recuriq.billing.dto.customer.NotificationResponse;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.Notification;
import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.NotificationStatus;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.repository.NotificationRepository;
import com.recuriq.billing.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Customer customer;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).build();
        subscription = Subscription.builder()
                .id(1L)
                .customer(customer)
                .status(Status.ACTIVE)
                .currentPeriodEnd(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    void testGetCustomerNotifications() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .customer(customer)
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.SENT))
                .thenReturn(Arrays.asList(notification));

        List<NotificationResponse> responses = notificationService.getCustomerNotifications(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getNotificationId());
    }

    @Test
    void testGenerateRenewalReminders_7Days() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(anyLong(), anyString(), any(),
                any()))
                .thenReturn(false);

        notificationService.generateRenewalReminders();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGenerateRenewalReminders_3Days() {
        subscription.setCurrentPeriodEnd(LocalDate.now().plusDays(3));
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(anyLong(), anyString(), any(),
                any()))
                .thenReturn(false);

        notificationService.generateRenewalReminders();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGenerateRenewalReminders_1Day() {
        subscription.setCurrentPeriodEnd(LocalDate.now().plusDays(1));
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(anyLong(), anyString(), any(),
                any()))
                .thenReturn(false);

        notificationService.generateRenewalReminders();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGenerateRenewalReminders_AlreadyExists() {
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(anyLong(), anyString(), any(),
                any()))
                .thenReturn(true);

        notificationService.generateRenewalReminders();

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testGeneratePendingPaymentReminders() {
        subscription.setStatus(Status.PAST_DUE);
        when(subscriptionRepository.findByStatus(Status.PAST_DUE)).thenReturn(Arrays.asList(subscription));
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(anyLong(), anyString(), any(),
                any()))
                .thenReturn(false);

        notificationService.generatePendingPaymentReminders();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testProcessPendingNotifications() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.findByStatus(NotificationStatus.PENDING)).thenReturn(Arrays.asList(notification));

        notificationService.processPendingNotifications();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testMarkAsRead() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L);

        assertEquals(NotificationStatus.READ, notification.getStatus());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testGenerateRenewalReminders_NullPeriodEnd() {
        subscription.setCurrentPeriodEnd(null);
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));

        notificationService.generateRenewalReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testGenerateRenewalReminders_NullCustomer() {
        subscription.setCustomer(null);
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));

        notificationService.generateRenewalReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testProcessPendingNotifications_Exception() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.findByStatus(NotificationStatus.PENDING)).thenReturn(Arrays.asList(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(notification);

        notificationService.processPendingNotifications();
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }

    @Test
    void testCreateNotification_AlreadyExists() {
        when(notificationRepository.existsByCustomerIdAndTypeAndScheduledAtBetween(any(), any(), any(), any()))
                .thenReturn(true);
        
        subscription.setCurrentPeriodEnd(LocalDate.now().plusDays(7));
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));

        notificationService.generateRenewalReminders();

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAllAsRead() {
        Notification n1 = Notification.builder().notificationId(1L).status(NotificationStatus.SENT).build();
        Notification n2 = Notification.builder().notificationId(2L).status(NotificationStatus.SENT).build();
        when(notificationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.SENT))
                .thenReturn(Arrays.asList(n1, n2));

        notificationService.markAllAsRead(1L);

        assertEquals(NotificationStatus.READ, n1.getStatus());
        assertEquals(NotificationStatus.READ, n2.getStatus());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testMarkAsRead_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(999L));
    }

    @Test
    void testGenerateRenewalReminders_NoMatchDate() {
        // Subscription with period end that doesn't match 1/3/7 days
        subscription.setCurrentPeriodEnd(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByStatus(Status.ACTIVE)).thenReturn(Arrays.asList(subscription));

        notificationService.generateRenewalReminders();

        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
