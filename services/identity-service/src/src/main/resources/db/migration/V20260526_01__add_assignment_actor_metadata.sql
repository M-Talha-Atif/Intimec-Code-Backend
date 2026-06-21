-- Captures the actor that directly/manually assigned standalone entitlements and access roles.
-- Existing rows remain nullable because historical assignments did not store actor metadata.
ALTER TABLE entitlement_assignments
    ADD COLUMN IF NOT EXISTS assigned_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assigned_by_user_name VARCHAR(255);

ALTER TABLE identity_access_role_assignments
    ADD COLUMN IF NOT EXISTS assigned_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assigned_by_user_name VARCHAR(255);

