package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.SupportMessageDTO;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.service.CustomerSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSupportController {

   private final CustomerSupportService supportService;

   @GetMapping("/support/faqs")
   public ResponseEntity<List<Map<String, String>>> getFAQs() {
       return ResponseEntity.ok(supportService.getFAQs());
   }

   @PostMapping("/support/contact")
   public ResponseEntity<Void> sendSupportMessage(
           @AuthenticationPrincipal User user,
           @RequestBody SupportMessageDTO message) {
       supportService.sendSupportMessage(user.getEmail(), message);
       return ResponseEntity.ok().build();
   }
}
