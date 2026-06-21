package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for extracting core attribute metadata from entity classes using
 * reflection.
 * This provides dynamic schema information for identity types.
 */
@Service
public class CoreAttributeMetadataService {

    private final IUnifiedLogger logger;

    public CoreAttributeMetadataService(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(CoreAttributeMetadataService.class);
    }

    /**
     * DTO for core attribute metadata
     */
    public record CoreAttributeMetadata(
            String fieldName,
            String displayName,
            DataType dataType,
            boolean isRequired,
            boolean isRelationship,
            String relationshipType,
            String description) {
    }

    /**
     * Gets the core attributes for a given identity type
     */
    public List<CoreAttributeMetadata> getCoreAttributesForIdentityType(IdentityType identityType) {
        Class<?> entityClass = getEntityClassForIdentityType(identityType);
        if (entityClass == null) {
            return Collections.emptyList();
        }

        List<CoreAttributeMetadata> attributes = new ArrayList<>();

        // Get all fields including inherited ones
        List<Field> allFields = getAllFields(entityClass);

        for (Field field : allFields) {
            // Skip static fields and fields we don't want to expose
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    field.getName().equals("serialVersionUID") ||
                    field.getName().startsWith("_")) {
                continue;
            }

            CoreAttributeMetadata metadata = createAttributeMetadata(field);
            if (metadata != null) {
                attributes.add(metadata);
            }
        }

        // Filter out hidden fields and mark all remaining fields as required
        attributes = attributes.stream()
                .filter(attr -> !isHiddenField(attr.fieldName()))
                .map(attr -> new CoreAttributeMetadata(
                        attr.fieldName(),
                        attr.displayName(),
                        attr.dataType(),
                        true, // Mark all core attributes as required
                        attr.isRelationship(),
                        attr.relationshipType(),
                        attr.description()))
                .collect(java.util.stream.Collectors.toList());

        // Sort by field name for consistent ordering
        attributes.sort(Comparator.comparing(CoreAttributeMetadata::fieldName));
        return attributes;
    }

    /**
     * Gets all fields from a class including inherited fields from parent classes
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        // Traverse up the inheritance hierarchy
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            // Add fields from current class
            Collections.addAll(fields, currentClass.getDeclaredFields());

            // Move to parent class
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Determines if a field is required based on JPA annotations
     */
    private boolean isFieldRequired(Field field) {
        // Check for @NotBlank, @NotNull, or @NotEmpty annotations
        if (field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotNull.class) ||
                field.isAnnotationPresent(jakarta.validation.constraints.NotEmpty.class)) {
            return true;
        }

        // Check for JPA @Column(nullable = false)
        if (field.isAnnotationPresent(jakarta.persistence.Column.class)) {
            jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);
            if (!column.nullable()) {
                return true;
            }
        }

        // Primitive types are typically required (can't be null)
        if (field.getType().isPrimitive()) {
            return true;
        }

        return false;
    }

    /**
     * Determines if a field should be hidden from the schema
     */
    private boolean isHiddenField(String fieldName) {
        return switch (fieldName) {
            case "id", "dgraphNodeId", "passwordHash", "tenantId", "createdAt", "updatedAt" -> true;
            default -> false;
        };
    }

    private Class<?> getEntityClassForIdentityType(IdentityType identityType) {
        return switch (identityType) {
            case EMPLOYEE -> Employee.class;
            case NON_EMPLOYEE -> NonEmployee.class;
            case NON_HUMAN -> NonHumanIdentity.class;
        };
    }

    private CoreAttributeMetadata createAttributeMetadata(Field field) {
        String fieldName = field.getName();

        // Check if it's a JPA relationship
        boolean isRelationship = field.isAnnotationPresent(ManyToOne.class) ||
                field.isAnnotationPresent(OneToMany.class) ||
                field.isAnnotationPresent(ManyToMany.class) ||
                field.isAnnotationPresent(OneToOne.class);

        String relationshipType = null;
        if (isRelationship) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                relationshipType = "ManyToOne";
            } else if (field.isAnnotationPresent(OneToMany.class)) {
                relationshipType = "OneToMany";
            } else if (field.isAnnotationPresent(ManyToMany.class)) {
                relationshipType = "ManyToMany";
            } else if (field.isAnnotationPresent(OneToOne.class)) {
                relationshipType = "OneToOne";
            }
        }

        DataType dataType = inferDataType(field);
        if (dataType == null) {
            // Skip fields we can't map to our data types
            return null;
        }

        // Check if field is required based on annotations
        boolean isRequired = isFieldRequired(field);
        String displayName = convertFieldNameToDisplayName(fieldName);
        String description = generateFieldDescription(field, isRelationship);

        return new CoreAttributeMetadata(
                fieldName,
                displayName,
                dataType,
                isRequired,
                isRelationship,
                relationshipType,
                description);
    }

    private DataType inferDataType(Field field) {
        Class<?> fieldType = field.getType();

        // Handle collections (arrays/lists)
        if (Collection.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
            return DataType.ARRAY;
        }

        // Handle primitive types and their wrappers
        if (fieldType == String.class || fieldType == Character.class || fieldType == char.class) {
            return DataType.STRING;
        } else if (fieldType == Integer.class || fieldType == int.class ||
                fieldType == Long.class || fieldType == long.class ||
                fieldType == Double.class || fieldType == double.class ||
                fieldType == Float.class || fieldType == float.class ||
                fieldType == BigDecimal.class) {
            return DataType.NUMBER;
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return DataType.BOOLEAN;
        } else if (fieldType == Instant.class || fieldType == LocalDate.class ||
                fieldType == Date.class || fieldType == java.sql.Date.class) {
            return DataType.DATE;
        } else if (fieldType == UUID.class) {
            return DataType.STRING; // UUIDs are typically treated as strings
        } else if (fieldType == Map.class || fieldType == Object.class) {
            return DataType.OBJECT;
        }

        // For custom objects (relationships), we'll handle them separately
        return null;
    }

    private String convertFieldNameToDisplayName(String fieldName) {
        // Convert camelCase to Title Case with spaces
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(" ").append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String generateFieldDescription(Field field, boolean isRelationship) {
        if (isRelationship) {
            return "Reference to another identity. Provide the ID of the related identity.";
        }

        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            return "Text field for storing string values.";
        } else if (Number.class.isAssignableFrom(fieldType) || fieldType.isPrimitive()) {
            return "Numeric field for storing numbers.";
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "Boolean field for true/false values.";
        } else if (fieldType == Instant.class || fieldType == LocalDate.class || fieldType == Date.class) {
            return "Date/time field.";
        } else if (fieldType == UUID.class) {
            return "Unique identifier field.";
        }

        return "Field for storing " + fieldType.getSimpleName() + " values.";
    }
}
