<!--
Copyright (C) 2026 Parkhomenko Stanislav
This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
To view a copy of this license, visit http://creativecommons.org
-->

На основе анализа макета задачи, pom.xml и документации по технологиям, составляю подробный план разработки.

---

# План разработки Web-сервера для статического содержимого архивов

## Общая архитектура

Проект представляет собой одиночный Java-класс (или небольшой набор классов), реализующий HTTP-сервер на Helidon SE 4.4, который:
1. При запуске сканирует указанный архив и регистрирует маршруты для каждого файла
2. При запросе к файлу ищет его в архиве и передаёт содержимое потоком
3. Поддерживает очередь запросов при параллельных обращениях

---

## Этап 1: Настройка CLI-параметров (jcommander)

**Цель:** Реализовать парсинг командной строки для получения порта и пути к архиву.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.cli.CliConfig` — класс-контейнер параметров

**Реализация:**
```java
// ru.devaarno.staticsquashserver.cli.CliConfig
public final class CliConfig {
    @Parameter(names = {"-p", "--port"}, description = "Port to bind", order = 1)
    private int port = 8080;
    
    @Parameter(names = {"-a", "--archive"}, description = "Path to archive file", order = 2)
    private String archivePath;
    
    // getters, validation logic
}
```

**Способ реализации:**
- Использовать `JCommander.newBuilder().addObject(config).build().parse(args)`
- Валидация: порт 1-65535, файл архива существует и читаем
- По умолчанию порт 8080, архив обязателен

**Контекст для восстановления:**
- Зависимость: `org.jcommander:jcommander:3.0`
- Аннотация: `@Parameter(names = {...}, description = "...", order = N)`
- Default port: 8080

---

## Этап 2: Модель данных архива

**Цель:** Создать иммутабельную модель для представления файлов внутри архива.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo` — информация о файле в архиве
- `ru.devaarno.staticsquashserver.archive.ArchiveDescriptor` — дескриптор всего архива

**Реализация:**
```java
// ru.devaarno.staticsquashserver.archive.ArchiveEntryInfo
public final class ArchiveEntryInfo {
    private final String path;           // путь внутри архива (например, "report/index.html")
    private final long size;             // размер в байтах
    private final MediaType mediaType;   // тип контента
    private final Instant modificationTime;
    
    // constructor, getters only (immutable)
}

// ru.devaarno.staticsquashserver.archive.ArchiveDescriptor
public final class ArchiveDescriptor {
    private final Path archivePath;
    private final ArchiveFormat format;  // ZIP, TAR_GZ, TAR_XZ
    private final List<ArchiveEntryInfo> entries;
    
    // constructor, getters
}
```

**Способ реализации:**
- Использовать `record` или класс с `final` полями и конструктором
- `MediaType` — из `io.helidon.http.MediaType`
- `ArchiveFormat` — enum с тремя значениями: `ZIP`, `TAR_GZ`, `TAR_XZ`

**Контекст для восстановления:**
- Пакет: `ru.devaarno.staticsquashserver.archive`
- Иммутабельность: только getters, no setters
- MediaType определяется по расширению файла через `MediaType.create(String)`

---

## Этап 3: Парсер архива (Apache Commons Compress)

**Цель:** Реализовать сканирование архива и извлечение метаданных файлов.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.archive.ArchiveParser` — интерфейс парсера
- `ru.devaarno.staticsquashserver.archive.ZipArchiveParser` — парсер ZIP
- `ru.devaarno.staticsquashserver.archive.TarGzArchiveParser` — парсер TAR.GZ
- `ru.devaarno.staticsquashserver.archive.TarXzArchiveParser` — парсер TAR.XZ
- `ru.devaarno.staticsquashserver.archive.ArchiveParserFactory` — фабрика по расширению файла

**Реализация:**
```java
// ru.devaarno.staticsquashserver.archive.ArchiveParser
public interface ArchiveParser {
    ArchiveDescriptor parse(Path archivePath);
    InputStream getEntryInputStream(Path archivePath, String entryPath);
}

