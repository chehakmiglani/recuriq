package com.recuriq.billing.service;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.*;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.*;
import com.recuriq.billing.request.AddPaymentMethodRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPaymentServiceImpl implements CustomerPaymentService {

   private final PaymentMethodRepository paymentMethodRepository;
   private final CustomerRepository customerRepository;
   private final UserRepository userRepository;

   public List<PaymentMethodDTO> getPaymentMethods(String email) {
       Customer customer = getCustomerByEmail(email);
       return paymentMethodRepository.findByCustomer_Id(customer.getId()).stream()
               .map(this::mapToPaymentMethodDTO)
               .toList();
   }

   @Transactional
   public PaymentMethodDTO addPaymentMethod(String email, AddPaymentMethodRequest request) {
       Customer customer = getCustomerByEmail(email);

       PaymentMethod method = new PaymentMethod();
       method.setCustomer(customer);
       method.setPaymentType(request.getPaymentType());
       method.setIsDefault(request.getIsDefault());
       method.setStatus(Status.ACTIVE);
       method.setCreatedAt(LocalDateTime.now());

       if (PaymentType.CARD.equals(request.getPaymentType())) {
           method.setCardLast4(request.getCardNumber().substring(request.getCardNumber().length() - 4));
           method.setCardBrand(detectCardBrand(request.getCardNumber()));
           method.setExpiryMonth(request.getExpiryMonth());
           method.setExpiryYear(request.getExpiryYear());
           method.setGatewayToken("mock_token_card_" + request.getCardNumber());
       } else if (PaymentType.UPI.equals(request.getPaymentType())) {
           method.setUpiId(request.getUpiId());
           method.setGatewayToken("mock_token_upi_" + request.getUpiId());
       }

       List<PaymentMethod> existing = paymentMethodRepository.findByCustomer_Id(customer.getId());
       if (existing.isEmpty() || Boolean.TRUE.equals(request.getIsDefault())) {
           existing.stream().filter(m -> Boolean.TRUE.equals(m.getIsDefault())).forEach(m -> {
               m.setIsDefault(false);
               paymentMethodRepository.save(m);
           });
           method.setIsDefault(true);
       }

       PaymentMethod saved = paymentMethodRepository.save(method);
       return mapToPaymentMethodDTO(saved);
   }

   @Transactional
   public void setDefaultPaymentMethod(String email, Long paymentMethodId) {
       Customer customer = getCustomerByEmail(email);
       
       List<PaymentMethod> methods = paymentMethodRepository.findByCustomer_Id(customer.getId());
       methods.forEach(m -> {
           m.setIsDefault(false);
           paymentMethodRepository.save(m);
       });

       PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
               .orElseThrow(() -> CustomException.notFound("Payment method not found"));
       
       if (!method.getCustomer().getId().equals(customer.getId())) {
           throw CustomException.forbidden("Unauthorized");
       }
       
       method.setIsDefault(true);
       paymentMethodRepository.save(method);
   }

   @Transactional
   public void deletePaymentMethod(String email, Long paymentMethodId) {
       Customer customer = getCustomerByEmail(email);
       PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
               .orElseThrow(() -> CustomException.notFound("Payment method not found"));
       
       if (!method.getCustomer().getId().equals(customer.getId())) {
           throw CustomException.forbidden("Unauthorized");
       }

       List<PaymentMethod> remaining = paymentMethodRepository.findByCustomer_Id(customer.getId());
       if (Boolean.TRUE.equals(method.getIsDefault()) && remaining.size() == 1) {
           throw CustomException.badRequest("Cannot delete your only payment method");
       }

       if (Boolean.TRUE.equals(method.getIsDefault()) && remaining.size() > 1) {
           PaymentMethod newDefault = remaining.stream()
                   .filter(m -> !m.getId().equals(paymentMethodId))
                   .findFirst()
                   .orElse(null);
           if (newDefault != null) {
               newDefault.setIsDefault(true);
               paymentMethodRepository.save(newDefault);
           }
       }

       paymentMethodRepository.delete(method);
   }

   private Customer getCustomerByEmail(String email) {
       User user = userRepository.findByEmail(email)
               .orElseThrow(() -> CustomException.notFound("User not found"));
       return customerRepository.findByUser_Id(user.getId())
               .orElseThrow(() -> CustomException.notFound("Customer not found"));
   }

   private String detectCardBrand(String cardNumber) {
       if (cardNumber.startsWith("4")) return "VISA";
       if (cardNumber.startsWith("5")) return "MASTERCARD";
       if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) return "AMEX";
       return "UNKNOWN";
   }

   private PaymentMethodDTO mapToPaymentMethodDTO(PaymentMethod method) {
       PaymentMethodDTO dto = new PaymentMethodDTO();
       dto.setPaymentMethodId(method.getId());
       dto.setPaymentType(method.getPaymentType());
       dto.setCardLast4(method.getCardLast4());
       dto.setCardBrand(method.getCardBrand());
       dto.setUpiId(method.getUpiId());
       dto.setIsDefault(method.getIsDefault());
       dto.setExpiryMonth(method.getExpiryMonth());
       dto.setExpiryYear(method.getExpiryYear());
       dto.setStatus(method.getStatus());
       return dto;
   }
}
