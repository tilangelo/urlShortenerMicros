CREATE TABLE urls (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0
);

-- Индексы
CREATE INDEX idx_short_code ON urls(short_code);
CREATE UNIQUE INDEX idx_short_code_unique ON urls(short_code);
CREATE INDEX idx_expires_at ON urls(expires_at);

-- Триггер для обновления click_count
CREATE OR REPLACE FUNCTION increment_click_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE urls SET click_count = click_count + 1 WHERE id = NEW.id;
    RETURN NEW;
END;
$$ language 'plpgsql';
