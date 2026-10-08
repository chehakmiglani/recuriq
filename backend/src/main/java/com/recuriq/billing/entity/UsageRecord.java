package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_record")
@Data
public class UsageRecord {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "usage_id")
private Long id;

@ManyToOne
@JoinColumn(name = "subscription_id")
private Subscription subscription;

@ManyToOne
@JoinColumn(name = "component_id")
private MeteredComponent component;

private Long quantity;
private LocalDateTime recordedAt;
private LocalDate billingPeriodStart;
private LocalDate billingPeriodEnd;

@Column(unique = true)
private String idempotencyKey;

}
