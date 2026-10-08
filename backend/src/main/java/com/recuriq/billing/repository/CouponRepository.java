package com.recuriq.billing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.recuriq.billing.entity.Coupon;
import com.recuriq.billing.enums.Status;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
   Optional<Coupon> findByCode(String code);
   boolean existsByCode(String code);
   Optional<Coupon> findByCodeAndStatus(String code, Status active);
   long countByStatus(Status status);
}