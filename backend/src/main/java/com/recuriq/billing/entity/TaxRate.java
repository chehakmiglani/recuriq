package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_rate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxRate {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "tax_id")
   private Long id;

   @Column(nullable = false, length = 100)
   private String name;

   @Column(nullable = false, length = 50)
   private String region;

   @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
   private BigDecimal ratePercent;

   @Builder.Default
   @Column(nullable = false)
   private Boolean inclusive = false;

   @Column(name = "effective_from", nullable = false)
   private LocalDate effectiveFrom;

   @Column(name = "effective_to")
   private LocalDate effectiveTo;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;
}
