package com.recuriq.billing.repository;

import com.recuriq.billing.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
	List<UsageRecord> findBySubscription_Customer_Id(Long customerId);
	List<UsageRecord> findBySubscription_IdAndBillingPeriodStartGreaterThanEqualAndBillingPeriodEndLessThanEqual(
			Long subscriptionId, LocalDate start, LocalDate end);

@Query("SELECT SUM(u.quantity) FROM UsageRecord u WHERE u.subscription.id = :subscriptionId AND u.component.id = :componentId")
	Long sumQuantityBySubscription_IdAndComponent_Id(@Param("subscriptionId") Long subscriptionId, @Param("componentId") Long componentId);

}