// ru.devaarno.staticsquashserver.archive.ArchiveParserFactory
public final class ArchiveParserFactory {
    public static ArchiveParser create(Path archivePath) {
        String name = archivePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip")) return new ZipArchiveParser();
        if (name.endsWith(".tar.gz")) return new TarGzArchiveParser();
        if (name.endsWith(".tar.xz")) return new TarXzArchiveParser();
        throw new IllegalArgumentException("Unsupported archive format");
    }
}
```

**Способ реализации для ZIP:**
```java
// ru.devaarno.staticsquashserver.archive.ZipArchiveParser
public final class ZipArchiveParser implements ArchiveParser {
    @Override
    public ArchiveDescriptor parse(Path archivePath) {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            Enumeration<ZipArchiveEntry> entriesEnum = zipFile.getEntries();
            while (entriesEnum.hasMoreElements()) {
                ZipArchiveEntry entry = entriesEnum.nextElement();
                if (!entry.isDirectory()) {
                    entries.add(new ArchiveEntryInfo(
                        entry.getName(),
                        entry.getSize(),
                        detectMediaType(entry.getName()),
                        Instant.ofEpochMilli(entry.getTime())
                    ));
                }
            }
        }
        return new ArchiveDescriptor(archivePath, ArchiveFormat.ZIP, entries);
    }
    
    @Override
    public InputStream getEntryInputStream(Path archivePath, String entryPath) throws IOException {
        ZipFile zipFile = new ZipFile(archivePath.toFile());
        ZipArchiveEntry entry = zipFile.getEntry(entryPath);
        if (entry == null) {
            zipFile.close();
            return null;
        }
        return zipFile.getInputStream(entry); // поток остаётся открытым, caller закрывает
    }
}
```

**Способ реализации для TAR.GZ:**
```java
// ru.devaarno.staticsquashserver.archive.TarGzArchiveParser
public final class TarGzArchiveParser implements ArchiveParser {
    @Override
    public ArchiveDescriptor parse(Path archivePath) {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        try (InputStream fis = Files.newInputStream(archivePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.add(new ArchiveEntryInfo(
                        entry.getName(),
                        entry.getSize(),
                        detectMediaType(entry.getName()),
                        Instant.ofEpochMilli(entry.getLastModifiedDate().getTime())
                    ));
                }
            }
        }
        return new ArchiveDescriptor(archivePath, ArchiveFormat.TAR_GZ, entries);
    }
    
    @Override
    public InputStream getEntryInputStream(Path archivePath, String entryPath) throws IOException {
        InputStream fis = Files.newInputStream(archivePath);
        BufferedInputStream bis = new BufferedInputStream(fis);
        GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
        TarArchiveInputStream tais = new TarArchiveInputStream(gzis);
        
        TarArchiveEntry entry;
        while ((entry = tais.getNextEntry()) != null) {
            if (entry.getName().equals(entryPath)) {
                return tais; // caller закроет весь стек потоков
            }
        }
        tais.close();
        return null;
    }
}
```

**Способ реализации для TAR.XZ:**
```java
// ru.devaarno.staticsquashserver.archive.TarXzArchiveParser
// Аналогично TAR.GZ, но с XZCompressorInputStream вместо GzipCompressorInputStream
```

**Важные замечания:**
- Для TAR-архивов поиск файла требует последовательного перебора (нет random access)
- Поток должен оставаться открытым после возврата из `getEntryInputStream`
- При повреждении архива бросать `IOException` или `ArchiveException`

**Контекст для восстановления:**
- Зависимость: `org.apache.commons:commons-compress:1.28.0`
- Классы: `ZipFile`, `ZipArchiveEntry`, `GzipCompressorInputStream`, `XZCompressorInputStream`, `TarArchiveInputStream`, `TarArchiveEntry`
- Обёртка потоков: `BufferedInputStream` обязательна для производительности
- Проверка на директорию: `entry.isDirectory()`

---

## Этап 4: Определение MediaType по расширению

**Цель:** Реализовать маппинг расширений файлов в HTTP Content-Type.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.mime.MimeTypeResolver`

