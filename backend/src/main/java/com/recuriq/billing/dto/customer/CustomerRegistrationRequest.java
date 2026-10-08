package com.recuriq.billing.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegistrationRequest {
    
    @NotBlank(message = "{customer.phone.required}")
    @Pattern(regexp = "^\\d{10,15}$", message = "{customer.phone.invalid}")
    private String phone;
    
    @NotBlank(message = "{customer.country.required}")
    @Size(min = 2, max = 2, message = "{customer.country.invalid}")
    private String country;
    
    @NotBlank(message = "{customer.state.required}")
    @Size(max = 100, message = "{customer.state.invalid}")
    private String state;
    
    @NotBlank(message = "{customer.city.required}")
    @Size(max = 100, message = "{customer.city.invalid}")
    private String city;
    
    @NotBlank(message = "{customer.address.required}")
    @Size(max = 255, message = "{customer.address.invalid}")
    private String addressLine1;
    
    @NotBlank(message = "{customer.postal.required}")
    @Size(max = 20, message = "{customer.postal.invalid}")
    private String postalCode;
    
    @NotBlank(message = "{customer.currency.required}")
    @Size(min = 3, max = 3, message = "{customer.currency.invalid}")
    private String currency;
}
