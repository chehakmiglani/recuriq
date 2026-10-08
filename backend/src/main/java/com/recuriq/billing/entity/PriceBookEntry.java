package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "price_book_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceBookEntry {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "price_book_id")
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "plan_id", nullable = false)
   private Plan plan;

   @Column(name = "region")
   private String region;

   @Column(nullable = false, length = 3)
   private String currency;

   @Column(name = "price_minor", nullable = false)
   private Long priceMinor;

   @Column(name = "effective_from", nullable = false)
   private LocalDate effectiveFrom;

   @Column(name = "effective_to")
   private LocalDate effectiveTo;
}

