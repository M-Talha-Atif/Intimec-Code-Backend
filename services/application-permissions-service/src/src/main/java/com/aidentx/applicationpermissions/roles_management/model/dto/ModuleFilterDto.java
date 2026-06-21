package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for simple module information used for filtering.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class ModuleFilterDto {
    
    private UUID id;
    private String name;

    // Constructors
    public ModuleFilterDto() {}

    public ModuleFilterDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}