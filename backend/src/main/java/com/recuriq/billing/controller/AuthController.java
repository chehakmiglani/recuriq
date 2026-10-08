package com.recuriq.billing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recuriq.billing.dto.auth.CustomerRegisterRequest;
import com.recuriq.billing.dto.auth.LoginRequest;
import com.recuriq.billing.dto.auth.UserResponse;
import com.recuriq.billing.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {

   private static final String NOT_AUTHENTICATED_MSG = "Not authenticated";

   private final AuthService authService;

   // --- CUSTOMER PORTAL ---
   @PostMapping("/customer/register")
   public ResponseEntity<UserResponse> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request, HttpServletRequest httpRequest) {
       UserResponse response = authService.registerCustomer(request);
       httpRequest.getSession(true);
       return ResponseEntity.ok(response);
   }

   @PostMapping("/customer/login")
   public ResponseEntity<UserResponse> loginCustomer(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
       UserResponse response = authService.loginCustomer(request);
       httpRequest.getSession(true);
       return ResponseEntity.ok(response);
   }

   // --- MANAGEMENT PORTAL ---
   @PostMapping("/manager/login")
   public ResponseEntity<UserResponse> loginManager(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
       UserResponse response = authService.loginManager(request);
       httpRequest.getSession(true);
       return ResponseEntity.ok(response);
   }

   // --- SESSION CHECK ENDPOINT ---
   // React calls this on page refresh to see if the JSESSIONID cookie is still valid
   @GetMapping("/auth/me")
   public ResponseEntity<Object> getMe(Authentication authentication, HttpServletRequest request) {
       if (authentication == null) {
           return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
       }
       if (!authentication.isAuthenticated()) {
           return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
       }
       if ("anonymousUser".equals(authentication.getPrincipal())) {
           return ResponseEntity.status(401).body(NOT_AUTHENTICATED_MSG);
       }
       return ResponseEntity.ok(authService.getAuthenticatedUserResponse(authentication.getName()));
   }
}

