# AGENTS.md — Guidelines for AI-Assisted Development

This document provides instructions for AI agents (including opencode, Qwen, Google AI) working on the Static Squash Server project.

---

## Project Overview

**Static Squash Server** — a lightweight web server for viewing static content from compressed archives without pre-extraction. Optimized for viewing Allure reports and similar static assets.

### Tech Stack

- **Java**: 25
- **Framework**: Helidon SE 4.5.0
- **Archive Handling**: Apache Commons Compress 1.28.0
- **CLI**: JCommander 3.0
- **Compression Libraries**: commons-io 2.22.0, xz 1.12
- **Build Tool**: Maven
- **Target**: GraalVM Native Image support

### Supported Archive Formats

- `.zip`
- `.tar.gz` (`.tgz`)
- `.tar.xz`

---

## Mandatory Requirements

### Coding Standards

1. **Java 25 Features**: Use records, sealed classes, pattern matching, switch expressions, text blocks, `var`, Stream API, and Optional where appropriate.

2. **Immutability**: Use `final` and immutable objects everywhere where it doesn't lead to memory copying.

3. **Lazy Evaluation**: Apply lazy computations wherever possible.

4. **No Deprecated APIs**: Using methods marked `@Deprecated` is strictly prohibited. Always verify with:
   - Maven dependency javadocs: `mvn dependency:sources dependency:resolve -Dclassifier=javadoc`
   - **context7** MCP server for up-to-date documentation
   - Official library documentation

5. **Async Programming**: Respect Helidon SE's asynchronous, non-blocking architecture.

6. **Stream Processing**: Handle giant files via streaming — never load entire files into memory.

7. **No Pre-extraction**: Strictly forbidden to extract archives or individual files to disk.

### Architecture Constraints

- ❌ No authorization
- ❌ No encryption
- ❌ No caching (at this stage)
- ❌ No databases
- ❌ No tests (separate task)

### HTTP Requirements

- Set `Content-Type` header correctly based on file extension
- Return `404` for missing files
- Stream file content directly from archive to Helidon response stream

### Logging

- Minimal logging using built-in Helidon logging (java.util.logging)
- No additional logging dependencies

---

## Agent Workflow

### 1. Search Before Writing

**Always** search the codebase for existing implementations before creating new classes or methods:

```bash
# Use grep to find similar patterns
grep -r "class.*Service" src/main/java
grep -r "@GetMapping" src/main/java
```

Prevent duplication of:
- Spring Boot auto-configurations
- Existing business logic
- Library capabilities

### 2. Use context7 for Documentation

**Force use context7** for any library/framework questions:

```
1. Resolve library ID: context7_resolve-library-id
2. Query documentation: context7_query-docs
```

Priority sources (in order):
1. Maven repository javadocs (`~/.m2/repository/`)
2. **context7** MCP server
3. Apache Commons Compress docs: https://commons.apache.org/proper/commons-compress/examples.html
4. Tests in the codebase

### 3. Leverage Skills

Load appropriate skills based on task type:

| Task Type | Skill |
|-----------|-------|
| REST endpoints, HTTP status codes, API versioning, pagination, ProblemDetail errors, OpenAPI annotations | `api-design` |
| Architectural decisions, ADRs | `architecture-decision-records` |
| Library documentation, setup, CLI tools | `context7` |
| opencode configuration | `customize-opencode` |
| Large Java codebase navigation | `iterative-retrieval` |
| Java 17+ idioms (records, sealed types, pattern matching, streams) | `java-coding-standards` |
| Java 17–25 policy, pitfalls, refactoring | `java-engineer` |
| Modern Java patterns, Spring Boot | `java-patterns` |
| JUnit testing | `junit` |
| New feature/bug fix implementation | `search-first` |
| Session compaction, context management | `strategic-compact` |
| TDD workflow (RED → GREEN → REFACTOR) | `tdd-workflow` |
| Quality gates, verification pipeline | `verification-loop` |

### 4. MCP Server Usage

Use MCP servers for:
- **context7**: Up-to-date library documentation
- Resource templates for structured data access

---

## Project Structure

```
src/main/java/ru/devaarno/staticsquashserver/
├── Main.java                 # Application entry point
├── ArchiveWebServer.java     # Core server logic
├── ContentProvider.java      # Archive content access
└── cli/
    └── ServerConfig.java     # CLI configuration (JCommander)
```

### Key Components

1. **Main**: Parses CLI arguments, initializes server
2. **ArchiveWebServer**: Manages HTTP routes, request queuing, archive traversal
3. **ContentProvider**: Stream-based file access from archives
4. **ServerConfig**: CLI parameters (`--port`, `--archive`)

---

## Request Flow

```
┌─────────────┐
│  GET /path  │
└──────┬──────┘
       ▼
┌─────────────────┐
│ Request Queue   │ (merge duplicates)
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Scan Archive    │ (non-blocking)
└──────┬──────────┘
       ▼
┌─────────────────┐
│ File Found? ────┼─── No ──► 404
│     Yes         │
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Stream Content  │ (no extraction)
│ Set Content-Type│
└─────────────────┘
```

---

## Build & Run

### Build

```bash
# Standard JAR
mvn clean package

# Native Image
mvn -P native-image clean package
```

### Run

```bash
java -jar target/StaticSquashServer.jar --port 8080 --archive path/to/archive.tar.gz
```

### CLI Parameters

| Parameter | Default | Required | Description |
|-----------|---------|----------|-------------|
| `--port` | 8080 | No | Server port |
| `--archive` | — | Yes | Path to archive file |

---

## Verification Commands

```bash
# Compile
mvn compile

# Run tests
mvn test

# Check for deprecated API usage
mvn compiler:compile -X  # Enable debug to see warnings

# Generate Allure report
mvn allure:report
```

---

## Commit Convention

Use **Conventional Commits** with scopes:

```
<type>(<scope>): <description>
```

### Scopes

| Scope | Area |
|-------|------|
| `ai` | AI generation, prompts, skills |
| `archive` | Archive handling (ZIP, TAR, XZ) |
| `cli` | Command-line interface |
| `contentprovider` | ContentProvider component |
| `core` | Core project logic |
| `doc` | Documentation |
| `log` | Logging |
| `native` | Native image, shading |
| `server` | Web server, ArchiveWebServer |
| `deps` | Dependencies, versions |

### Examples

```bash
feat(server): add request queue for duplicate merging
fix(archive): handle corrupted ZIP gracefully
refactor(contentprovider): use stream-based reading
build(deps): upgrade commons-compress to 1.28.0
```

---

## Token Budget

- **Target limit**: 100,000 tokens per session
- Use `strategic-compact` skill when approaching limits
- Summarize progress when switching major tasks

---

## License

- **Code**: AGPL-3.0
- **Prompts/Data in `/ai`**: CC BY-SA 4.0

---

*Last updated: 2026-07-04*

<!-- CODEGRAPH_START -->
## CodeGraph

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), reach for it BEFORE grep/find or reading files when you need to understand or locate code:

- **MCP tool** (when available): `codegraph_explore` answers most code questions in one call — the relevant symbols' verbatim source plus the call paths between them, including dynamic-dispatch hops grep can't follow. Name a file or symbol in the query to read its current line-numbered source. If it's listed but deferred, load it by name via tool search.
- **Shell** (always works): `codegraph explore "<symbol names or question>"` prints the same output.

If there is no `.codegraph/` directory, skip CodeGraph entirely — indexing is the user's decision.
<!-- CODEGRAPH_END -->
