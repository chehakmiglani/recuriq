package com.recuriq.billing.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLoggingServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLoggingServiceImpl auditLoggingService;

    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUp() {
        originalSecurityContext = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(originalSecurityContext);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testLogAction_SystemFallback_NoAuth_NoRequestAttributes() {
        auditLoggingService.logAction("TEST_ACTION", "TestEntity", 123L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("TEST_ACTION", savedLog.getAction());
        assertEquals("TestEntity", savedLog.getEntityType());
        assertEquals(123L, savedLog.getEntityId());
        assertEquals("SYSTEM", savedLog.getActor());
        assertEquals("SYSTEM", savedLog.getActorRole());
        assertNull(savedLog.getOldValue());
        assertNull(savedLog.getNewValue());
        assertNull(savedLog.getIp());
        assertNotNull(savedLog.getRequestId());
    }

    @Test
    void testLogAction_AuthenticatedWithUserDetails() {
        // Setup Security Context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test_user");
        
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        doReturn(Collections.singletonList(authority)).when(userDetails).getAuthorities();

        SecurityContextHolder.setContext(securityContext);

        // Setup Request Context
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Request-ID")).thenReturn("req-12345");

        RequestContextHolder.setRequestAttributes(attributes);

        auditLoggingService.logAction("CREATE", "Product", 456L, "old_data", "new_data");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("test_user", savedLog.getActor());
        assertEquals("ROLE_ADMIN", savedLog.getActorRole());
        assertEquals("192.168.1.1", savedLog.getIp());
        assertEquals("req-12345", savedLog.getRequestId());
        assertEquals("old_data", savedLog.getOldValue());
        assertEquals("new_data", savedLog.getNewValue());
    }

    @Test
    void testLogAction_AuthenticatedWithUserDetails_NoAuthorities() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user_no_roles");
        doReturn(Collections.emptyList()).when(userDetails).getAuthorities();

        SecurityContextHolder.setContext(securityContext);

        auditLoggingService.logAction("UPDATE", "Plan", 789L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("user_no_roles", savedLog.getActor());
        assertNull(savedLog.getActorRole());
    }

    @Test
    void testLogAction_AuthenticatedWithNonUserDetailsPrincipal() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("simple_string_principal");

        SecurityContextHolder.setContext(securityContext);

        auditLoggingService.logAction("DELETE", "Subscription", 999L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("simple_string_principal", savedLog.getActor());
        assertNull(savedLog.getActorRole());
    }

    @Test
    void testLogAction_AnonymousUser() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        SecurityContextHolder.setContext(securityContext);

        auditLoggingService.logAction("READ", "Report", 111L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("SYSTEM", savedLog.getActor());
        assertEquals("SYSTEM", savedLog.getActorRole());
    }

    @Test
    void testLogAction_Unauthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);

        auditLoggingService.logAction("READ", "Report", 111L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("SYSTEM", savedLog.getActor());
        assertEquals("SYSTEM", savedLog.getActorRole());
    }

    @Test
    void testLogAction_RequestAttributesWithoutHeader() {
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Request-ID")).thenReturn(null);

        RequestContextHolder.setRequestAttributes(attributes);

        auditLoggingService.logAction("TEST", "Entity", 1L, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("127.0.0.1", savedLog.getIp());
        assertNotNull(savedLog.getRequestId());
        assertFalse(savedLog.getRequestId().isEmpty());
    }

    @Test
    void testLogAction_Serialization_ObjectValues() {
        Map<String, Object> oldVal = Map.of("status", "INACTIVE", "price", 100);
        Map<String, Object> newVal = Map.of("status", "ACTIVE", "price", 120);

        auditLoggingService.logAction("UPDATE", "Plan", 2L, oldVal, newVal);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertTrue(savedLog.getOldValue().contains("\"status\":\"INACTIVE\""));
        assertTrue(savedLog.getNewValue().contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void testLogAction_Serialization_UnserializableObject() {
        ThrowingObject throwingObject = new ThrowingObject();

        auditLoggingService.logAction("UPDATE", "Plan", 3L, throwingObject, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals("{\"error\": \"Unserializable object: ThrowingObject\"}", savedLog.getOldValue());
        assertNull(savedLog.getNewValue());
    }

    @Test
    void testLogAction_RepositoryThrowsException() {
        doThrow(new RuntimeException("Database Connection Error")).when(auditLogRepository).save(any(AuditLog.class));

        // This call should not throw an exception as it's wrapped in a try-catch block inside logAction
        assertDoesNotThrow(() ->
            auditLoggingService.logAction("FAIL_SAVE", "Entity", 5L, null, null)
        );

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // Helper static class to trigger JSON serialization failure
    @SuppressWarnings("unused")
    private static class ThrowingObject {
        public String getValue() {
            throw new RuntimeException("Jackson serialization failure");
        }
    }
}
