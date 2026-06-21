-- Add optional risk level to human identity subtype tables.
-- Nullable columns keep existing employee/non-employee rows valid without backfill.

ALTER TABLE IF EXISTS employees
    ADD COLUMN IF NOT EXISTS risk_level varchar(20);

ALTER TABLE IF EXISTS non_employees
    ADD COLUMN IF NOT EXISTS risk_level varchar(20);

