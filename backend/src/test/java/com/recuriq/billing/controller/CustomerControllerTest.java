package com.recuriq.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.customer.*;
import com.recuriq.billing.entity.Customer;
import com.recuriq.billing.entity.PaymentMethod;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.BillingPeriod;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.service.CustomerService;
import com.recuriq.billing.service.SubscriptionFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.recuriq.billing.config.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@EnableMethodSecurity
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private SubscriptionFlowService subscriptionFlowService;

    @MockitoBean
    private UserRepository userRepository;

    private User authUser;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        authUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .fullName("Test Customer")
                .role(UserRole.CUSTOMER)
                .status(Status.ACTIVE)
                .build();
        auth = new UsernamePasswordAuthenticationToken(authUser, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void testGetProfile() throws Exception {
        CustomerProfileDTO mockProfile = new CustomerProfileDTO();
        mockProfile.setFullName("Test Customer");
        when(customerService.getProfile(anyString())).thenReturn(mockProfile);

        mockMvc.perform(get("/api/customer/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test Customer"));

        verify(customerService, times(1)).getProfile("test@test.com");
    }

    @Test
    void testUpdateProfile() throws Exception {
        CustomerProfileDTO requestDto = new CustomerProfileDTO();
        requestDto.setFullName("Updated Customer");
        requestDto.setPhone("1234567890");

        when(customerService.updateProfile(anyString(), any())).thenReturn(requestDto);

        mockMvc.perform(put("/api/customer/me")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Customer"));

        verify(customerService, times(1)).updateProfile(eq("test@test.com"), any(CustomerProfileDTO.class));
    }

    @Test
    void testGetAvailablePlans() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Pro");
        when(customerService.getAvailablePlans(anyString())).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));

        verify(customerService, times(1)).getAvailablePlans("IN");
    }

    @Test
    void testGetFeaturedPlans() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Basic");
        when(customerService.getFeaturedPlans(anyString())).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans/featured").with(authentication(auth))) // permitAll
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Basic"));

        verify(customerService, times(1)).getFeaturedPlans("IN");
    }

    @Test
    void testGetAllPlans() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Enterprise");
        when(customerService.getAllActivePlans(anyString())).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans/all").with(authentication(auth))) // permitAll
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Enterprise"));

        verify(customerService, times(1)).getAllActivePlans("IN");
    }

    @Test
    void testGetAvailablePlans_WithRegionParam() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Pro");
        when(customerService.getAvailablePlans("US")).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans")
                        .param("region", "US")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));

        verify(customerService, times(1)).getAvailablePlans("US");
    }

    @Test
    void testGetAvailablePlans_WithoutRegionParam_WithCustomer() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Pro");
        CustomerProfileDTO mockProfile = new CustomerProfileDTO();
        mockProfile.setCountry("CA");
        
        when(customerService.getProfile("test@test.com")).thenReturn(mockProfile);
        when(customerService.getAvailablePlans("CA")).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));

        verify(customerService, times(1)).getAvailablePlans("CA");
    }

    @Test
    void testGetAvailablePlans_WithoutRegionParam_WithoutCustomer() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Pro");
        
        when(customerService.getProfile("test@test.com")).thenThrow(new RuntimeException("Profile not found"));
        when(customerService.getAvailablePlans("IN")).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));

        verify(customerService, times(1)).getAvailablePlans("IN");
    }

    @Test
    void testGetAvailablePlans_WithEmptyRegionParam_WithCustomer() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Pro");
        CustomerProfileDTO mockProfile = new CustomerProfileDTO();
        mockProfile.setCountry("CA");
        
        when(customerService.getProfile("test@test.com")).thenReturn(mockProfile);
        when(customerService.getAvailablePlans("CA")).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans")
                        .param("region", "   ")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));

        verify(customerService, times(1)).getAvailablePlans("CA");
    }

    @Test
    void testGetFeaturedPlans_WithoutUser() throws Exception {
        PlanDTO plan = new PlanDTO();
        plan.setName("Basic");
        when(customerService.getFeaturedPlans("IN")).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/customer/plans/featured")) // No authentication
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Basic"));

        verify(customerService, times(1)).getFeaturedPlans("IN");
    }

    @Test
    void testGetAvailableAddOns() throws Exception {
        AddOnDTO addOn = new AddOnDTO();
        addOn.setName("Extra Storage");
        when(customerService.getAvailableAddOns(anyString())).thenReturn(List.of(addOn));

        mockMvc.perform(get("/api/customer/addons").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Extra Storage"));

        verify(customerService, times(1)).getAvailableAddOns("test@test.com");
    }

    @Test
    void testCheckCustomerStatus() throws Exception {
        CustomerStatusResponse statusResponse = new CustomerStatusResponse();
        statusResponse.setHasActiveSubscription(true);
        when(subscriptionFlowService.checkCustomerStatus(anyString())).thenReturn(statusResponse);

        mockMvc.perform(get("/api/customer/status").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveSubscription").value(true));

        verify(subscriptionFlowService, times(1)).checkCustomerStatus("test@test.com");
    }

    @Test
    void testRegisterCustomerDetails() throws Exception {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest();
        request.setCountry("US");
        request.setAddressLine1("123 Main St");
        request.setCity("Seattle");
        request.setState("WA");
        request.setPostalCode("98101");
        request.setPhone("1234567890");
        request.setCurrency("USD");
        
        Customer customer = new Customer();
        customer.setId(10L);
        
        when(subscriptionFlowService.registerCustomerDetails(anyString(), any())).thenReturn(customer);

        mockMvc.perform(post("/api/customer/register-details")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.message").value("Customer details registered successfully"));

        verify(subscriptionFlowService, times(1)).registerCustomerDetails(eq("test@test.com"), any(CustomerRegistrationRequest.class));
    }

    @Test
    void testCreatePaymentMethod() throws Exception {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setCardNumber("1234567890123456");
        request.setPaymentType(PaymentType.CARD);
        
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(20L);
        paymentMethod.setCardLast4("4242");
        paymentMethod.setCardBrand("Visa");

        when(subscriptionFlowService.createPaymentMethod(eq(5L), any())).thenReturn(paymentMethod);

        mockMvc.perform(post("/api/customer/payment-method")
                        .param("customerId", "5")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethodId").value(20))
                .andExpect(jsonPath("$.last4").value("4242"))
                .andExpect(jsonPath("$.brand").value("Visa"))
                .andExpect(jsonPath("$.message").value("Payment method created successfully"));

        verify(subscriptionFlowService, times(1)).createPaymentMethod(eq(5L), any(PaymentMethodRequest.class));
    }

    @Test
    void testCompleteSubscription() throws Exception {
        SubscriptionCompletionRequest request = new SubscriptionCompletionRequest();
        request.setPlanId(1L);
        request.setPaymentMethodId(20L);
        request.setBillingPeriod(BillingPeriod.MONTHLY);
        
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(100L);
        response.setStatus("ACTIVE");

        when(subscriptionFlowService.completeSubscription(eq(5L), any())).thenReturn(response);

        mockMvc.perform(post("/api/customer/subscription/complete")
                        .param("customerId", "5")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionFlowService, times(1)).completeSubscription(eq(5L), any(SubscriptionCompletionRequest.class));
    }
}
