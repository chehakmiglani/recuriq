package com.recuriq.billing.controller;

import com.recuriq.billing.dto.customer.SubscriptionDTO;
import com.recuriq.billing.dto.support.CustomerDetailResponse;
import com.recuriq.billing.dto.support.CustomerSearchResponse;
import com.recuriq.billing.dto.customer.CustomerProfileDTO;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.DunningStatus;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.entity.BillingJob;
import com.recuriq.billing.entity.DunningRetryLog;
import com.recuriq.billing.service.SupportService;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.dto.customer.CancellationRequestDTO;
import com.recuriq.billing.dto.customer.CancellationResponse;
import com.recuriq.billing.request.ProcessCancellationRequestInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupportController.class)
class SupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupportService supportService;

    private User supportAgent;
    private UsernamePasswordAuthenticationToken supportAuth;

    @BeforeEach
    void setUp() {
        supportAgent = User.builder()
                .id(2L)
                .email("support@test.com")
                .fullName("Support Agent")
                .role(UserRole.SUPPORT)
                .status(Status.ACTIVE)
                .build();
        supportAuth = new UsernamePasswordAuthenticationToken(
                supportAgent, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
    }

    @Test
    void testSearchCustomers() throws Exception {
        CustomerSearchResponse response = new CustomerSearchResponse();
        response.setEmail("test@test.com");
        when(supportService.searchCustomers(anyString())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/support/customers")
                        .param("query", "test")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));

        verify(supportService, times(1)).searchCustomers("test");
    }

    @Test
    void testGetCustomerDetails() throws Exception {
        CustomerDetailResponse response = new CustomerDetailResponse();
        CustomerProfileDTO profile = new CustomerProfileDTO();
        profile.setEmail("test@test.com");
        response.setCustomerProfile(profile);
        when(supportService.getCustomerDetails(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/support/customers/1")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerProfile.email").value("test@test.com"));

        verify(supportService, times(1)).getCustomerDetails(1L);
    }

    @Test
    void testGetRecentAuditLogs() throws Exception {
        AuditLog log = new AuditLog();
        log.setAction("SUBSCRIPTION_CREATED");
        when(supportService.getRecentAuditLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/support/audit-logs")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SUBSCRIPTION_CREATED"));

        verify(supportService, times(1)).getRecentAuditLogs();
    }

    @Test
    void testGetBillingJobs() throws Exception {
        BillingJob job = new BillingJob();
        job.setStatus(BillingJob.Status.COMPLETED);
        when(supportService.getBillingJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/support/billing-jobs")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        verify(supportService, times(1)).getBillingJobs();
    }

    @Test
    void testGetDunningLogs() throws Exception {
        DunningRetryLog log = new DunningRetryLog();
        log.setStatus(DunningStatus.SCHEDULED);
        when(supportService.getDunningLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/support/dunning-logs")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(supportService, times(1)).getDunningLogs();
    }

    @Test
    void testGetPastDueSubscriptions() throws Exception {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setStatus(Status.PAST_DUE);
        when(supportService.getPastDueSubscriptions()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/support/subscriptions/past-due")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAST_DUE"));

        verify(supportService, times(1)).getPastDueSubscriptions();
    }

    @Test
    void testGetPendingCancellationRequests() throws Exception {
        CancellationRequestDTO dto = new CancellationRequestDTO();
        dto.setRequestId(1L);
        dto.setReason("Too expensive");
        
        when(supportService.getPendingCancellationRequests()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/support/cancellation-requests")
                        .with(authentication(supportAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(1))
                .andExpect(jsonPath("$[0].reason").value("Too expensive"));

        verify(supportService, times(1)).getPendingCancellationRequests();
    }

    @Test
    void testApproveCancellationRequest() throws Exception {
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Approved refund");

        CancellationResponse response = new CancellationResponse();
        response.setRefundAmountMinor(50L);

        when(supportService.approveCancellationRequest(eq(1L), eq("support@test.com"), any(ProcessCancellationRequestInput.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/support/cancellation-requests/1/approve")
                        .with(authentication(supportAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmountMinor").value(50));

        verify(supportService, times(1)).approveCancellationRequest(eq(1L), eq("support@test.com"), any(ProcessCancellationRequestInput.class));
    }

    @Test
    void testRejectCancellationRequest() throws Exception {
        ProcessCancellationRequestInput input = new ProcessCancellationRequestInput();
        input.setAgentNotes("Rejected cancellation");

        doNothing().when(supportService).rejectCancellationRequest(eq(1L), eq("support@test.com"), any(ProcessCancellationRequestInput.class));

        mockMvc.perform(post("/api/support/cancellation-requests/1/reject")
                        .with(authentication(supportAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNoContent());

        verify(supportService, times(1)).rejectCancellationRequest(eq(1L), eq("support@test.com"), any(ProcessCancellationRequestInput.class));
    }
}
