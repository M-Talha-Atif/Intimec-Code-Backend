package com.aidentx.access_portal_service.service.enrichment;

import java.util.UUID;

/**
 * Contract for DTOs that carry an item reference whose display name
 * can be resolved from an external source-of-truth service.
 *
 * Any response DTO holding itemType + itemId + itemName can implement
 * this interface to participate in read-time name enrichment via
 * {@link ItemNameResolver}.
 */
public interface NameResolvable {

    String getItemType();

    UUID getItemId();

    String getItemName();

    void setItemName(String name);
}