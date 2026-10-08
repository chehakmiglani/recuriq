package com.recuriq.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.customer.SupportMessageDTO;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.service.CustomerSupportService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerSupportController.class)
class CustomerSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerSupportService supportService;

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
    void testGetFaqs() throws Exception {
        Map<String, String> faq = Map.of("Q1", "A1");
        
        when(supportService.getFAQs()).thenReturn(List.of(faq));

        mockMvc.perform(get("/api/customer/support/faqs").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Q1").value("A1"));

        verify(supportService, times(1)).getFAQs();
    }

    @Test
    void testContactSupport() throws Exception {
        SupportMessageDTO request = new SupportMessageDTO();
        request.setSubject("Test Subject");
        
        doNothing().when(supportService).sendSupportMessage(anyString(), any());

        mockMvc.perform(post("/api/customer/support/contact")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(supportService, times(1)).sendSupportMessage(eq("test@test.com"), any(SupportMessageDTO.class));
    }
}