**Реализация:**
```java
// ru.devaarno.staticsquashserver.mime.MimeTypeResolver
public final class MimeTypeResolver {
    private static final Map<String, MediaType> EXTENSION_TO_TYPE = Map.of(
        "html", MediaType.TEXT_HTML,
        "htm", MediaType.TEXT_HTML,
        "css", MediaType.TEXT_CSS,
        "js", MediaType.APPLICATION_JAVASCRIPT,
        "json", MediaType.APPLICATION_JSON,
        "png", MediaType.IMAGE_PNG,
        "jpg", MediaType.IMAGE_JPEG,
        "jpeg", MediaType.IMAGE_JPEG,
        "gif", MediaType.IMAGE_GIF,
        "svg", MediaType.IMAGE_SVG,
        "txt", MediaType.TEXT_PLAIN,
        "xml", MediaType.APPLICATION_XML,
        "ico", MediaType.valueOf("image/x-icon"),
        "woff", MediaType.valueOf("font/woff"),
        "woff2", MediaType.valueOf("font/woff2"),
        "ttf", MediaType.valueOf("font/ttf"),
        "eot", MediaType.valueOf("application/vnd.ms-fontobject"),
        "default", MediaType.APPLICATION_OCTET_STREAM
    );
    
    public static MediaType resolve(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) return EXTENSION_TO_TYPE.get("default");
        String ext = fileName.substring(lastDot + 1).toLowerCase();
        return EXTENSION_TO_TYPE.getOrDefault(ext, EXTENSION_TO_TYPE.get("default"));
    }
}
```

**Способ реализации:**
- Использовать `MediaType.valueOf(String)` для нестандартных типов
- Все стандартные типы из `io.helidon.http.MediaType`

**Контекст для восстановления:**
- Пакет: `ru.devaarno.staticsquashserver.mime`
- Возвращать `APPLICATION_OCTET_STREAM` для неизвестных расширений
- Allure-отчёт содержит много HTML, JS, CSS, JSON — эти типы приоритетны

---

## Этап 5: HTTP-сервер и маршрутизация

**Цель:** Запустить Helidon SE сервер и зарегистрировать маршруты для всех файлов архива.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.server.ArchiveWebServer` — основной класс сервера

**Реализация:**
```java
// ru.devaarno.staticsquashserver.server.ArchiveWebServer
public final class ArchiveWebServer {
    private final WebServer server;
    private final ArchiveParser archiveParser;
    private final ArchiveDescriptor archiveDescriptor;
    
    public ArchiveWebServer(CliConfig config) {
        Path archivePath = Path.of(config.getArchivePath());
        this.archiveParser = ArchiveParserFactory.create(archivePath);
        this.archiveDescriptor = archiveParser.parse(archivePath);
        
        // Валидация: если парсинг упал с исключением — приложение завершается
        if (archiveDescriptor.entries().isEmpty()) {
            throw new IllegalStateException("Archive contains no files");
        }
        
        this.server = WebServer.builder()
            .port(config.getPort())
            .routing(this::setupRouting)
            .build();
    }
    
    private void setupRouting(HttpRouting.Builder routing) {
        // Регистрируем маршрут для каждого файла
        for (ArchiveEntryInfo entry : archiveDescriptor.entries()) {
            String path = "/" + entry.getPath().replace('\\', '/');
            routing.get(path, createHandler(entry));
        }
        
        // 404 для всех остальных путей
        routing.any((req, resp) -> {
            resp.status(Http.Status.NOT_FOUND_404);
            resp.send("Not found");
        });
    }
    
    private HttpService createHandler(ArchiveEntryInfo entryInfo) {
        return (req, resp) -> {
            // Обработка запроса
            try (InputStream entryStream = archiveParser.getEntryInputStream(
                    archiveDescriptor.archivePath(), 
                    entryInfo.path())) {
                
                if (entryStream == null) {
                    resp.status(Http.Status.NOT_FOUND_404);
                    resp.send("File not found in archive");
                    return;
                }
                
                resp.headers().contentLength(entryInfo.size());
                resp.headers().contentType(entryInfo.mediaType());
                resp.send(entryStream);
            } catch (IOException e) {
                resp.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Error reading file from archive");
            }
        };
    }
    
