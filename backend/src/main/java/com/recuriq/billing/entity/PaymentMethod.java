package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;

@Entity
@Table(name = "payment_method")
@Data
public class PaymentMethod {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "payment_method_id")
private Long id;

@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;

@Enumerated(EnumType.STRING)
private PaymentType paymentType;

private String cardLast4;
private String cardBrand;
private String gatewayToken;
private String upiId;
private Boolean isDefault;
private Integer expiryMonth;
private Integer expiryYear;

@Enumerated(EnumType.STRING)
private Status status;

private LocalDateTime createdAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
}

}
