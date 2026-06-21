package com.aidentx.core.onboarding.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ApplicationSchemaDefinitionDTO(
        UUID id,
        String name,
        String type,
        String category,
        String description,
        List<String> flags) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ApplicationSchemaDefinitionDTO fromEntity(
            com.aidentx.core.onboarding.model.entity.ApplicationSchemaAttribute entity) {
        List<String> flagsList = new ArrayList<>();
        JsonNode flagsNode = entity.getFlags();
        if (flagsNode != null && flagsNode.isArray()) {
            try {
                flagsList = objectMapper.convertValue(flagsNode, new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                // Handle or log error
                e.printStackTrace();
            }
        }

        return new ApplicationSchemaDefinitionDTO(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getCategory(),
                entity.getDescription(),
                flagsList);
    }
}
