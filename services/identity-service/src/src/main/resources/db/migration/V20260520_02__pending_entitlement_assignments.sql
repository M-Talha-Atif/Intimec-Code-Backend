-- Allow entitlement assignments to exist before an account shadow is available.
-- Such rows are identity-scoped and marked PENDING until an account exists.

ALTER TABLE IF EXISTS entitlement_assignments
    ADD COLUMN IF NOT EXISTS identity_id uuid;

ALTER TABLE IF EXISTS entitlement_assignments
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'ASSIGNED';

UPDATE entitlement_assignments ea
SET identity_id = s.linked_identity_id
FROM shadows s
WHERE ea.shadow_id = s.id
  AND ea.identity_id IS NULL
  AND s.linked_identity_id IS NOT NULL;

UPDATE entitlement_assignments
SET status = 'ASSIGNED'
WHERE status IS NULL OR btrim(status) = '';

ALTER TABLE IF EXISTS entitlement_assignments
    ALTER COLUMN shadow_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ea_identity_entitlement
    ON entitlement_assignments (identity_id, entitlement_id, tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ea_shadow_entitlement_tenant_assigned
    ON entitlement_assignments (shadow_id, entitlement_id, tenant_id)
    WHERE shadow_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_ea_pending_identity_entitlement_tenant
    ON entitlement_assignments (identity_id, entitlement_id, tenant_id)
    WHERE shadow_id IS NULL AND identity_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_entitlement_assignment_status'
    ) THEN
        ALTER TABLE entitlement_assignments
            ADD CONSTRAINT chk_entitlement_assignment_status
            CHECK (status IN ('ASSIGNED', 'PENDING'));
    END IF;
END $$;

