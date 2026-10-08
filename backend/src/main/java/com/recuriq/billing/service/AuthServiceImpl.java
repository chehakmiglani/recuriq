package com.recuriq.billing.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.recuriq.billing.dto.auth.CustomerRegisterRequest;
import com.recuriq.billing.dto.auth.LoginRequest;
import com.recuriq.billing.dto.auth.UserResponse;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

   public UserResponse registerCustomer(CustomerRegisterRequest request) {
       if (userRepository.existsByEmail(request.getEmail())) {
           throw CustomException.conflict("Email is already registered.");
       }

       User user = new User();
       user.setFullName(request.getFullName());
       user.setEmail(request.getEmail());
       user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
       user.setRole(UserRole.CUSTOMER);
       user.setStatus(Status.ACTIVE);

       userRepository.save(user);

       // Auto-login after register
       return authenticateAndBuildSession(request.getEmail(), request.getPassword(), UserRole.CUSTOMER);
   }

   public UserResponse loginCustomer(LoginRequest request) {
       return authenticateAndBuildSession(request.getEmail(), request.getPassword(), UserRole.CUSTOMER);
   }

   public UserResponse loginManager(LoginRequest request) {
       return authenticateAndBuildSession(request.getEmail(), request.getPassword(), null); // Null means any Role can try, we check below
   }

   private UserResponse authenticateAndBuildSession(String email, String password, UserRole allowedRole) {
       
       // 1. Validate native Spring Security Credentials
       Authentication authentication = authenticationManager.authenticate(
               new UsernamePasswordAuthenticationToken(email, password)
       );

       // 2. Set Context (This is what tells Spring Boot to issue the Session Cookie!)
       SecurityContextHolder.getContext().setAuthentication(authentication);

       // 3. Check Portal Boundaries
       User user = userRepository.findByEmail(email).orElseThrow();
       
       if (allowedRole == UserRole.CUSTOMER && user.getRole() != UserRole.CUSTOMER) {
            SecurityContextHolder.clearContext();
            throw CustomException.forbidden("Staff cannot login via the customer portal.");
       }
       if (allowedRole == null && user.getRole() == UserRole.CUSTOMER) {
            SecurityContextHolder.clearContext();
            throw CustomException.forbidden("Access Denied: Customers cannot access the Management Portal.");
       }

       return new UserResponse(user);
   }

   public UserResponse getAuthenticatedUserResponse(String email) {
       User user = userRepository.findByEmail(email).orElseThrow();
       return new UserResponse(user);
   }
}
