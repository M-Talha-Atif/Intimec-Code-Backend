package com.aidentx.applicationpermissions.roles_management.enums;

/**
 * Enumeration representing HTTP methods for actions.
 * 
 * <p>This enum defines the standard HTTP methods that can be associated
 * with actions in the permission system. Actions can be tied to specific
 * HTTP endpoints and methods for fine-grained access control.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public enum HttpMethod {
    
    /**
     * HTTP GET method - typically used for retrieving/reading data.
     */
    GET,
    
    /**
     * HTTP POST method - typically used for creating new resources.
     */
    POST,
    
    /**
     * HTTP PUT method - typically used for updating/replacing entire resources.
     */
    PUT,
    
    /**
     * HTTP PATCH method - typically used for partial updates to resources.
     */
    PATCH,
    
    /**
     * HTTP DELETE method - typically used for removing/deleting resources.
     */
    DELETE
}