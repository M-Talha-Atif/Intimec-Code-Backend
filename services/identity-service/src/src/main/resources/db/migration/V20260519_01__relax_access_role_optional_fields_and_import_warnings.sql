-- Relaxes access-role create/import invariants so only name is mandatory.
-- Missing optional categorical fields are backfilled to the UI-facing stored defaults.

ALTER TABLE access_roles
    ADD COLUMN IF NOT EXISTS business_unit VARCHAR(255);

UPDATE access_roles
   SET display_name = name
 WHERE display_name IS NULL
    OR btrim(display_name) = '';

UPDATE access_roles
   SET type = 'No type'
 WHERE type IS NULL
    OR btrim(type) = '';

UPDATE access_roles
   SET business_unit = 'No business unit'
 WHERE business_unit IS NULL
    OR btrim(business_unit) = '';

UPDATE access_roles
   SET status = 'Enabled'
 WHERE status IS NULL
    OR btrim(status) = '';

UPDATE access_roles
   SET requestable = FALSE
 WHERE requestable IS NULL;

UPDATE access_roles
   SET require_approval = FALSE
 WHERE require_approval IS NULL;

ALTER TABLE access_roles
    ALTER COLUMN type DROP NOT NULL,
    ALTER COLUMN business_unit DROP NOT NULL,
    ALTER COLUMN organization_id DROP NOT NULL;

ALTER TABLE role_import_job_items
    ADD COLUMN IF NOT EXISTS warning_message TEXT;
