package com.recuriq.billing.service;
import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.PaymentMethod;

public interface SubscriptionFlowService {
   Customer registerCustomerDetails(String email, CustomerRegistrationRequest request);
   PaymentMethod createPaymentMethod(Long customerId, PaymentMethodRequest request);
   SubscriptionResponse completeSubscription(Long customerId, SubscriptionCompletionRequest request);
   CustomerStatusResponse checkCustomerStatus(String email);
}
