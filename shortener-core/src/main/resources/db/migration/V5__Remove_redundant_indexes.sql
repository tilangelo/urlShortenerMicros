-- UNIQUE constraint urls_short_code_key уже предоставляет индекс.
DROP INDEX IF EXISTS idx_short_code;
DROP INDEX IF EXISTS idx_short_code_unique;

-- UNIQUE constraint uk_link_policies_shortcode уже предоставляет индекс.
DROP INDEX IF EXISTS idx_link_policies_shortcode;

DROP INDEX IF EXISTS idx_link_policies_time_window;