package com.recuriq.billing.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.recuriq.billing.dto.auth.CustomerRegisterRequest;
import com.recuriq.billing.dto.auth.LoginRequest;
import com.recuriq.billing.dto.auth.UserResponse;
import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.Status;
import com.recuriq.billing.enums.UserRole;
import com.recuriq.billing.exception.CustomException;
import com.recuriq.billing.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .fullName("Test User")
                .role(UserRole.CUSTOMER)
                .status(Status.ACTIVE)
                .build();
        
        authentication = mock(Authentication.class);
    }

    // ==================== 1. testRegisterCustomer_Success (existing) ====================
    @Test
    void testRegisterCustomer_Success() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("password");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.registerCustomer(request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== 2. testRegisterCustomer_EmailConflict (existing) ====================
    @Test
    void testRegisterCustomer_EmailConflict() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("test@test.com");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(CustomException.class, () -> authService.registerCustomer(request));
    }

    // ==================== 3. testLoginCustomer_Success (existing) ====================
    @Test
    void testLoginCustomer_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginCustomer(request);

        assertNotNull(response);
    }

    // ==================== 4. testLoginCustomer_Forbidden_StaffPortal (existing) ====================
    @Test
    void testLoginCustomer_Forbidden_StaffPortal() {
        user.setRole(UserRole.ADMIN);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(CustomException.class, () -> authService.loginCustomer(request));
    }

    // ==================== 5. testLoginManager_Success (existing) ====================
    @Test
    void testLoginManager_Success() {
        user.setRole(UserRole.ADMIN);
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginManager(request);

        assertNotNull(response);
    }

    // ==================== 6. testLoginManager_Forbidden_Customer (existing) ====================
    @Test
    void testLoginManager_Forbidden_Customer() {
        user.setRole(UserRole.CUSTOMER);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(CustomException.class, () -> authService.loginManager(request));
    }

    // ==================== 7. testGetAuthenticatedUserResponse (existing) ====================
    @Test
    void testGetAuthenticatedUserResponse() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.getAuthenticatedUserResponse("test@test.com");

        assertNotNull(response);
        assertEquals("test@test.com", response.getEmail());
    }

    // ==================== 8. testRegisterCustomer_VerifiesPasswordEncoding ====================
    @Test
    void testRegisterCustomer_VerifiesPasswordEncoding() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("myRawPassword123");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("myRawPassword123")).thenReturn("hashed_value");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(user));

        authService.registerCustomer(request);

        verify(passwordEncoder, times(1)).encode("myRawPassword123");
    }

    // ==================== 9. testRegisterCustomer_SetsCorrectRoleAndStatus ====================
    @Test
    void testRegisterCustomer_SetsCorrectRoleAndStatus() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("password");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(user));

        authService.registerCustomer(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(UserRole.CUSTOMER, savedUser.getRole());
        assertEquals(Status.ACTIVE, savedUser.getStatus());
    }

    // ==================== 10. testRegisterCustomer_VerifiesUserSaved ====================
    @Test
    void testRegisterCustomer_VerifiesUserSaved() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("password");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(user));

        authService.registerCustomer(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("new@test.com", savedUser.getEmail());
        assertEquals("New User", savedUser.getFullName());
        assertEquals("encoded_password", savedUser.getPasswordHash());
    }

    // ==================== 11. testLoginCustomer_VerifiesAuthenticationCalled ====================
    @Test
    void testLoginCustomer_VerifiesAuthenticationCalled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        authService.loginCustomer(request);

        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class));
    }

    // ==================== 12. testLoginCustomer_ReturnsCorrectEmail ====================
    @Test
    void testLoginCustomer_ReturnsCorrectEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginCustomer(request);

        assertEquals("test@test.com", response.getEmail());
    }

    // ==================== 13. testLoginCustomer_ReturnsCorrectRole ====================
    @Test
    void testLoginCustomer_ReturnsCorrectRole() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginCustomer(request);

        assertEquals("CUSTOMER", response.getRole());
    }

    // ==================== 14. testLoginManager_Success_SupportAgentRole ====================
    @Test
    void testLoginManager_Success_SupportAgentRole() {
        user.setRole(UserRole.SUPPORT);
        LoginRequest request = new LoginRequest();
        request.setEmail("support@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("support@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginManager(request);

        assertNotNull(response);
        assertEquals("SUPPORT", response.getRole());
    }

    // ==================== 15. testLoginManager_Success_FinanceManagerRole ====================
    @Test
    void testLoginManager_Success_FinanceManagerRole() {
        user.setRole(UserRole.FINANCE);
        LoginRequest request = new LoginRequest();
        request.setEmail("finance@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("finance@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginManager(request);

        assertNotNull(response);
        assertEquals("FINANCE", response.getRole());
    }

    // ==================== 16. testLoginManager_ReturnsCorrectRole ====================
    @Test
    void testLoginManager_ReturnsCorrectRole() {
        user.setRole(UserRole.ADMIN);
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.loginManager(request);

        assertEquals("ADMIN", response.getRole());
    }

    // ==================== 17. testGetAuthenticatedUserResponse_UserNotFound ====================
    @Test
    void testGetAuthenticatedUserResponse_UserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> authService.getAuthenticatedUserResponse("unknown@test.com"));
    }

    // ==================== 18. testRegisterCustomer_AuthenticationCalledAfterSave ====================
    @Test
    void testRegisterCustomer_AuthenticationCalledAfterSave() {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("new@test.com");
        request.setFullName("New User");
        request.setPassword("password");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(user));

        authService.registerCustomer(request);

        InOrder inOrder = inOrder(userRepository, authenticationManager);
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ==================== 19. testLoginCustomer_AuthenticationFails ====================
    @Test
    void testLoginCustomer_AuthenticationFails() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.loginCustomer(request));
    }

    // ==================== 20. testGetAuthenticatedUserResponse_ReturnsFullName ====================
    @Test
    void testGetAuthenticatedUserResponse_ReturnsFullName() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserResponse response = authService.getAuthenticatedUserResponse("test@test.com");

        assertEquals("Test User", response.getFullName());
    }
}
