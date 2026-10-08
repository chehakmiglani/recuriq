package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_line_item")
@Data
public class InvoiceLineItem {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "line_item_id")
private Long id;

@ManyToOne
@JoinColumn(name = "invoice_id")
private Invoice invoice;

private String description;

@Enumerated(EnumType.STRING)
private LineType lineType;

private Integer quantity;
private Long unitPriceMinor;
private Long amountMinor;

@ManyToOne
@JoinColumn(name = "tax_rate_id")
private TaxRate taxRate;

@ManyToOne
@JoinColumn(name = "discount_id")
private Coupon discount;

private LocalDate periodStart;
private LocalDate periodEnd;

@Column(columnDefinition = "JSON")
private String metadata;

public enum LineType { PLAN, ADDON, METERED, PRORATION, DISCOUNT, TAX, CREDIT }

}