    public void start() {
        server.start();
        System.out.println("Server started on port " + server.port());
    }
    
    public void stop() {
        server.stop();
    }
}
```

**Контекст для восстановления:**
- Helidon SE 4.x использует синхронный `server.start()` (не возвращает CompletableFuture)
- `HttpRouting.Builder` — для регистрации маршрутов
- `HttpService` — функциональный интерфейс `(req, resp) -> void`
- `resp.send(InputStream)` — передаёт поток напрямую

---

## Этап 6: Очередь запросов и асинхронность

**Цель:** Реализовать очередь для обработки параллельных запросов к одному файлу.

**Ключевые классы:**
- `ru.devaarno.staticsquashserver.server.RequestQueue` — очередь запросов
- `ru.devaarno.staticsquashserver.server.EntryRequest` — задача запроса файла

**Реализация:**
```java
// ru.devaarno.staticsquashserver.server.EntryRequest
public final class EntryRequest {
    private final String entryPath;
    private final CompletableFuture<InputStream> result;
    private final CountDownLatch latch;
    
    public EntryRequest(String entryPath) {
        this.entryPath = entryPath;
        this.result = new CompletableFuture<>();
        this.latch = new CountDownLatch(1);
    }
}

// ru.devaarno.staticsquashserver.server.RequestQueue
public final class RequestQueue {
    private final Map<String, EntryRequest> pendingRequests = new ConcurrentHashMap<>();
    private final ArchiveParser archiveParser;
    private final Path archivePath;
    private final ExecutorService executor;
    
    public RequestQueue(ArchiveParser archiveParser, Path archivePath) {
        this.archiveParser = archiveParser;
        this.archivePath = archivePath;
        this.executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 virtual threads
    }
    
    public InputStream getEntryStream(String entryPath) throws InterruptedException {
        EntryRequest request = pendingRequests.computeIfAbsent(entryPath, path -> {
            EntryRequest req = new EntryRequest(path);
            executor.execute(() -> processRequest(req));
            return req;
        });
        
        request.latch.await(); // ждём завершения обработки
        pendingRequests.remove(entryPath);
        
        InputStream stream = request.result.join();
        if (stream == null) {
            throw new FileNotFoundException("Entry not found: " + entryPath);
        }
        return stream;
    }
    
    private void processRequest(EntryRequest request) {
        try {
            InputStream stream = archiveParser.getEntryInputStream(archivePath, request.entryPath);
            request.result.complete(stream);
        } catch (IOException e) {
            request.result.completeExceptionally(e);
        } finally {
            request.latch.countDown();
        }
    }
}
```

**Важные замечания:**
- Дублирующиеся запросы объединяются через `ConcurrentHashMap.computeIfAbsent`
- Virtual threads (Java 25) обеспечивают эффективную параллельную обработку
- `CountDownLatch` синхронизирует ожидание завершения обработки

**Контекст для восстановления:**
- `Executors.newVirtualThreadPerTaskExecutor()` — для Java 21+
- `ConcurrentHashMap.computeIfAbsent` — атомарное создание и возврат
- `CompletableFuture` — для асинхронного результата
- `CountDownLatch` — для синхронизации ожидания

---

## Этап 7: Интеграция очереди в обработчик запросов

**Цель:** Модифицировать обработчик для использования очереди.

**Реализация:**
```java
// ru.devaarno.staticsquashserver.server.ArchiveWebServer (модификация)
public final class ArchiveWebServer {
    private final RequestQueue requestQueue;
    
    // ... в конструкторе
    this.requestQueue = new RequestQueue(archiveParser, archivePath);
    
