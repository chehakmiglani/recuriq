package com.recuriq.billing.dto.support;

import com.recuriq.billing.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSearchResponse {
    private Long customerId;
    private String fullName;
    private String email;
    private Status status;
}
