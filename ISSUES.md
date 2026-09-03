# URL Shortener — backlog

Этот файл хранит результаты технического аудита. Issues расположены в рекомендуемом порядке исправления.

Статусы:

- `[ ]` — не начато
- `[~]` — в работе
- `[x]` — завершено и проверено

## P0 — работоспособность и безопасность

- [x] Разделить local/debug и Docker-конфигурацию. `localhost` оставить для локальной отладки; в Docker-профилях использовать имена сервисов PostgreSQL, Redis, core и redirect.
- [x] Исправить gateway route `/internal/**`: текущий `StripPrefix=1` удаляет сегмент `/internal`, хотя core ожидает `/internal/links/**`.
- [x] Закрыть обход политик через публичный `/redirect-api/{shortcode}`.
- [ ] Не публиковать наружу порты core, redirect, PostgreSQL и Redis в production-подобном Compose; внешний вход должен идти через gateway.
- [x] Для прямого подключения к gateway не доверять клиентским `X-Forwarded-For` и `X-Real-IP`; использовать IP непосредственного TCP-соединения (`remoteAddress`).
- [ ] После появления reverse proxy обновить определение client IP: завести конфигурацию trusted proxy/CIDR, читать forwarded-заголовки только когда непосредственный peer доверенный, разбирать цепочку `X-Forwarded-For` справа налево и добавить тесты против header spoofing. Edge proxy должен удалять или перезаписывать forwarded-заголовки от внешнего клиента.
- [x] Определить fail-open/fail-closed поведение политик. Ошибка Redis/core или десериализации не должна автоматически означать «политики нет».
- [x] Реализовать настоящую BASIC-аутентификацию либо временно запретить этот `authType`. Сейчас `BASIC` явно запрещён в core и gateway; полноценная реализация отложена.
- [x] Защитить management API, создание/удаление политик и internal endpoints аутентификацией/авторизацией. Временный учебный вариант: stateless HTTP Basic, отдельные роли `MANAGER` и `SERVICE_GATEWAY`, изолированный `coreWebClient` с service credentials; production roadmap — корпоративный IdP/OIDC/JWT.

## P1 — корректность бизнес-логики

- [x] Определить стратегию согласованности PostgreSQL и Redis при создании ссылки: PostgreSQL является источником истины, а Redis заполняется через локальное событие `AFTER_COMMIT`; ошибка кэша не откатывает успешно созданную ссылку.
- [x] Записывать в Redis нормализованный `shortUrl.getLongUrl()`, а не исходный пользовательский `longUrl`.
- [x] Устанавливать TTL Redis-ключа ссылки на основании `expiresAt`.
- [ ] Определить поведение redirect при потере/eviction Redis: fallback, восстановление кэша или явно принятая потеря доступности.
- [x] Добавить negative caching для ссылок без политики, чтобы gateway не обращался в core на каждый redirect.
- [x] Возвращать при создании ссылки как минимум `id`, `shortcode`, `shortUrl`, `createdAt` и `expiresAt`; оба create endpoint возвращают единый JSON contract, `201 Created` и `Location`.
- [ ] Проверять, что переданные `linkId` и `shortcode` принадлежат одной ссылке.
- [ ] Валидировать временное окно (`start <= end`), `authType` и верхнюю границу TTL при создании политики/ссылки.
- [ ] Зафиксировать контракт IP policy. Для первой версии рекомендуется явно поддерживать только IPv4: exact, CIDR, range и wildcard.
- [ ] Валидировать `allowedIps` в core до записи в PostgreSQL/Redis: запретить `null`, пустые строки, hostname и неподдерживаемые форматы; ограничить количество правил и длину каждого элемента.
- [ ] Использовать строгий числовой IPv4 parser без DNS. Не применять `InetAddress.getByName()` для проверки IP на Reactor event loop.
- [ ] Канонизировать IP rules до сохранения и не сравнивать IP только по исходному строковому представлению.
- [ ] Для CIDR проверять четыре IPv4-октета и prefix `0..32`; явно определить семантику `0.0.0.0/0` для IPv6 и отсутствующего client IP.
- [ ] Для range требовать `start <= end`, а обратные и некорректные диапазоны отклонять вместо автоматического исправления через `Math.min/Math.max`.
- [ ] Для wildcard разрешать ровно четыре сегмента, где каждый сегмент — `*` или число `0..255`; валидировать как client IP, так и pattern.
- [ ] Явно обрабатывать отсутствие `remoteAddress`/`getAddress()` и запрещать доступ fail-closed, если policy содержит IP-ограничение; не передавать строку `unknown` как IP.
- [ ] Удалить закомментированную старую реализацию `X-Forwarded-For` после фиксации истории в Git.
- [ ] Добавить HTTP endpoint обновления политики либо удалить неиспользуемый `updatePolicy` из публичного use case до реализации.
- [ ] Реализовать корректный `click_count`; обновлять статистику асинхронно или пакетно.
- [ ] Включить scheduling для очистки либо заменить её явным job; при удалении синхронно очищать Redis и связанные policy keys.
- [ ] Пересмотреть длину `short_code`: Base62 для положительного `long` потенциально требует 11 символов, а схема допускает 10.
- [ ] Добавить единый error contract и глобальные exception handlers для core, redirect и gateway.

