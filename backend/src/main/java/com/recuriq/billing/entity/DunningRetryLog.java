package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.recuriq.billing.enums.DunningStatus;

@Entity
@Table(name = "dunning_retry_log")
@Getter
@Setter
public class DunningRetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Payment payment;

    @com.fasterxml.jackson.annotation.JsonProperty("invoiceId")
    public Long getInvoiceId() {
        return invoice != null ? invoice.getId() : null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("paymentId")
    public Long getPaymentId() {
        return payment != null ? payment.getId() : null;
    }

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DunningStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}
