package com.recuriq.billing.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recuriq.billing.entity.CancellationRequest;
import com.recuriq.billing.enums.CancellationRequestStatus;

public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, Long> {
    Optional<CancellationRequest> findBySubscription_Customer_IdAndStatus(Long customerId, CancellationRequestStatus status);
    List<CancellationRequest> findByStatus(CancellationRequestStatus status);
}
