package com.recuriq.billing.service;
import com.recuriq.billing.dto.customer.PaymentMethodDTO;
import com.recuriq.billing.request.AddPaymentMethodRequest;
import java.util.List;

public interface CustomerPaymentService {
   List<PaymentMethodDTO> getPaymentMethods(String email);
   PaymentMethodDTO addPaymentMethod(String email, AddPaymentMethodRequest request);
   void setDefaultPaymentMethod(String email, Long paymentMethodId);
   void deletePaymentMethod(String email, Long paymentMethodId);
}
