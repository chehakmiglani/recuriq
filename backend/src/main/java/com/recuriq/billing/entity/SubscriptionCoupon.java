package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import com.recuriq.billing.enums.Status;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_coupon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionCoupon {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "subscription_id", nullable = false)
   private Subscription subscription;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "coupon_id", nullable = false)
   private Coupon coupon;

   @Column(name = "applied_at", nullable = false)
   private LocalDateTime appliedAt;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "applied_by")
   private User appliedBy;

   @Column(name = "expires_at")
   private LocalDateTime expiresAt;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   @Builder.Default
   private Status status = Status.ACTIVE;
}
