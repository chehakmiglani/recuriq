package com.recuriq.billing.repository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.recuriq.billing.entity.Notification;
import com.recuriq.billing.enums.NotificationStatus;
@Repository

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
	List<Notification> findByStatus(NotificationStatus status);
	boolean existsByCustomerIdAndTypeAndScheduledAtBetween(Long customerId,String type,LocalDateTime start,LocalDateTime end);
	long countByCustomerIdAndStatus(Long customerId,NotificationStatus status);
	List<Notification> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId,NotificationStatus status);
	List<Notification> findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(NotificationStatus status,LocalDateTime time);
}
