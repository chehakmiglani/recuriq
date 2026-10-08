package com.recuriq.billing.repository;

import com.recuriq.billing.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.recuriq.billing.enums.Status;

import java.util.List;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
	List<CreditNote> findByInvoice_Id(Long invoiceId);
	List<CreditNote> findByInvoice_Customer_Id(Long customerId);
	Page<CreditNote> findAllByOrderByIdDesc(Pageable pageable);
	List<CreditNote> findByStatus(Status status);
}