-- Convert entitlement assignment rule identity targeting from a single text column
-- to a JSONB array so rules can target multiple identity types.
--
-- Notes:
--   * Existing singular identity_type data is backfilled into identity_types.
--   * Existing JSONB arrays are preserved.
--   * Legacy identity_type columns are intentionally retained for a safer rollout.

ALTER TABLE IF EXISTS entitlement_assignment_rules
    ADD COLUMN IF NOT EXISTS identity_types jsonb;

ALTER TABLE IF EXISTS entitlement_assignment_rules
    ALTER COLUMN identity_types SET DEFAULT '["EMPLOYEE"]'::jsonb;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'entitlement_assignment_rules'
          AND column_name = 'identity_type'
    ) THEN
        EXECUTE $sql$
            UPDATE entitlement_assignment_rules
            SET identity_types = CASE
                WHEN identity_types IS NOT NULL
                     AND jsonb_typeof(identity_types) = 'array'
                     AND jsonb_array_length(identity_types) > 0
                    THEN identity_types
                WHEN identity_type IS NOT NULL AND btrim(identity_type) <> ''
                    THEN jsonb_build_array(upper(btrim(identity_type)))
                ELSE '["EMPLOYEE"]'::jsonb
            END
        $sql$;
    ELSE
        UPDATE entitlement_assignment_rules
        SET identity_types = '["EMPLOYEE"]'::jsonb
        WHERE identity_types IS NULL
           OR jsonb_typeof(identity_types) <> 'array'
           OR jsonb_array_length(identity_types) = 0;
    END IF;
END $$;

ALTER TABLE IF EXISTS rule_execution_history
    ADD COLUMN IF NOT EXISTS identity_types jsonb;

ALTER TABLE IF EXISTS rule_execution_history
    ALTER COLUMN identity_types SET DEFAULT '["EMPLOYEE"]'::jsonb;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rule_execution_history'
          AND column_name = 'identity_type'
    ) THEN
        EXECUTE $sql$
            UPDATE rule_execution_history
            SET identity_types = CASE
                WHEN identity_types IS NOT NULL
                     AND jsonb_typeof(identity_types) = 'array'
                     AND jsonb_array_length(identity_types) > 0
                    THEN identity_types
                WHEN identity_type IS NOT NULL AND btrim(identity_type) <> ''
                    THEN jsonb_build_array(upper(btrim(identity_type)))
                ELSE '["EMPLOYEE"]'::jsonb
            END
        $sql$;
    ELSE
        UPDATE rule_execution_history
        SET identity_types = '["EMPLOYEE"]'::jsonb
        WHERE identity_types IS NULL
           OR jsonb_typeof(identity_types) <> 'array'
           OR jsonb_array_length(identity_types) = 0;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_entitlement_assignment_rules_identity_types_array'
    ) THEN
        ALTER TABLE entitlement_assignment_rules
            ADD CONSTRAINT chk_entitlement_assignment_rules_identity_types_array
            CHECK (identity_types IS NULL OR jsonb_typeof(identity_types) = 'array');
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_rule_execution_history_identity_types_array'
    ) THEN
        ALTER TABLE rule_execution_history
            ADD CONSTRAINT chk_rule_execution_history_identity_types_array
            CHECK (identity_types IS NULL OR jsonb_typeof(identity_types) = 'array');
    END IF;
END $$;

