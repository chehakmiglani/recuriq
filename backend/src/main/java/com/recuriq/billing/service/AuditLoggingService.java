package com.recuriq.billing.service;

public interface AuditLoggingService {
    void logAction(String action, String entityType, Long entityId, Object oldValue, Object newValue);
}
