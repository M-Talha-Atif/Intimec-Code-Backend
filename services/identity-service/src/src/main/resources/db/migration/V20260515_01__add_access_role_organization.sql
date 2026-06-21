-- Adds the mandatory singular organization reference for access roles.
-- API field: organization (UUID)
-- DB column: access_roles.organization_id -> org_units.id

ALTER TABLE access_roles
	ADD COLUMN IF NOT EXISTS organization_id UUID;

-- Backfill existing rows to a deterministic active organization in the same tenant.
-- Preference is tenant root org units first, then the shallowest active org unit.
WITH tenant_default_org AS (
	SELECT DISTINCT ON (tenant_id)
		   tenant_id,
		   id AS organization_id
	  FROM org_units
	 WHERE status = 'ACTIVE'
	 ORDER BY tenant_id,
			  CASE WHEN parent_id IS NULL THEN 0 ELSE 1 END,
			  depth ASC,
			  name ASC,
			  id ASC
)
UPDATE access_roles ar
   SET organization_id = tdo.organization_id
  FROM tenant_default_org tdo
 WHERE ar.tenant_id = tdo.tenant_id
   AND ar.organization_id IS NULL;

DO $$
BEGIN
	IF EXISTS (SELECT 1 FROM access_roles WHERE organization_id IS NULL) THEN
		RAISE EXCEPTION 'Cannot make access_roles.organization_id mandatory: some access roles have no active org_units row for their tenant. Create/backfill org_units first, then rerun this migration.';
	END IF;
END $$;

ALTER TABLE access_roles
	ALTER COLUMN organization_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_access_roles_tenant_organization
	ON access_roles (tenant_id, organization_id);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		  FROM pg_constraint
		 WHERE conname = 'fk_access_roles_organization'
	) THEN
		ALTER TABLE access_roles
			ADD CONSTRAINT fk_access_roles_organization
			FOREIGN KEY (organization_id)
			REFERENCES org_units (id);
	END IF;
END $$;

