package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.recuriq.billing.enums.CouponType;
import com.recuriq.billing.enums.Duration;
import com.recuriq.billing.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "coupon_id")
   private Long id;

   @Column(nullable = false, unique = true, length = 50)
   private String code;

   @Column(length = 100)
   private String name;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private CouponType type;

   @Column(nullable = false)
   private Long amount;

   @Column(length = 3)
   private String currency;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private Duration duration;

   @Column(name = "duration_in_months")
   private Integer durationInMonths;

   @Column(name = "max_redemptions")
   private Integer maxRedemptions;

   @Builder.Default
   @Column(name = "redeemed_count", nullable = false)
   private Integer redeemedCount = 0;

   @Column(name = "valid_from", nullable = false)
   private LocalDate validFrom;

   @Column(name = "valid_to")
   private LocalDate validTo;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;
}

