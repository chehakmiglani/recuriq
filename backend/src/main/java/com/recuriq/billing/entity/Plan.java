package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.TaxMode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "plan_id")
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "product_id", nullable = false)
   private Product product;

   @Column(nullable = false, length = 100)
   private String name;

   @Enumerated(EnumType.STRING)
   @Column(name = "billing_period", nullable = false)
   private BillingPeriod billingPeriod;

   @Column(name = "default_price_minor", nullable = false)
   private Long defaultPriceMinor;

   @Column(name = "default_currency", nullable = false, length = 3)
   private String defaultCurrency;

   @Builder.Default
   @Column(name = "trial_days", nullable = false)
   private Integer trialDays = 7;

   @Builder.Default
   @Column(name = "setup_fee_minor", nullable = false)
   private Long setupFeeMinor = 0L;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(name = "tax_mode", nullable = false)
   private TaxMode taxMode = TaxMode.INCLUSIVE;

   @Column(name = "effective_from", nullable = false)
   private LocalDate effectiveFrom;

   @Column(name = "effective_to")
   private LocalDate effectiveTo;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;

}

