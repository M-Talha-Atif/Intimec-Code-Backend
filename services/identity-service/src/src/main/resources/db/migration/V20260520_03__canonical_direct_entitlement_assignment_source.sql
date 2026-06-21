-- Canonicalize entitlement assignment source semantics.
-- Manual/UI and file/import assignments are DIRECT; only rule-created assignments remain DYNAMIC.

UPDATE entitlement_assignments
SET source = 'DIRECT'
WHERE source IN ('IMPORT', 'MANUAL');
