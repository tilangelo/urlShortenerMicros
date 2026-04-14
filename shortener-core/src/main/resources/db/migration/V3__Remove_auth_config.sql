-- Удаляем колонку auth_config, т.к. авторизация теперь обрабатывается в API Gateway
ALTER TABLE link_policies DROP COLUMN IF EXISTS auth_config;

-- Обновляем CHECK constraint для auth_type (убираем 'jwt', добавляем возможные новые типы)
ALTER TABLE link_policies DROP CONSTRAINT IF EXISTS chk_auth_type;
ALTER TABLE link_policies ADD CONSTRAINT chk_auth_type
    CHECK (auth_type IS NULL OR auth_type IN ('none', 'corporate_sso', 'api_key', 'basic'));
