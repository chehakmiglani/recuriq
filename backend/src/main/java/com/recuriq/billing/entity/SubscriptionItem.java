package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.recuriq.billing.enums.ItemType;
import com.recuriq.billing.enums.TaxMode;

@Entity
@Table(name = "subscription_item")
@Data
public class SubscriptionItem {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "item_id")
private Long id;

@ManyToOne
@JoinColumn(name = "subscription_id")
private Subscription subscription;

@Enumerated(EnumType.STRING)
private ItemType itemType;

@ManyToOne
@JoinColumn(name = "plan_id")
private Plan plan;

@ManyToOne
@JoinColumn(name = "addon_id")
private AddOn addOn;

@ManyToOne
@JoinColumn(name = "component_id")
private MeteredComponent component;

private Long unitPriceMinor;
private Integer quantity;

@Enumerated(EnumType.STRING)
private TaxMode taxMode;

@ManyToOne
@JoinColumn(name = "discount_id")
private Coupon discount;

@Column(nullable = false)
private LocalDateTime createdAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
}

}
