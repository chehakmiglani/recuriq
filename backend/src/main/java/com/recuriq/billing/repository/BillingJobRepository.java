package com.recuriq.billing.repository;

import com.recuriq.billing.entity.BillingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingJobRepository extends JpaRepository<BillingJob, Long> {
}
