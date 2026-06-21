package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.List;

/**
 * DTO representing a module/group in the hierarchical structure.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class HierarchicalModuleDto {
    
    private String id;
    private String name;
    private List<HierarchicalPageDto> pages;

    // Constructors
    public HierarchicalModuleDto() {}

    public HierarchicalModuleDto(String id, String name, List<HierarchicalPageDto> pages) {
        this.id = id;
        this.name = name;
        this.pages = pages;
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

    public List<HierarchicalPageDto> getPages() {
        return pages;
    }

    public void setPages(List<HierarchicalPageDto> pages) {
        this.pages = pages;
    }
}