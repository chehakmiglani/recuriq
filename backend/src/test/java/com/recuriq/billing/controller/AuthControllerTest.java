package com.recuriq.billing.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recuriq.billing.config.SecurityConfig;
import com.recuriq.billing.dto.auth.CustomerRegisterRequest;
import com.recuriq.billing.dto.auth.LoginRequest;
import com.recuriq.billing.dto.auth.UserResponse;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.repository.UserRepository;
import com.recuriq.billing.service.AuthService;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .fullName("Test User")
                .role(UserRole.CUSTOMER)
                .status(Status.ACTIVE)
                .build();

        userResponse = new UserResponse(testUser);
    }

    // ==================== CUSTOMER REGISTER ====================

    @Test
    void testRegisterCustomer_Success() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("Password1");

        when(authService.registerCustomer(any(CustomerRegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/customer/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(authService, times(1)).registerCustomer(any(CustomerRegisterRequest.class));
    }

    @Test
    void testRegisterCustomer_ValidationError_BlankEmail() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("");
        request.setFullName("New User");
        request.setPassword("Password1");

        mockMvc.perform(post("/api/customer/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerCustomer(any());
    }

    @Test
    void testRegisterCustomer_ValidationError_InvalidEmail() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("not-an-email");
        request.setFullName("New User");
        request.setPassword("Password1");

        mockMvc.perform(post("/api/customer/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterCustomer_ValidationError_WeakPassword() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("user@test.com");
        request.setFullName("User");
        request.setPassword("short");

        mockMvc.perform(post("/api/customer/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== CUSTOMER LOGIN ====================

    @Test
    void testLoginCustomer_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("Password1");

        when(authService.loginCustomer(any(LoginRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/customer/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(authService, times(1)).loginCustomer(any(LoginRequest.class));
    }

    @Test
    void testLoginCustomer_ValidationError_BlankPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("");

        mockMvc.perform(post("/api/customer/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== MANAGER LOGIN ====================

    @Test
    void testLoginManager_Success() throws Exception {
        User adminUser = User.builder()
                .id(2L)
                .email("admin@streamflix.com")
                .fullName("System Admin")
                .role(UserRole.ADMIN)
                .status(Status.ACTIVE)
                .build();
        UserResponse adminResponse = new UserResponse(adminUser);

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@streamflix.com");
        request.setPassword("Password1");

        when(authService.loginManager(any(LoginRequest.class))).thenReturn(adminResponse);

        mockMvc.perform(post("/api/manager/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@streamflix.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService, times(1)).loginManager(any(LoginRequest.class));
    }

    // ==================== GET ME (Session Check) ====================

    @Test
    void testGetMe_Authenticated() throws Exception {
        when(authService.getAuthenticatedUserResponse("test@test.com")).thenReturn(userResponse);

        // Use a real UsernamePasswordAuthenticationToken (JDK 25 compatible)
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        mockMvc.perform(get("/api/auth/me")
                        .session(new MockHttpSession())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));

        verify(authService, times(1)).getAuthenticatedUserResponse("test@test.com");
    }

    @Test
    void testGetMe_NotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMe_AnonymousUser() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        
        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(auth)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMe_UnauthenticatedToken() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test@test.com", "password");
        auth.setAuthenticated(false); // Token is present but not authenticated
        
        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(auth)))
                .andExpect(status().isUnauthorized());
    }
    @Autowired
    private AuthController authController;

    @Test
    void testGetMe_NullAuthentication() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<?> response = authController.getMe(null, request);
        Assertions.assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testGetMe_UnauthenticatedDirectCall() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "pass");
        auth.setAuthenticated(false);
        ResponseEntity<?> response = authController.getMe(auth, request);
        Assertions.assertEquals(401, response.getStatusCode().value());
    }
}