## P2 — данные, инфраструктура и эксплуатация

- [ ] Использовать Flyway как единственный владелец схемы и заменить `ddl-auto: update` на `validate`.
- [ ] Удалить дублирующие индексы `short_code` и неиспользуемую SQL-функцию `increment_click_count` либо корректно оформить механизм статистики.
- [ ] Выбрать один способ маппинга `allowed_ips` JSONB: Hibernate JSON type или `AttributeConverter`.
- [ ] Вынести credentials и environment-specific настройки из основного `application.yml`; добавить безопасный `.env.example`.
- [ ] Унифицировать или осознанно зафиксировать версии Spring Boot/Jackson между сервисами.
- [ ] Добавить healthchecks для Redis, core, redirect и gateway; `depends_on` не должен означать готовность сервиса принимать запросы.
- [ ] Заполнить конфигурацию Prometheus и добавить dashboards/alerts для основных SLI.
- [ ] Не показывать подробности health/metrics неавторизованным внешним клиентам.
- [ ] Добавить rate limiting, который заявлялся в README, но пока отсутствует.
- [ ] Не логировать полные URL с query-параметрами и части auth tokens; в них могут быть секреты.

## P3 — производительность и качество кода

- [ ] Убрать лишний Redis `hasKey` перед чтением данных и сократить число round trips на redirect hot path.
- [x] Переиспользовать настроенный `WebClient`; не создавать и не переконфигурировать его на каждый fallback.
- [x] Уменьшить `maxInMemorySize` для маленького ответа политики с текущих 16 MB.
- [ ] Не выполнять потенциальный DNS lookup через `InetAddress.getByName()` на Reactor event loop (см. контракт и валидацию IP policy в P1).
- [ ] Инжектировать `MeterRegistry` вместо глобального `Metrics` и уточнить семантику счётчиков.
- [ ] Исправить измерение времени в core `RequestLoggingFilter`: сейчас лог выполняется до `filterChain.doFilter`.
- [ ] Удалить устаревшие DTO, imports, deprecated `WebClientConfig` и отладочные `System.out.println`.
- [ ] Привести package naming к Java conventions (`adapter`, единый base package).

## Тесты

- [ ] Добавить unit-тесты `LongUrl`, `ShortCode`, `Base62Encoder`, `SnowflakeIdGenerator`, TTL и доменных инвариантов policy.
- [ ] Добавить unit-тесты сервисов с mock-портами, включая частичные отказы PostgreSQL/Redis.
- [ ] Разделить unit-тесты gateway и `@SpringBootTest`; IP/CIDR проверки не должны поднимать весь Spring context.
- [ ] Добавить controller/filter tests через MockMvc и WebTestClient.
- [ ] Добавить Testcontainers для PostgreSQL, Redis и проверки Flyway migrations.
- [ ] Добавить end-to-end сценарий: create link → create policy → redirect → expire/delete.
- [ ] Добавить security/regression tests: `/redirect-api` bypass, forged proxy headers, фиктивный Basic header, повреждённая policy в Redis.
- [ ] Расширить IP policy tests: invalid exact/wildcard, `null` внутри списка, whitespace, hostname, reverse range, IPv6 при IPv4 policy, `0.0.0.0/0`, отсутствующий `remoteAddress` и round trip core → PostgreSQL/Redis → gateway.
- [ ] Добавить CI: compile, tests, formatting/static analysis, dependency и container image scanning.

## Документация

- [x] Актуализировать endpoints создания ссылки: `POST /core-api/shorten/public` и `POST /core-api/shorten/protected`.
- [x] Исправить удаление политики: path parameter — `shortcode`, а не `policyId`.
- [x] Добавить фактически существующие GET endpoints политик и уточнить единицы `ttl`.
- [x] Уточнить, что `localhost` используется для local/debug, а Docker-конфигурация является отдельной задачей.
- [x] Не заявлять rate limiting как реализованную возможность.
- [ ] Добавить OpenAPI/Swagger и проверять API contract в CI.

## Future roadmap — после закрытия основного backlog

Эти задачи не должны отвлекать от P0–P3. Их цель — последовательно превратить исправленный проект в полноценный учебный production-like стенд.

### Этап 1 — тестовая пирамида и API-контракты