    private HttpService createHandler(ArchiveEntryInfo entryInfo) {
        return (req, resp) -> {
            try {
                InputStream entryStream = requestQueue.getEntryStream(entryInfo.path());
                
                resp.headers().contentLength(entryInfo.size());
                resp.headers().contentType(entryInfo.mediaType());
                resp.send(entryStream);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Request interrupted");
            } catch (FileNotFoundException e) {
                resp.status(Http.Status.NOT_FOUND_404);
                resp.send("File not found in archive");
            } catch (Exception e) {
                resp.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                resp.send("Error: " + e.getMessage());
            }
        };
    }
}
```

---

## Этап 8: Обработка ошибок и завершение приложения

**Цель:** Корректно завершать приложение при повреждении архива.

**Реализация:**
```java
// ru.devaarno.staticsquashserver.Main (модификация)
public final class Main {
    public static void main(String[] args) {
        try {
            CliConfig config = parseCli(args);
            
            ArchiveWebServer server = new ArchiveWebServer(config);
            server.start();
            
            // Добавляем обработчик shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
                System.out.println("Server stopped");
            }));
            
            server.awaitShutdown();
            
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Archive error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static CliConfig parseCli(String[] args) {
        CliConfig config = new CliConfig();
        JCommander.newBuilder()
            .addObject(config)
            .build()
            .parse(args);
        config.validate();
        return config;
    }
}
```

**Контекст для восстановления:**
- При `IOException` во время парсинга архива — приложение завершается с кодом 1
- Shutdown hook для корректной остановки сервера
- `server.awaitShutdown()` — блокирует главный поток до остановки

---

## Этап 9: Логирование

**Цель:** Минималистичное логирование без дополнительных зависимостей.

**Реализация:**
```java
// ru.devaarno.staticsquashserver.logging.SimpleLogger
public final class SimpleLogger {
    private static final String PREFIX = "[StaticSquashServer]";
    
    public static void info(String message) {
        System.out.println(PREFIX + " INFO: " + message);
    }
    
    public static void warn(String message) {
        System.err.println(PREFIX + " WARN: " + message);
    }
    
    public static void error(String message, Throwable t) {
        System.err.println(PREFIX + " ERROR: " + message);
        t.printStackTrace(System.err);
    }
}
```

**Использование:**
- `SimpleLogger.info("Server started on port " + port)`
- `SimpleLogger.error("Failed to parse archive", e)`

---

## Этап 10: GraalVM Native Image поддержка

**Цель:** Убедиться, что проект компилируется в нативный образ.

**Проверки:**
1. Избегать рефлексии там, где это возможно
2. JCommander использует рефлексию — добавить конфигурацию для GraalVM
3. Apache Commons Compress может требовать конфигурации

**Реализация:**
```java
// src/main/resources/META-INF/native-image/ru.devaarno.staticsquashserver/static-squash-server/proxy-config.json
[
  ["org.apache.commons.compress.archivers.ArchiveInputStream"],
  ["org.apache.commons.compress.compressors.CompressorInputStream"]
]

// src/main/resources/META-INF/native-image/ru.devaarno.staticsquashserver/static-squash-server/reflect-config.json
[
  {
    "name": "ru.devaarno.staticsquashserver.cli.CliConfig",
    "allDeclaredFields": true
  }
]
```

**Контекст для восстановления:**
- В pom.xml уже настроен `native-maven-plugin`
- Команда сборки: `mvn native:compile`
- Тестирование: `./target/static-squash-server --archive bundle.tar.gz --port 8080`

---

## Итоговая структура проекта

```
src/main/java/ru/devaarno/
├── Main.java                          # Точка входа
├── cli/
│   └── CliConfig.java                 # CLI параметры (jcommander)
├── archive/
│   ├── ArchiveEntryInfo.java          # Модель файла в архиве
│   ├── ArchiveDescriptor.java         # Модель архива
│   ├── ArchiveFormat.java             # Enum форматов
│   ├── ArchiveParser.java             # Интерфейс парсера
│   ├── ArchiveParserFactory.java      # Фабрика парсеров
│   ├── ZipArchiveParser.java          # Парсер ZIP
│   ├── TarGzArchiveParser.java        # Парсер TAR.GZ
│   └── TarXzArchiveParser.java        # Парсер TAR.XZ
├── mime/
│   └── MimeTypeResolver.java          # Маппинг расширений в MediaType
├── server/
│   ├── ArchiveWebServer.java          # HTTP сервер
│   ├── RequestQueue.java              # Очередь запросов
│   └── EntryRequest.java              # Задача запроса
└── logging/
    └── SimpleLogger.java              # Простой логгер
