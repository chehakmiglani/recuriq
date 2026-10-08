package com.recuriq.billing.repository;

import com.recuriq.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
	List<Invoice> findByCustomer_IdOrderByIssueDateDesc(Long customerId);
	List<Invoice> findByCustomer_IdAndStatus(Long customerId, String status);
	List<Invoice> findByCustomer_Id(Long customerId);
    java.util.Optional<Invoice> findBySubscription_IdAndStatus(Long subscriptionId, com.recuriq.billing.enums.Status status);
    
    boolean existsBySubscriptionAndBillingReasonAndIssueDate(com.recuriq.billing.entity.Subscription subscription, com.recuriq.billing.enums.BillingReason billingReason, java.time.LocalDate issueDate);
    org.springframework.data.domain.Page<Invoice> findAllByOrderByIdDesc(org.springframework.data.domain.Pageable pageable);
}
