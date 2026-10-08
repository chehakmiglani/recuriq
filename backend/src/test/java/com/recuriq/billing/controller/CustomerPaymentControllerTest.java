package com.recuriq.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.dto.customer.PaymentMethodDTO;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.enums.PaymentType;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.request.AddPaymentMethodRequest;
import com.recuriq.billing.service.CustomerPaymentService;
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

@WebMvcTest(CustomerPaymentController.class)
class CustomerPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerPaymentService paymentService;

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
    void testGetPaymentMethods() throws Exception {
        PaymentMethodDTO method = new PaymentMethodDTO();
        method.setPaymentMethodId(1L);
        when(paymentService.getPaymentMethods(anyString())).thenReturn(List.of(method));

        mockMvc.perform(get("/api/customer/payment-methods").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentMethodId").value(1));

        verify(paymentService, times(1)).getPaymentMethods("test@test.com");
    }

    @Test
    void testAddPaymentMethod() throws Exception {
        AddPaymentMethodRequest request = new AddPaymentMethodRequest();
        request.setPaymentType(PaymentType.CARD);
        request.setCardNumber("4111111111111111");
        
        PaymentMethodDTO method = new PaymentMethodDTO();
        method.setPaymentMethodId(1L);
        
        when(paymentService.addPaymentMethod(anyString(), any())).thenReturn(method);

        mockMvc.perform(post("/api/customer/payment-methods")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethodId").value(1));

        verify(paymentService, times(1)).addPaymentMethod(eq("test@test.com"), any(AddPaymentMethodRequest.class));
    }

    @Test
    void testSetDefaultPaymentMethod() throws Exception {
        doNothing().when(paymentService).setDefaultPaymentMethod(anyString(), anyLong());

        mockMvc.perform(put("/api/customer/payment-methods/1/default")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).setDefaultPaymentMethod("test@test.com", 1L);
    }

    @Test
    void testRemovePaymentMethod() throws Exception {
        doNothing().when(paymentService).deletePaymentMethod(anyString(), anyLong());

        mockMvc.perform(delete("/api/customer/payment-methods/1")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).deletePaymentMethod("test@test.com", 1L);
    }
}
