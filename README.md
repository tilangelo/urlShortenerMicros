# URL Shortener Microservices

Pet project URL shortener на Java 25 и Spring Boot. Проект разделён на три приложения и использует PostgreSQL как источник истины, а Redis — как быстрое хранилище данных для redirect-path.

## Архитектура

- `api-gateway` (`8082`) — единственная публичная точка входа, маршрутизация и проверка link policy.
- `shortener-core` (`8080`) — блокирующий MVC-сервис: создание ссылок и политик, PostgreSQL, заполнение Redis.
- `shortener_redirect` (`8081`) — реактивный redirect-сервис, читающий ссылки из Redis.
- PostgreSQL — основное долговременное хранилище ссылок и политик.
- Redis — быстрый serving storage для redirect и кэш политик.

Публичный redirect проходит по цепочке:

```text
GET /{shortcode}
    -> API Gateway
    -> проверка существования ссылки и policy
    -> Redirect Service
    -> Redis
    -> 302 Location: <longUrl>
```

## Запуск и конфигурация

Значения `localhost` в обычных `application.yml` предназначены для локального запуска и отладки. Docker-профили используют имена Compose-сервисов.

Для запуска через Compose необходим `.env` как минимум с параметрами PostgreSQL и учётными данными Spring Security. Секреты не следует добавлять в Git.

Внешние запросы отправляются в API Gateway:

```text
http://localhost:8082
```

Management API временно защищён stateless HTTP Basic:

- роль `MANAGER` создаёт ссылки и управляет политиками;
- роль `SERVICE_GATEWAY` используется Gateway для `/internal/**` Core API.

## Создание публичной ссылки

```http
POST /core-api/shorten/public
Authorization: Basic <manager-credentials>
Content-Type: application/json
```

```json
{
  "longUrl": "https://example.com/docs",
  "expiration": "2030-01-01T12:00:00Z"
}
```

`expiration` — абсолютный момент окончания жизни ссылки в UTC, а не длительность в миллисекундах. Он обязателен и должен находиться в будущем.

Пример ответа:

```json
{
  "shortUrl": "http://localhost:8082/Ab3xYz"
}
```

Публичная ссылка не имеет policy. Если Gateway не находит policy, доступ разрешается.

## Создание защищённой ссылки

```http
POST /core-api/shorten/protected
Authorization: Basic <manager-credentials>
Content-Type: application/json
```

```json
{
  "longUrl": "https://example.com/internal/report",
  "allowedIps": ["127.0.0.1"],
  "allowedTimeStart": "2030-01-01T08:00:00Z",
  "allowedTimeEnd": "2030-01-01T12:00:00Z",
  "authType": "NONE"
}
```

Правила времени:

- `allowedTimeEnd` обязателен и должен находиться в будущем;
- `allowedTimeStart` необязателен;
- если начало указано, должно выполняться `allowedTimeStart <= allowedTimeEnd`;
- `allowedTimeEnd` одновременно является `expiresAt` самой ссылки;
- Redis TTL ссылки и policy вычисляется относительно одного абсолютного `allowedTimeEnd` непосредственно перед записью.

Ссылка и policy создаются внутри одной PostgreSQL-транзакции. Если создание policy завершается ошибкой, создание ссылки также откатывается. Redis заполняется только после успешного commit через `AFTER_COMMIT` events.

Пример ответа:

```json
{
  "fullUrl": "http://localhost:8082/Ab3xYz",
  "expiresAt": "2030-01-01T12:00:00Z"
}
```

Значения `authType`:

- `NONE` — без проверки credentials, но IP/time policy продолжает действовать;
- `API_KEY` — проверка API key через настроенный auth endpoint;
- `CORPORATE_SSO` — проверка Bearer token через настроенный auth endpoint;
- `BASIC` — временно запрещён и возвращает ошибку.

## Управление политиками

Создать policy для существующей ссылки:

```http
POST /core-api/policies
Authorization: Basic <manager-credentials>
Content-Type: application/json
```

```json
{
  "linkId": 299826759354814464,
  "shortcode": "Ab3xYz",
  "allowedIps": ["127.0.0.1"],
  "allowedTimeStart": "2030-01-01T08:00:00Z",
  "allowedTimeEnd": "2030-01-01T12:00:00Z",
  "authType": "NONE"
}
```

Дополнительные endpoints:

```text
GET    /core-api/policies/{shortcode}
GET    /core-api/policies/{shortcode}/exists
DELETE /core-api/policies/{shortcode}
```

Создание, обновление и удаление policy синхронизируют Redis после успешного commit PostgreSQL. Событие удаления содержит только `shortcode`, поскольку именно он нужен listener для удаления Redis-ключа.

## Internal API

Internal endpoints предназначены только для Gateway и требуют роли `SERVICE_GATEWAY`:

```text
GET /internal/links/{shortcode}/policy
GET /internal/links/{shortcode}/policy-exists
```

Gateway использует отдельный `coreWebClient` с service credentials. Пользовательские credentials к этим внутренним запросам отношения не имеют.

## Redirect и policy validation

При `GET /{shortcode}` Gateway:

1. проверяет наличие shortcode в Redis;
2. получает policy из Redis, а при cache miss обращается во внутренний Core endpoint;
3. проверяет временное окно;
4. проверяет IP whitelist;
5. проверяет выбранный тип аутентификации;
6. проксирует запрос в Redirect Service.

Основные ответы:

- `302 Found` — redirect разрешён;
- `401 Unauthorized` — отсутствуют или неверны пользовательские credentials;
- `403 Forbidden` — IP или временное окно не разрешают доступ;
- `404 Not Found` — ссылка отсутствует;
- `410 Gone` — Redirect Service прочитал уже истёкшую запись;
- `503 Service Unavailable` — недоступен обязательный внутренний сервис или Redis;
- `504 Gateway Timeout` — downstream не ответил за установленное время.

## Согласованность PostgreSQL и Redis

PostgreSQL-транзакция не включает Redis. Поэтому Core публикует локальные Spring events внутри транзакции, а `@TransactionalEventListener(AFTER_COMMIT)` изменяет Redis только после успешного commit.

Это предотвращает появление Redis-записи после rollback PostgreSQL, но локальное событие не является гарантированной очередью. Если приложение аварийно завершится между commit и listener, Redis может не получить запись. В backlog запланирован fallback ссылки через Core/PostgreSQL с восстановлением Redis; до его реализации Redis остаётся обязательным для redirect-path.

## Наблюдаемость и ограничения

- Actuator/Prometheus endpoints подключены в Gateway.
- Circuit breaker и timeout применяются при проверке policy.
- Rate limiting пока не реализован.
- Полноценный OAuth2/JWT и корпоративный Identity Provider оставлены в roadmap.
- Production-подобная сетевая изоляция контейнеров и trusted reverse proxy ещё находятся в backlog.

Полный технический backlog и порядок работ находятся в [ISSUES.md](ISSUES.md).
