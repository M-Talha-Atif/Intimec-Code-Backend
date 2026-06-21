-- Stores the human-readable actor username captured at import submission time.
-- This preserves audit attribution for async import processing even when X-User-ID is an auth UUID.
ALTER TABLE role_import_jobs
    ADD COLUMN IF NOT EXISTS actor_username VARCHAR(255);

