package com.recuriq.billing.service;

import com.recuriq.billing.dto.auth.CustomerRegisterRequest;
import com.recuriq.billing.dto.auth.LoginRequest;
import com.recuriq.billing.dto.auth.UserResponse;

public interface AuthService {
	   UserResponse registerCustomer(CustomerRegisterRequest request);
	   UserResponse loginCustomer(LoginRequest request);
	   UserResponse loginManager(LoginRequest request);
	   UserResponse getAuthenticatedUserResponse(String email);
	}
