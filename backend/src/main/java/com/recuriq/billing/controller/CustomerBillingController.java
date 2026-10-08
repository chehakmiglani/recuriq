package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.request.ApplyCouponRequest;
import com.recuriq.billing.service.CustomerBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerBillingController {

   private final CustomerBillingService billingService;

   @GetMapping("/invoices")
   public ResponseEntity<List<InvoiceDTO>> getInvoices(
           @AuthenticationPrincipal User user,
           @RequestParam(required = false) String status,
           @RequestParam(required = false) String from,
           @RequestParam(required = false) String to) {
       return ResponseEntity.ok(billingService.getInvoices(user.getEmail(), status, from, to));
   }

   @GetMapping("/invoices/{invoiceId}")
   public ResponseEntity<InvoiceDTO> getInvoiceDetail(
           @AuthenticationPrincipal User user,
           @PathVariable Long invoiceId) {
       return ResponseEntity.ok(billingService.getInvoiceDetail(user.getEmail(), invoiceId));
   }

   @GetMapping("/invoices/{invoiceId}/download")
   public ResponseEntity<byte[]> downloadInvoice(
           @AuthenticationPrincipal User user,
           @PathVariable Long invoiceId) {
       byte[] pdf = billingService.generateInvoicePdf(user.getEmail(), invoiceId);
       return ResponseEntity.ok()
               .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + invoiceId + ".pdf")
               .contentType(MediaType.APPLICATION_PDF)
               .body(pdf);
   }

   @GetMapping("/payments")
   public ResponseEntity<List<PaymentDTO>> getPayments(@AuthenticationPrincipal User user) {
       return ResponseEntity.ok(billingService.getPayments(user.getEmail()));
   }

   @GetMapping("/credit-notes")
   public ResponseEntity<List<CreditNoteDTO>> getCreditNotes(@AuthenticationPrincipal User user) {
       return ResponseEntity.ok(billingService.getCreditNotes(user.getEmail()));
   }

   @PostMapping("/coupons/apply")
   public ResponseEntity<CouponDTO> applyCoupon(
           @AuthenticationPrincipal User user,
           @RequestBody ApplyCouponRequest request) {
       return ResponseEntity.ok(billingService.applyCoupon(user.getEmail(), request.getCode()));
   }

   @PostMapping("/coupons/validate")
   @PreAuthorize("permitAll()")
   public ResponseEntity<CouponDTO> validateCoupon(
           @RequestBody ApplyCouponRequest request) {
       return ResponseEntity.ok(billingService.validateCoupon(request.getCode()));
   }

   @GetMapping("/coupons")
   @PreAuthorize("permitAll()")
   public ResponseEntity<List<CouponDTO>> getAvailableCoupons() {
       return ResponseEntity.ok(billingService.getAvailableCoupons());
   }
}