```

---

## Ключевые моменты для реализации

| Пункт | Способ реализации | Контекст |
|-------|-------------------|----------|
| CLI | JCommander `@Parameter` | Порт 8080 по умолчанию |
| Архив | Apache Commons Compress `ArchiveInputStream` | TAR.GZ, TAR.XZ, ZIP |
| Streaming | `resp.send(InputStream)` | Без временных файлов |
| MediaType | `MimeTypeResolver` по расширению | Allure-отчёт приоритет |
| Очередь | `ConcurrentHashMap` + `CompletableFuture` | Объединение дублей |
| Асинхронность | Virtual threads (Java 25) | `Executors.newVirtualThreadPerTaskExecutor()` |
| Логирование | `System.out.println` | Без зависимостей |
| GraalVM | `native-maven-plugin` + конфигурация | Рефлексия JCommander |

---

## Дополнительные уточнения к плану

### Пункт 3 (очередь) — детализация

Текущая реализация `RequestQueue` корректно объединяет запросы к **одному и тому же файлу**. Запросы к разным файлам обрабатываются независимо. Это соответствует требованию.

**Поведение:**
- Запрос A к `/report/index.html` → создаётся задача, поток запускается
- Запрос B к `/report/index.html` (пока A не завершён) → возвращается та же задача, оба ждут
- Запрос C к `/report/results.json` → создаётся отдельная задача, не блокируется A/B

---

### Пункт 1 (Allure) — общий подход

Не требуется специальная логика для Allure. Общее решение автоматически работает:
- `index.html` будет доступен по `/report/index.html` (или как указано в архиве)
- Статические ресурсы (JS, CSS, images) будут отдаваться с правильным `Content-Type`

---

## Финальная версия плана

План готов к реализации. Ниже — краткая сводка для быстрого старта в следующей сессии:

### Структура реализации (порядок выполнения)

1. **`CliConfig.java`** — CLI с JCommander
2. **`ArchiveEntryInfo.java`, `ArchiveDescriptor.java`, `ArchiveFormat.java`** — модели
3. **`MimeTypeResolver.java`** — маппинг расширений
4. **`ArchiveParser.java` + реализации** — парсинг архивов
5. **`RequestQueue.java`** — очередь запросов
6. **`ArchiveWebServer.java`** — HTTP сервер
7. **`Main.java`** — точка входа, интеграция
8. **`SimpleLogger.java`** — логирование (опционально, можно inline)
9. **GraalVM конфигурация** — если потребуется

### Критические зависимости

| Зависимость | Версия | Использование |
|-------------|--------|---------------|
| Helidon WebServer | 4.4.0 | HTTP сервер |
| Apache Commons Compress | 1.28.0 | Работа с архивами |
| JCommander | 3.0 | CLI парсинг |

### Критические API

| Задача | API |
|--------|-----|
| Запуск сервера | `WebServer.builder().port().routing().build().start()` |
| Отправка потока | `resp.headers().contentType().contentLength(); resp.send(inputStream)` |
| ZIP архив | `ZipFile(File).getInputStream(ZipArchiveEntry)` |
| TAR.GZ архив | `new TarArchiveInputStream(new GzipCompressorInputStream(is))` |
| TAR.XZ архив | `new TarArchiveInputStream(new XZCompressorInputStream(is))` |
| CLI | `JCommander.newBuilder().addObject().build().parse()` |
| Virtual threads | `Executors.newVirtualThreadPerTaskExecutor()` |

### Потенциальные проблемы

1. **JCommander + GraalVM** — может потребовать `reflect-config.json`
2. **TAR-архивы без random access** — поиск файла требует полного перебора при каждом запросе (медленно для больших архивов)
3. **Потоки остаются открытыми** — caller должен закрывать `InputStream` после использования

---

План завершён. Готов к переходу в режим реализации по вашему сигналу.
