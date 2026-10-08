package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.recuriq.billing.enums.CancellationRequestStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CancellationRequest {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "request_id")
   private Long id;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "subscription_id", nullable = false)
   private Subscription subscription;

   @Column(name = "reason")
   private String reason;

   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   private CancellationRequestStatus status;

   @Column(name = "at_period_end", nullable = false)
   private Boolean atPeriodEnd;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private LocalDateTime updatedAt;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "processed_by")
   private User processedBy;

   @Column(name = "processed_at")
   private LocalDateTime processedAt;

   @Column(name = "agent_notes")
   private String agentNotes;
}
