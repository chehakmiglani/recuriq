package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.recuriq.billing.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "customer_id")
   private Long id;

   @OneToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "user_id", nullable = false, unique = true)
   private User user;

   @Column(length = 20)
   private String phone;

   @Column(nullable = false, length = 3)
   private String currency;

   @Column(nullable = false, length = 2)
   private String country;

   @Column(length = 100)
   private String state;

   @Column(length = 100)
   private String city;

   @Column(name = "address_line1", length = 255)
   private String addressLine1;

   @Column(name = "postal_code", length = 20)
   private String postalCode;

   @Builder.Default
   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private Status status = Status.ACTIVE;

   @Builder.Default
   @Column(name = "credit_balance_minor", nullable = false)
   private Long creditBalanceMinor = 0L;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;
}
