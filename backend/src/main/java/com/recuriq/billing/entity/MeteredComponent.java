package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.recuriq.billing.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "metered_component")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeteredComponent {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "component_id")
   private Long id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "plan_id", nullable = false)
   private Plan plan;

   @Column(nullable = false, length = 100)
   private String name;

   @Column(name = "unit_name", nullable = false, length = 50)
   private String unitName;

   @Column(name = "price_per_unit_minor", nullable = false)
   private Long pricePerUnitMinor;

   @Builder.Default
   @Column(name = "free_tier_quantity", nullable = false)
   private Long freeTierQuantity = 0L;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;
}

