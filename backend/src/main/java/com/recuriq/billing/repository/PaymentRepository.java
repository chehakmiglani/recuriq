package com.recuriq.billing.repository;

import com.recuriq.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByInvoice_Id(Long invoiceId);

	List<Payment> findByInvoice_Customer_Id(Long customerId);

	List<Payment> findByStatus(com.recuriq.billing.enums.Status status);

	org.springframework.data.domain.Page<Payment> findAllByOrderByIdDesc(org.springframework.data.domain.Pageable pageable);

	Optional<Payment> findFirstByInvoiceIdOrderByCreatedAtDesc(Long id);
}