package com.recuriq.billing.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "revenue_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RevenueSnapshot {
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
    @Column(name = "active_customers", nullable = false)

    private Integer activeCustomers;
    @Column(name = "new_customers", nullable = false)

    private Integer newCustomers;
    @Column(name = "churned_customers", nullable = false)

    private Integer churnedCustomers;
    @Column(name = "gross_churn_percent", nullable = false, precision = 5, scale = 2)

    private BigDecimal grossChurnPercent;
    @Column(name = "net_churn_percent", nullable = false, precision = 5, scale = 2)

    private BigDecimal netChurnPercent;
    @Column(name = "ltv_minor", nullable = false)

    private Long ltvMinor;
    @Column(name = "total_revenue_minor", nullable = false)

    private Long totalRevenueMinor;
    @Column(name = "total_refunds_minor", nullable = false)

    private Long totalRefundsMinor;
    @Column(name = "created_at", nullable = false, updatable = false)

    private LocalDateTime createdAt;
}
