package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.recuriq.billing.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "product_id")
   private Long id;

   @Column(nullable = false, length = 100)
   private String name;

   @Column(columnDefinition = "TEXT")
   private String description;

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

