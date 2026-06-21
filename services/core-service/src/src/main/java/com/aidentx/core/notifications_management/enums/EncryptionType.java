package com.aidentx.core.notifications_management.enums;

/**
 * Enumeration representing different encryption types for email communication.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public enum EncryptionType {
    
    /**
     * STARTTLS encryption method for email communication.
     */
    STARTTLS,
    
    /**
     * SSL/TLS encryption method for email communication.
     */
    SSL_TLS,
    
    /**
     * No encryption - plain text communication.
     */
    NONE
}
