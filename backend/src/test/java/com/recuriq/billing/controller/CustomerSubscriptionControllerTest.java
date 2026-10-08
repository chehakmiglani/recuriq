package com.recuriq.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.customer.UsageRecordDTO;
import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.request.CancellationRequestInput;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.request.CreateSubscriptionRequest;
import com.recuriq.billing.request.PauseSubscriptionRequest;
import com.recuriq.billing.request.UpgradeSubscriptionRequest;
import com.recuriq.billing.service.CustomerSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerSubscriptionController.class)
class CustomerSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerSubscriptionService subscriptionService;

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
    void testGetSubscription() throws Exception {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.getCurrentSubscription(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/customer/subscription").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).getCurrentSubscription("test@test.com");
    }

    @Test
    void testGetSubscription_NotFound() throws Exception {
        when(subscriptionService.getCurrentSubscription(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/customer/subscription").with(authentication(auth)))
                .andExpect(status().isNotFound());

        verify(subscriptionService, times(1)).getCurrentSubscription("test@test.com");
    }

    @Test
    void testCreateSubscription() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanId(1L);
        
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.createSubscription(anyString(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/customer/subscription")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).createSubscription(eq("test@test.com"), any(CreateSubscriptionRequest.class));
    }

    @Test
    void testUpgradeSubscription() throws Exception {
        UpgradeSubscriptionRequest request = new UpgradeSubscriptionRequest();
        request.setPlanId(2L);
        
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.upgradeSubscription(anyString(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/customer/subscription/upgrade")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).upgradeSubscription(eq("test@test.com"), any(UpgradeSubscriptionRequest.class));
    }

    @Test
    void testCreateCancellationRequest() throws Exception {
        CancellationRequestInput input = new CancellationRequestInput();
        input.setReason("Too expensive");
        input.setAtPeriodEnd(false);
        
        CancellationRequestDTO dto = new CancellationRequestDTO();
        dto.setRequestId(1L);
        dto.setReason("Too expensive");
        dto.setAtPeriodEnd(false);

        when(subscriptionService.createCancellationRequest(anyString(), any(CancellationRequestInput.class))).thenReturn(dto);

        mockMvc.perform(post("/api/customer/subscription/cancel-request")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1))
                .andExpect(jsonPath("$.reason").value("Too expensive"));

        verify(subscriptionService, times(1)).createCancellationRequest(eq("test@test.com"), any(CancellationRequestInput.class));
    }

    @Test
    void testWithdrawCancellationRequest() throws Exception {
        CancellationRequestDTO dto = new CancellationRequestDTO();
        dto.setRequestId(1L);
        when(subscriptionService.withdrawCancellationRequest(anyString())).thenReturn(dto);

        mockMvc.perform(post("/api/customer/subscription/cancel-request/withdraw")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1));

        verify(subscriptionService, times(1)).withdrawCancellationRequest("test@test.com");
    }

    @Test
    void testGetPendingCancellationRequest() throws Exception {
        CancellationRequestDTO dto = new CancellationRequestDTO();
        dto.setRequestId(1L);
        
        when(subscriptionService.getPendingCancellationRequest(anyString())).thenReturn(dto);

        mockMvc.perform(get("/api/customer/subscription/cancel-request")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1));

        verify(subscriptionService, times(1)).getPendingCancellationRequest("test@test.com");
    }

    @Test
    void testGetPendingCancellationRequest_NoContent() throws Exception {
        when(subscriptionService.getPendingCancellationRequest(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/customer/subscription/cancel-request")
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        verify(subscriptionService, times(1)).getPendingCancellationRequest("test@test.com");
    }

    @Test
    void testPauseSubscription() throws Exception {
        PauseSubscriptionRequest request = new PauseSubscriptionRequest();
        request.setPausedTo("2025-01-01");
        
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.pauseSubscription(anyString(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/customer/subscription/pause")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).pauseSubscription(eq("test@test.com"), any(PauseSubscriptionRequest.class));
    }

    @Test
    void testResumeSubscription() throws Exception {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.resumeSubscription(anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/customer/subscription/resume")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).resumeSubscription("test@test.com");
    }

    @Test
    void testAddAddon() throws Exception {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.addAddOn(anyString(), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/customer/addons/1")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).addAddOn("test@test.com", 1L);
    }

    @Test
    void testRemoveAddon() throws Exception {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(100L);
        
        when(subscriptionService.removeAddOn(anyString(), anyLong())).thenReturn(dto);

        mockMvc.perform(delete("/api/customer/addons/1")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(100));

        verify(subscriptionService, times(1)).removeAddOn("test@test.com", 1L);
    }

    @Test
    void testGetMeteredUsage() throws Exception {
        UsageRecordDTO usage = new UsageRecordDTO();
        usage.setUsageId(10L);
        
        when(subscriptionService.getMeteredUsage(anyString(), any(), any())).thenReturn(List.of(usage));

        mockMvc.perform(get("/api/customer/usage")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usageId").value(10));

        verify(subscriptionService, times(1)).getMeteredUsage(eq("test@test.com"), any(), any());
    }
}
