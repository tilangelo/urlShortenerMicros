# Исправление ошибки миграции базы данных

## Проблема
Ошибка в строке `REFERENCES urls(id)` - таблица `urls` не существует при выполнении миграции V2.

## Причина
1. Отсутствует миграция V1 для создания таблицы `urls`
2. Используется `ddl-auto: update` вместо Flyway для управления миграциями

## Решение

### 1. Заменить конфигурацию
Замените `application.yml` на `application-updated.yml`:
```bash
mv src/main/resources/application.yml src/main/resources/application-old.yml
mv src/main/resources/application-updated.yml src/main/resources/application.yml
```

### 2. Новая структура миграций
- **V1__Create_urls_table.sql** - создает основную таблицу `urls`
- **V2__Create_link_policies_table.sql** - создает таблицу `link_policies` с foreign key

### 3. Изменения в конфигурации
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate  # Вместо update
```

### 4. Зависимости Flyway
Добавлены в pom.xml:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## Порядок выполнения миграций
1. **V1**: Создает таблицу `urls` с индексами
2. **V2**: Создает таблицу `link_policies` с foreign key на `urls(id)`

## Запуск
```bash
# Очистить базу данных (если нужно)
docker-compose down -v

# Запустить с новыми миграциями
docker-compose up --build
```

Flyway автоматически выполнит V1, затем V2 в правильном порядке.
