package com.recuriq.billing.repository;

import com.recuriq.billing.entity.SubscriptionCoupon;
import com.recuriq.billing.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionCouponRepository extends JpaRepository<SubscriptionCoupon, Long> {
    Optional<SubscriptionCoupon> findBySubscription_IdAndStatus(Long subscriptionId, Status status);
    List<SubscriptionCoupon> findBySubscription_Id(Long subscriptionId);
    boolean existsBySubscription_IdAndCoupon_Id(Long subscriptionId, Long couponId);
}
