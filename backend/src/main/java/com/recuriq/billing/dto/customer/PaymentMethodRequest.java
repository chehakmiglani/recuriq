package com.recuriq.billing.dto.customer;

import com.recuriq.billing.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentMethodRequest {
    
    @NotNull(message = "{payment.type.required}")
    private PaymentType paymentType;
    
    // Card fields
    @Pattern(regexp = "^\\d{16}$", message = "{payment.card.invalid}")
    private String cardNumber;
    
    @Size(max = 100, message = "{payment.cardholder.invalid}")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Cardholder name must contain only letters and spaces")
    private String cardholderName;
    
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "{payment.expiry.month.invalid}")
    private String expiryMonth;
    
    @Pattern(regexp = "^\\d{4}$", message = "{payment.expiry.year.invalid}")
    private String expiryYear;
    
    @Pattern(regexp = "^\\d{3,4}$", message = "{payment.cvv.invalid}")
    private String cvv;
    
    // UPI fields
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z]+$", message = "{payment.upi.invalid}")
    private String upiId;
}
