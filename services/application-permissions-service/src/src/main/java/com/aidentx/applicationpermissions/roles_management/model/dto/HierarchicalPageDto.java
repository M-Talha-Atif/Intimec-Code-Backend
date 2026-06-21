package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.List;

/**
 * DTO representing a page in the hierarchical structure.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class HierarchicalPageDto {
    
    private String id;
    private String name;
    private List<HierarchicalActionDto> actions;

    // Constructors
    public HierarchicalPageDto() {}

    public HierarchicalPageDto(String id, String name, List<HierarchicalActionDto> actions) {
        this.id = id;
        this.name = name;
        this.actions = actions;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<HierarchicalActionDto> getActions() {
        return actions;
    }

    public void setActions(List<HierarchicalActionDto> actions) {
        this.actions = actions;
    }
}