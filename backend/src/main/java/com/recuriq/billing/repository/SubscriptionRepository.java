package com.recuriq.billing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recuriq.billing.entity.Subscription;
import com.recuriq.billing.enums.Status;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
   long countByStatus(Status status);

   List<Subscription> findByCustomer_Id(Long customerId);
   List<Subscription> findByCustomer_IdAndStatusIn(Long id, List<Status> statuses);
   Optional<Subscription> findByCustomer_IdAndStatus(Long customerId, Status status);

   long countByPlan_IdAndStatusIn(Long planId, List<Status> statuses);

   List<Subscription> findByStatusInAndCurrentPeriodEndLessThan(List<Status> statuses, java.time.LocalDate date);
   List<Subscription> findByStatusIn(List<Status> statuses);

   List<Subscription> findByCurrentPeriodEndAndStatusAndCancelAtPeriodEndFalse(java.time.LocalDate date, Status status);
   List<Subscription> findByTrialEndDateAndStatus(java.time.LocalDate date, Status status);
   List<Subscription> findByStatus(Status status);
}