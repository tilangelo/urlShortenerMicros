# API Gateway с политиками доступа

## Исправленные ошибки

### 1. Jackson 3 и JsonMapper
- **Проблема**: Использовался устаревший ObjectMapper вместо JsonMapper
- **Решение**: Заменен на `JsonMapper` в core и API Gateway сервисах
- **Изменения**:
  - `LinkPolicyEntity`: `JsonMapper.builder().addModule(new JavaTimeModule()).build()`
  - `LinkPolicyRedisAdapter`: статический `JsonMapper` с Java Time Module
  - `GatewayConfig`: Bean `JsonMapper` вместо `ObjectMapper`

### 2. Redis конфигурация Spring Boot 3.x
- **Проблема**: Deprecated `spring.redis.*` свойства
- **Решение**: Заменены на `spring.data.redis.*`
- **Изменения**:
  - `application.yml`: `spring.data.redis.host` вместо `spring.redis.host`
  - `application-docker.yml`: аналогичные изменения

### 3. Spring Cloud Gateway
- **Проблема**: `GatewayFilter` вместо `GlobalFilter`
- **Решение**: Изменен на `GlobalFilter` для корректной работы
- **Проблема**: Несовместимость версий Spring Boot 3.2.0 и Spring Cloud
- **Решение**: Обновлен до Spring Cloud 2023.0.1

### 4. Domain модель
- **Проблема**: Приватный конструктор `ShortCode`
- **Решение**: Использован статический метод `ShortCode.of()`
- **Проблема**: Метод `generate()` вместо `nextId()` в `IdGenerator`
- **Решение**: Исправлен вызов метода

### 5. Зависимости Jackson
- **Проблема**: Неполные зависимости для Jackson 3
- **Решение**: Добавлены `jackson-core`, `jackson-databind`, `jackson-datatype-jsr310`

## Архитектура

### Core сервис (порт 8080)
- **База данных**: PostgreSQL с таблицей `link_policies`
- **Кеш**: Redis для политик доступа (JsonMapper serialization)
- **API endpoints**:
  - `POST /core-api/policies` - создать политику
  - `GET /core-api/policies/{shortcode}` - получить политику
  - `GET /internal/links/{shortcode}/policy` - внутренний API для Gateway

### API Gateway (порт 8082)
- **Фильтр**: `LinkPolicyFilter` - проверка политик (GlobalFilter)
- **Валидация**: IP, time window, authentication
- **Метрики**: Prometheus (`access_denied_total`, `clicks_total`, `redirect_success_total`)
- **Роутинг**: Проксирование в redirect-сервис (порт 8081)
- **JSON**: JsonMapper с JavaTimeModule для сериализации политик

### Redirect сервис (порт 8081)
- Реактивный сервис для редиректа по коротким ссылкам

## Запуск

### 1. Заменить docker-compose.yml
```bash
cp docker-compose-updated.yml docker-compose.yml
```

### 2. Собрать проекты
```bash
cd shortener-core && mvn clean package
cd ../api-gateway && mvn clean package
```

### 3. Запустить систему
```bash
docker-compose up --build
```

## Пример использования

### Создать политику
```bash
curl -X POST http://localhost:8080/core-api/policies \
  -H "Content-Type: application/json" \
  -d '{
    "linkId": 12345,
    "shortcode": "abc123",
    "allowedIps": ["10.0.0.0/8", "192.168.1.50"],
    "allowedTimeStart": "2026-04-01T00:00:00Z",
    "allowedTimeEnd": "2026-05-01T00:00:00Z",
    "authType": "corporate_sso",
    "authConfig": "{\"sso_endpoint\": \"https://sso.company.com/validate\"}"
  }'
```

### Тестировать редирект
```bash
curl -i http://localhost:8082/abc123
```

## Метрики
- http://localhost:8082/actuator/prometheus
- http://localhost:8082/actuator/health

## Логика работы
1. Client → Gateway (`/abc123`)
2. Gateway проверяет политику в Redis (`link:policy:abc123`)
3. Cache miss → запрос в Core API (`/internal/links/abc123/policy`)
4. Валидация: IP, время, аутентификация
5. Успех → проксирование в redirect-сервис
6. Redirect-сервис возвращает 302 на long URL
7. Gateway логирует метрики и возвращает ответ клиенту

## Технические улучшения
- ✅ **JsonMapper**: Современная Jackson 3 сериализация
- ✅ **Spring Boot 3.x**: Актуальная конфигурация Redis
- ✅ **GlobalFilter**: Правильная работа с Spring Cloud Gateway
- ✅ **Java Time Module**: Корректная работа с Instant полями
- ✅ **Метрики**: Полный мониторинг производительности
