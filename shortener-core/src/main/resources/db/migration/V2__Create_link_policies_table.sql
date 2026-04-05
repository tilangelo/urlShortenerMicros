CREATE TABLE link_policies (
    id BIGINT PRIMARY KEY,
    link_id BIGINT NOT NULL,
    shortcode VARCHAR(10) NOT NULL,
    allowed_ips JSONB,
    allowed_time_start TIMESTAMP WITH TIME ZONE,
    allowed_time_end TIMESTAMP WITH TIME ZONE,
    auth_type VARCHAR(50),
    auth_config JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_link_policies_link_id 
        FOREIGN KEY (link_id) 
        REFERENCES urls(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT uk_link_policies_shortcode 
        UNIQUE (shortcode),
    
    CONSTRAINT chk_auth_type 
        CHECK (auth_type IS NULL OR auth_type IN ('corporate_sso', 'api_key', 'jwt', 'basic'))
);

-- Индексы
CREATE INDEX idx_link_policies_shortcode ON link_policies(shortcode);
CREATE INDEX idx_link_policies_link_id ON link_policies(link_id);
CREATE INDEX idx_link_policies_time_window ON link_policies(allowed_time_start, allowed_time_end);

-- Триггер для обновления updated_at
CREATE OR REPLACE FUNCTION update_link_policies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_link_policies_updated_at
    BEFORE UPDATE ON link_policies
    FOR EACH ROW
    EXECUTE FUNCTION update_link_policies_updated_at();
