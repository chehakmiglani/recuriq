package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.TaxMode;

import java.time.LocalDateTime;

@Entity
@Table(name = "add_on")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddOn {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "add_on_id")
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "product_id", nullable = false)
   private Product product;

   @Column(nullable = false, length = 100)
   private String name;

   @Column(name = "price_minor", nullable = false)
   private Long priceMinor;

   @Column(nullable = false, length = 3)
   private String currency;

   @Enumerated(EnumType.STRING)
   @Column(name = "billing_period", nullable = false)
   private BillingPeriod billingPeriod;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(name = "tax_mode", nullable = false)
   private TaxMode taxMode = TaxMode.EXCLUSIVE;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;
}
