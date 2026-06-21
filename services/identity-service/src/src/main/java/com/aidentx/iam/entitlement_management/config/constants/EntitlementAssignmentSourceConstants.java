package com.aidentx.iam.entitlement_management.config.constants;

/**
 * Source markers for entitlement assignments.
 *
 * <p>Canonical values are intentionally small:
 * <ul>
 *   <li>{@code DIRECT}: assigned outside entitlement rules, including UI/manual and file/import flows</li>
 *   <li>{@code DYNAMIC}: assigned by entitlement rules and controlled by rule reconciliation</li>
 *   <li>{@code ROLE_DERIVED}: assigned because an identity holds an access role</li>
 * </ul>
 */
public final class EntitlementAssignmentSourceConstants {

    private EntitlementAssignmentSourceConstants() {
    }

    public static final String SOURCE_DIRECT = "DIRECT";
    public static final String SOURCE_DYNAMIC = "DYNAMIC";
    public static final String SOURCE_ROLE_DERIVED = "ROLE_DERIVED";

    /** Legacy value kept only for backward-compatible reads/migrations. */
    public static final String LEGACY_SOURCE_IMPORT = "IMPORT";

    /** Legacy value kept only for backward-compatible reads/migrations. */
    public static final String LEGACY_SOURCE_MANUAL = "MANUAL";
}


