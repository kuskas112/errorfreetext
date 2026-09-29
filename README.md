# Error Free Text

REST-приложение для автоматической корректировки опечаток в тексте. Принимает текст с указанием языка, асинхронно обрабатывает его через API Яндекс.Спеллера и отдаёт исправленный результат по идентификатору задачи.

## Стек

- Java 21
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Gradle
- Docker / Docker Compose

## Запуск

### Через Docker Compose

Самый простой способ — поднимается и приложение, и база:

```bash
docker compose up --build
```

После старта приложение доступно на `http://localhost:8080`.

### Локально

1. Нужно поднять PostgreSQL (можно через `docker compose up -d db`).
2. Необходимо убедиться, что параметры подключения в `application.properties` совпадают с используемой БД.
3. Запустить приложение:

```bash
./gradlew bootRun
```

## API

Базовый URL: `http://localhost:8080`

### POST /tasks — создать задачу

Принимает текст и язык, сохраняет задачу со статусом `NEW`, возвращает её идентификатор.

**Тело запроса:**

```json
{
  "text": "Привкт менч зовут Иван",
  "language": "RU"
}
```

| Поле | Тип | Обязательное | Ограничения |
|---|---|---|---|
| `text` | string | да | минимум 3 символа, хотя бы одна буква, не только пробелы |
| `language` | string | да | `EN` или `RU` |

**Ответ `201 Created`:**

```json
{
  "id": "44bd78dc-d08c-41c6-b87d-fb82046bd470"
}
```

**Ошибки:**

| Код | `errorCode` | Причина |
|---|---|---|
| 400 | 40001 | Невалидное поле (короткий текст, нет букв, пустой язык) |
| 400 | 40002 | Некорректный JSON или неизвестное значение `language` |

Пример ответа `400`:

```json
{
  "errorMessage": "text: Text must contain at least one letter",
  "errorCode": 40001,
  "timestamp": "2025-01-01T12:00:00.000",
  "path": "/tasks"
}
```

### GET /tasks/{id} — получить результат

Возвращает статус задачи и, в зависимости от статуса, исправленный текст или сообщение об ошибке.

**Ответ `200 OK` для `NEW` / `IN_PROGRESS`:**

```json
{
  "status": "NEW"
}
```

**Ответ `200 OK` для `DONE`:**

```json
{
  "status": "DONE",
  "correctedText": "Привет меня зовут Иван"
}
```

**Ответ `200 OK` для `ERROR`:**

```json
{
  "status": "ERROR",
  "errorMessage": "Yandex Speller call failed"
}
```

**Ответ `404 Not Found`:**

```json
{
  "errorMessage": "Task with id: 44bd78dc-d08c-41c6-b87d-fb82046bd470 not found",
  "errorCode": 40401,
  "timestamp": "2025-01-01T12:00:00.000",
  "path": "/tasks/44bd78dc-d08c-41c6-b87d-fb82046bd470"
}
```

### Статусы задачи

| Статус | Значение |
|---|---|
| `NEW` | Задача создана, ждёт обработки |
| `IN_PROGRESS` | Scheduler забрал задачу в работу |
| `DONE` | Текст исправлен, результат в `correctedText` |
| `ERROR` | Обработка упала, причина в `errorMessage` |

## Примеры вызовов

### curl (Linux, macOS, Git Bash)

Создание задачи:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"text":"Привкт менч зовут Иван","language":"RU"}'
```

Получение результата (подставить свой `id`):

```bash
curl http://localhost:8080/tasks/44bd78dc-d08c-41c6-b87d-fb82046bd470
```

Проверка валидации — текст из двух символов:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"text":"ab","language":"RU"}'
```

Проверка валидации — только цифры и спецсимволы:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"text":"123!!!","language":"RU"}'
```

Несуществующий идентификатор:

```bash
curl http://localhost:8080/tasks/00000000-0000-0000-0000-000000000000
```

### PowerShell

В PowerShell действуют свои правила экранирования кавычек и кириллицы. Надёжнее отправлять через `Invoke-RestMethod` с хэш-таблицей:

```powershell
$body = @{
    text     = "Привкт менч зовут Иван"
    language = "RU"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/tasks" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

Получение результата:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tasks/44bd78dc-d08c-41c6-b87d-fb82046bd470"
```

### Postman

1. Метод — `POST`, URL — `http://localhost:8080/tasks`.
2. Вкладка **Body** → **raw** → в выпадающем списке справа выбрать **JSON**.
3. Вставить тело:
   ```json
   {
     "text": "Привкт менч зовут Иван",
     "language": "RU"
   }
   ```
4. Send. В ответе будет `id`.
5. Для проверки результата — новый запрос `GET http://localhost:8080/tasks/{id}`. Через ~5 секунд статус сменится на `DONE`.

### Полный сценарий через curl

```bash
# 1. Создаём задачу и вытаскиваем id
ID=$(curl -s -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"text":"Привкт менч зовут Иван","language":"RU"}' \
  | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo "Task id: $ID"

# 2. Ждём обработки
sleep 6

# 3. Забираем результат
curl -s http://localhost:8080/tasks/$ID
```

Ожидаемый вывод:

```json
{"status":"DONE","correctedText":"Привет меня зовут Иван"}
```

## Как это работает

1. Пользователь отправляет текст на `POST /tasks`. Задача сохраняется со статусом `NEW`.
2. Scheduler раз в 5 секунд забирает пачку `NEW`-задач и переводит их в `IN_PROGRESS`.
3. Каждая задача уходит в `TextCorrectionService`, который:
    - бьёт текст на чанки по 10 000 символов (лимит API Яндекса), стараясь рвать по границам слов;
    - определяет опции: `IGNORE_DIGITS`, если в тексте есть цифры, `IGNORE_URLS`, если есть ссылки;
    - вызывает Яндекс.Спеллер и собирает исправленный текст из ответа.
4. Результат сохраняется в БД со статусом `DONE`. Если API вернул ошибку — `ERROR` с текстом ошибки.

## Тесты

```bash
./gradlew test
```

Покрыты:
- `TextChunker` — разбивка, граничные случаи, Unicode-пробелы;
- `SpellerOptionsResolver` — битовая маска опций;
- `YandexTextCorrectionService` — сборка текста, пересекающиеся ошибки, fallback;
- `YandexSpellerClient` — формат запроса и парсинг ответа;
- `TaskController` — валидация и формат ошибок;
- `TaskService` — создание, смена статусов, claim.

## Конфигурация

Основные параметры в `application.properties`:

```properties
app.scheduler.delay-ms=5000
app.scheduler.batch-size=10
```

## Ограничения

- Scheduler однопоточный. Для реальной нагрузки стоит вынести пул и добавить таймаут на «зависшие» `IN_PROGRESS`-задачи.
- Нет ретраев при ошибках API Яндекса — задача сразу уходит в `ERROR`.
