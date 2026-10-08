package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.request.AddPaymentMethodRequest;
import com.recuriq.billing.service.CustomerPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPaymentController {

   private final CustomerPaymentService paymentService;

   @GetMapping("/payment-methods")
   public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods(@AuthenticationPrincipal User user) {
       return ResponseEntity.ok(paymentService.getPaymentMethods(user.getEmail()));
   }

   @PostMapping("/payment-methods")
   public ResponseEntity<PaymentMethodDTO> addPaymentMethod(
           @AuthenticationPrincipal User user,
           @RequestBody AddPaymentMethodRequest request) {
       return ResponseEntity.ok(paymentService.addPaymentMethod(user.getEmail(), request));
   }

   @PutMapping("/payment-methods/{paymentMethodId}/default")
   public ResponseEntity<Void> setDefaultPaymentMethod(
           @AuthenticationPrincipal User user,
           @PathVariable Long paymentMethodId) {
       paymentService.setDefaultPaymentMethod(user.getEmail(), paymentMethodId);
       return ResponseEntity.ok().build();
   }

   @DeleteMapping("/payment-methods/{paymentMethodId}")
   public ResponseEntity<Void> deletePaymentMethod(
           @AuthenticationPrincipal User user,
           @PathVariable Long paymentMethodId) {
       paymentService.deletePaymentMethod(user.getEmail(), paymentMethodId);
       return ResponseEntity.ok().build();
   }
}
