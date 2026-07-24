# Conventional Commits Scopes

Анализ git истории ветки `premain`.

## Scopes

| Scope | Описание | Примеры использования |
|-------|----------|----------------------|
| `ai` | AI-генерация, OpenCode skills, промпты | `feat(ai)`, `test(ai)` |
| `archive` | Работа с архивами (ZIP, TAR, XZ) | `refactor(archive)`, `test(archive)` |
| `cli` | Командная строка, JCommander | `refactor(cli)` |
| `contentprovider` | ContentProvider компонент | `refactor(contentprovider)` |
| `core` | Ядро проекта | `feat(core)` |
| `doc` | Документация, README | `refactor(doc)`, `feat(doc)` |
| `github` | Интеграция с GitHub | `feat(github)` |
| `log` | Логирование | `refactor(log)`, `feat(log)` |
| `native` | Native image, shading | `build(native)` |
| `server` | Веб-сервер, ArchiveWebServer | `fix(server)`, `refactor(server)` |
| `spell` | Исправление опечаток | `fix(spell)` |
| `test` | Тесты (общие) | `refactor(test)` |
| `deps` | Зависимости, версии | `build(deps)` |

## Типы коммитов

| Type | Описание |
|------|----------|
| `feat` | Новые функции |
| `fix` | Исправления багов |
| `refactor` | Рефакторинг кода |
| `test` | Добавление/изменение тестов |
| `build` | Сборка, зависимости, версии |

## Формат коммитов

```
<type>(<scope>): <description>
```

### Примеры

```bash
feat(ai): add new skill for code review
fix(server): resolve race condition in scan loop
refactor(archive): reduce code duplication
test(archive): add integration test for TAR.XZ
build(deps): version up
```
