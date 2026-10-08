package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.recuriq.billing.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "subscription_id")
   private Long id;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "customer_id", nullable = false)
   private Customer customer;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "plan_id", nullable = false)
   private Plan plan;

   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status;

   @Column(name = "start_date", nullable = false)
   private LocalDate startDate;

   @Column(name = "trial_end_date")
   private LocalDate trialEndDate;

   @Column(name = "current_period_start", nullable = false)
   private LocalDate currentPeriodStart;

   @Column(name = "current_period_end", nullable = false)
   private LocalDate currentPeriodEnd;
   
   @Builder.Default
   @Column(name = "cancel_at_period_end", nullable = false)
   private Boolean cancelAtPeriodEnd = false;
   
   @Column(name = "canceled_at", nullable = true)
   private LocalDateTime canceledAt;
   
   @Column(name = "paused_from", nullable = true)
   private LocalDate pausedFrom;
   
   @Column(nullable = true)
   private LocalDate pausedTo;
   
   @Column(nullable = false)
   private Long paymentMethodId;

   @Column(nullable = false, length = 3)
   private String currency;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;
}
