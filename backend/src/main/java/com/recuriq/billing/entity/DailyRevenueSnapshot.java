package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_revenue_snapshot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyRevenueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "mrr_minor", nullable = false)
    private Long mrrMinor;

    @Column(name = "arr_minor", nullable = false)
    private Long arrMinor;

    @Column(name = "arpu_minor", nullable = false)
    private Long arpuMinor;

    @Column(name = "ltv_minor", nullable = false)
    private Long ltvMinor;

    @Column(name = "active_subscriptions", nullable = false)
    private Integer activeSubscriptions;

    @Column(name = "canceled_subscriptions", nullable = false)
    private Integer canceledSubscriptions;

    @Column(name = "churn_rate", nullable = false)
    private Double churnRate;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }
}