- [ ] Сформировать быстрый unit-test слой на JUnit 5 + AssertJ + Mockito без запуска Spring context; договориться о структуре Arrange–Act–Assert и понятном именовании сценариев.
- [ ] Использовать Testcontainers для интеграционных тестов PostgreSQL, Redis и Flyway; тесты не должны зависеть от вручную запущенной локальной инфраструктуры.
- [ ] Добавить WireMock для HTTP-интеграций gateway/core/auth: проверить success, `404`, `409`, `429`, `5xx`, timeout, malformed JSON и обрыв соединения.
- [ ] Добавить contract tests между gateway, core и redirect, чтобы несовместимое изменение DTO или path обнаруживалось в CI.
- [ ] Подключить springdoc OpenAPI, описать status codes и схемы ошибок; сравнивать сгенерированный API contract в CI.
- [ ] Добавить JaCoCo как измеритель непокрытых рисков, не превращая процент покрытия в самоцель.

### Этап 2 — безопасность

- [ ] Подключить Spring Security и определить threat model: публичные redirect endpoints, management API, policy management и internal service-to-service endpoints.
- [ ] Реализовать JWT/OAuth2 Resource Server для управляющего API и роли/authorities для создания, изменения и удаления ссылок и политик.
- [ ] Определить service-to-service authentication для internal API; не полагаться только на сетевую изоляцию.
- [ ] Добавить security tests для отсутствующего, просроченного, повреждённого JWT, неверной роли, обхода маршрутов и утечки чувствительных данных.
- [ ] Добавить dependency и container scanning (например, OWASP Dependency-Check/Trivy), секрет-сканирование и генерацию SBOM в CI.

### Этап 3 — наблюдаемость и устойчивость

- [ ] Спроектировать RED-метрики через Micrometer: request rate, error rate и duration по сервисам; отдельно cache hit/miss, policy denied и redirect result.
- [ ] Настроить Prometheus + Grafana dashboards и базовые alerts; проверить метрики под небольшой контролируемой нагрузкой.
- [ ] Подключить OpenTelemetry distributed tracing и Jaeger/Tempo; передавать trace/correlation ID по цепочке gateway → core/redirect → Redis/PostgreSQL.
- [ ] Ввести структурированные JSON-логи, корреляцию запросов и redaction URL query, Authorization, API keys и персональных данных.
- [ ] Углубить Resilience4j: явные timeout, circuit breaker, retry и bulkhead; зафиксировать идемпотентность и не складывать несколько независимых retry-слоёв.
- [ ] Провести failure tests: недоступный Redis/core/auth, медленный downstream, открытый circuit breaker, исчерпание connection pool и восстановление сервиса.

### Этап 4 — HTTP-клиенты и межсервисные контракты

- [ ] Перейти от ручной сборки URL к Spring HTTP Service interfaces: WebClient adapter в реактивном gateway и RestClient adapter в блокирующих MVC-сервисах.
- [ ] Переиспользовать один настроенный HTTP client на downstream: base URL, connection/read timeout, ограничение body, status handlers, metrics и tracing.
- [ ] Сделать отдельный учебный spike с Spring Cloud OpenFeign в блокирующем сервисе и сравнить его с HTTP Interface + RestClient по timeout, error mapping, тестируемости и объёму конфигурации; не добавлять Feign без реального outbound use case.
- [ ] При добавлении Spring Cloud использовать release train, совместимый с версией Spring Boot каждого сервиса, и управлять версиями через BOM.

### Этап 5 — CI/CD и оркестрация

- [ ] Настроить CI pipeline: compile, unit/integration/contract tests, JaCoCo, Spotless/Checkstyle, SpotBugs, dependency scan и сборка контейнеров.
- [ ] Привести Dockerfiles к воспроизводимой multi-stage сборке, запускать приложения не от root, добавить healthcheck, graceful shutdown и ограничение ресурсов.
- [ ] Добавить отдельные local, test и production-like Compose-конфигурации; наружу публиковать только необходимые порты.
- [ ] Изучать Kubernetes только после готовности приложения: stateless instances, externalized config/secrets, readiness/liveness probes, graceful shutdown, metrics и resource requests/limits.
- [ ] В Kubernetes-этапе освоить Deployment, Service, ConfigMap, Secret, Ingress/Gateway API, HPA и rolling update; service discovery и config server добавлять только при подтверждённой необходимости.

### Этап 6 — асинхронность, transactional outbox и broker (последний этап)

- [ ] Сначала реализовать и измерить корректный синхронный учёт кликов, затем определить требования к допустимой потере, задержке и порядку событий.
- [ ] Изучить и реализовать transactional outbox: сохранять бизнес-изменение и outbox-событие в одной PostgreSQL-транзакции, а публикацию выполнять с retry после commit.
- [ ] Реализовать учебный поток click events через осознанно выбранный broker (в первую очередь рассмотреть Kafka; сравнить с RabbitMQ и Redis Streams по требованиям, а не популярности).
- [ ] Сделать consumer идемпотентным; определить retry policy, dead-letter queue/topic, replay и обработку дубликатов.
- [ ] Добавить contract/integration/load tests для outbox publisher, producer, broker и consumer, включая недоступность broker и повторную доставку.
