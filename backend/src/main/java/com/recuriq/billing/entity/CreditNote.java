package com.recuriq.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.recuriq.billing.enums.Status;

@Entity
@Table(name = "credit_note")
@Data
public class CreditNote {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "credit_note_id")
private Long id;

@Column(unique = true)
private String creditNoteNumber;

@ManyToOne
@JoinColumn(name = "invoice_id")
private Invoice invoice;

private String reason;
private Long amountMinor;

@Enumerated(EnumType.STRING)
private Status status;

@ManyToOne
@JoinColumn(name = "created_by")
private User createdBy;

@Column(nullable = false)
private LocalDateTime createdAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
}

}
