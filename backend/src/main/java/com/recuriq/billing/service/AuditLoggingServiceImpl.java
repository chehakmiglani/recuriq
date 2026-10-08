package com.recuriq.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recuriq.billing.entity.AuditLog;
import com.recuriq.billing.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
public class AuditLoggingServiceImpl implements AuditLoggingService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLoggingServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Override
    public void logAction(String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            
            // Serialize JSON safely, handling potential recursive references gracefully by catching exceptions
            auditLog.setOldValue(serializeSafely(oldValue));
            auditLog.setNewValue(serializeSafely(newValue));

            // Populate Actor details from SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                if (auth.getPrincipal() instanceof UserDetails userDetails) {
                    auditLog.setActor(userDetails.getUsername());
                    // Pick the first granted authority as the role for simplicity
                    if (!userDetails.getAuthorities().isEmpty()) {
                        auditLog.setActorRole(userDetails.getAuthorities().iterator().next().getAuthority());
                    }
                } else {
                    auditLog.setActor(auth.getPrincipal().toString());
                }
            } else {
                auditLog.setActor("SYSTEM");
                auditLog.setActorRole("SYSTEM");
            }

            // Populate Request Context details
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIp(request.getRemoteAddr());
                String reqId = request.getHeader("X-Request-ID");
                auditLog.setRequestId(reqId != null ? reqId : UUID.randomUUID().toString());
            } else {
                auditLog.setRequestId(UUID.randomUUID().toString());
            }

            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to write audit log for action: {} on entity: {}", action, entityType, e);
        }
    }

    private String serializeSafely(Object value) {
        if (value == null) return null;
        if (value instanceof String string) return string; // if it's already a JSON string
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize audit log value for object type: {}. Error: {}", value.getClass().getName(), e.getMessage());
            return "{\"error\": \"Unserializable object: " + value.getClass().getSimpleName() + "\"}";
        }
    }
}
