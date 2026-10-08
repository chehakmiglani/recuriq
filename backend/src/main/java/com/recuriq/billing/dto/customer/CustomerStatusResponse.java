package com.recuriq.billing.dto.customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatusResponse {
    @JsonProperty("isCustomer")
    private boolean isCustomer;
    private boolean hasActiveSubscription;
    private boolean hasDraftSubscription;
    private String message;
}
