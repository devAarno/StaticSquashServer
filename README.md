# Static Squash Server

A lightweight web server for viewing static content from compressed archives without prior extraction.

## Highlights

* AI-generated project;
* Alpha status.

---

## Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Solution](#solution)
- [Technology Stack](#technology-stack)
- [Build Instructions](#build-instructions)
- [Usage](#usage)
- [AI-Assisted Development](#ai-assisted-development)
- [License](#license)
- [Acknowledgements](#acknowledgements)

---

## Overview

Static Squash Server is a minimal HTTP server designed to serve files directly from compressed archives (`.tar.gz`, `.zip`, `.tar.xz`) without extracting them to disk. The project is optimised for scenarios where archive sizes are large and disk space or I/O performance is constrained.

---

## Problem Statement

In modern CI/CD pipelines, automated test reports (particularly Allure reports) are often archived to conserve storage and meet artifact size limits. When reviewing these reports, users typically face the following challenges:

- Large archive files require significant time and resources to extract.
- Resource-constrained environments (e.g., restricted VDI sessions) may lack sufficient disk space or processing power.
- Extracting an entire archive just to view a single file is inefficient and wasteful.

---

## Solution

Static Squash Server addresses these issues by:

- Serving files directly from compressed archives via HTTP.
- Streaming file contents on demand without full extraction.
- Preserving archive directory structure in URL paths.
- Supporting concurrent requests with internal queue management.
- Operating with minimal memory footprint using lazy evaluation and streaming I/O.

This approach enables efficient report viewing even on systems with limited resources.

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 25 |
| Web Framework | Helidon SE 4.4.1 |
| Archive Handling | Apache Commons Compress 1.28.0 |
| CLI Parsing | JCommander 3.0 |
| Build Tool | Apache Maven |
| Native Compilation | GraalVM Native Image |
| Testing | JUnit 5, AssertJ, Allure |

### Key Features

- **Asynchronous I/O**: Non-blocking request handling with Helidon.
- **Streaming**: Files are streamed directly from archives without temporary extraction.
- **Native Support**: Compiled binaries via GraalVM for reduced startup time and memory usage.
- **Content-Type Detection**: Automatic HTTP `Content-Type` header based on file extension.

---

## Build Instructions

### Prerequisites

- Java 25 (or compatible JDK)
- Apache Maven 3.9+
- GraalVM (for native image builds)

### Standard JAR Build

```bash
mvn clean package
```

Output: `target/StaticSquashServer.jar`

### Native Image Build

```bash
mvn -P native-image clean package
```

Output: Native executable in `target/`

---

## Usage

### Running from JAR

```bash
java -jar target/StaticSquashServer.jar --port 8080 --archive path/to/archive.tar.gz
```

### Running Native Binary

```bash
./target/staticsquashserver --port 8080 --archive path/to/archive.tar.gz
```

### Command-Line Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `--port` | HTTP server port | `8080` |
| `--archive` | Path to archive file (`.zip`, `.tar.gz`, `.tar.xz`) | Required |

### Example

```bash
java -jar target/StaticSquashServer.jar --archive allure-results.tar.gz
```

Once started, navigate to `http://localhost:8080/index.html` (or any file within the archive).

---

## AI-Assisted Development

This project was developed with significant assistance from artificial intelligence:

- **AI Model**: Qwen3.5-122B
- **Inference Engine**: `llama.cpp`
- **Orchestration Tool**: [opencode](https://github.com/anomalyco/opencode)

All AI prompts and task specifications are preserved in the [`ai`](ai/) directory for full transparency and reproducibility. Manual code reviews and adjustments were performed throughout the development process.

---

## Licensing

This project is open-source and uses a combined licensing model:

* **Software Code**: All Java source code and build scripts are licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](./LICENSE);
* **Prompts and Data**: All AI prompts, instructions, and datasets located in the `/ai` directory are licensed under the [Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)](./LICENSE-PROMPTS).

---

## Acknowledgements

- **Alibaba Cloud** — for developing and open-sourcing the Qwen series of large language models.
- **Helidon Team** — for the excellent Java microservices framework.
- **Apache Software Foundation** — for Commons Compress and other foundational libraries.

---

## Skills sources

- [context7 and java-engineer](https://github.com/JetBrains/junie-extensions);
- [java-patterns](https://github.com/projectious-work/processkit/tree/main/src/context/skills/engineering/java-patterns);
- [api-design, architecture-decision-records, iterative-retrieval, java-coding-standards, search-first, strategic-compact, tdd-workflow, verification-loop](.opencode/skills/verification-loop)](.opencode/skills/tdd-workflow)](.opencode/skills/strategic-compact)](.opencode/skills/search-first)](.opencode/skills/java-coding-standards)](.opencode/skills/iterative-retrieval)](.opencode/skills/architecture-decision-records)](https://github.com/RogerioSobrinho/codeme-copilot/);
- [junit](https://github.com/partme-ai/full-stack-skills/tree/main/skills/testing-skills/junit)

---

## Contributing

As this is a young and evolving project, contributions and feedback are welcome. Please ensure that any changes align with the project's core principles: simplicity, efficiency, and minimal resource consumption.

---

*Built with care for efficient archive inspection.*
