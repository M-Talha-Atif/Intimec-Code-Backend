package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;

/**
 * Simple wrapper class to provide user.property syntax for SpEL evaluation.
 * This allows conditional scripts to use: user.email, user.status, user.firstName, etc.
 */
public class UserWrapper {
    private final UserDto user;
    
    public UserWrapper(UserDto user) {
        this.user = user;
    }
    
    public UserDto getUser() {
        return user;
    }
}