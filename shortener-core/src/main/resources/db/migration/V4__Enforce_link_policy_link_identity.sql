-- требует уникальность всего набора колонок,
-- на который ссылается составной foreign key.
ALTER TABLE urls
    ADD CONSTRAINT uk_urls_id_short_code
        UNIQUE (id, short_code);

-- У одной ссылки может быть только одна политика.
ALTER TABLE link_policies
    ADD CONSTRAINT uk_link_policies_link_id
        UNIQUE (link_id);

-- проверяет только существование link_id.
ALTER TABLE link_policies
    DROP CONSTRAINT fk_link_policies_link_id;

-- гарантирует, что link_id и shortcode принадлежат
-- одной и той же записи urls.
ALTER TABLE link_policies
    ADD CONSTRAINT fk_link_policies_link_pair
        FOREIGN KEY (link_id, shortcode)
            REFERENCES urls (id, short_code)
            ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_link_policies_link_id;