-- Allow one DIRECT and one DYNAMIC entitlement assignment for the same identity/account + entitlement.
-- Same-source duplicates remain blocked.

CREATE UNIQUE INDEX IF NOT EXISTS ux_ea_shadow_entitlement_tenant_source_assigned
    ON entitlement_assignments (shadow_id, entitlement_id, tenant_id, source)
    WHERE shadow_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_ea_pending_identity_entitlement_tenant_source
    ON entitlement_assignments (identity_id, entitlement_id, tenant_id, source)
    WHERE shadow_id IS NULL AND identity_id IS NOT NULL;

ALTER TABLE IF EXISTS entitlement_assignments DROP CONSTRAINT IF EXISTS idx_ea_shadow_entitlement;
ALTER TABLE IF EXISTS entitlement_assignments DROP CONSTRAINT IF EXISTS ux_ea_shadow_entitlement_tenant_assigned;
ALTER TABLE IF EXISTS entitlement_assignments DROP CONSTRAINT IF EXISTS ux_ea_pending_identity_entitlement_tenant;

DROP INDEX IF EXISTS idx_ea_shadow_entitlement;
DROP INDEX IF EXISTS ux_ea_shadow_entitlement_tenant_assigned;
DROP INDEX IF EXISTS ux_ea_pending_identity_entitlement_tenant;

CREATE INDEX IF NOT EXISTS idx_ea_shadow_entitlement
    ON entitlement_assignments (shadow_id, entitlement_id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_ea_shadow_entitlement_source
    ON entitlement_assignments (shadow_id, entitlement_id, tenant_id, source);
