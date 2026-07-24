# Static Squash Server

A lightweight web server for viewing static content from compressed archives without full extraction.

## Highlights

* AI-generated project;
* Alpha status.

## Overview

In modern CI/CD pipelines, test reports (particularly Allure reports) are often archived to save storage space. When dealing with large test suites, these archives can become substantial. Viewing such reports typically requires full extraction, which can be resource-intensive—especially on constrained environments like VDI with limited resources.

Static Squash Server addresses this challenge by allowing you to browse and serve files directly from compressed archives without extracting them to disk.

## Features

- **No extraction required** — Files are streamed directly from the archive;
- **Supports multiple formats** — ZIP, TAR.GZ, TAR.XZ;
- **Lightweight** — Built with Helidon SE for minimal resource consumption;
- **Native Image support** — Compile to a native binary with GraalVM;
- **Asynchronous processing** — Non-blocking HTTP server with request queuing;
- **Proper MIME types** — Automatic Content-Type detection by file extension;
- **CLI configuration** — Simple command-line interface for server setup.

## Build Commands

### Standard JAR Build

```bash
mvn clean package
```

Produces: `target/StaticSquashServer.jar`

### Native Image Build (GraalVM)

```bash
mvn -P native-image clean package
```

Produces: A native executable binary

## Usage

```bash
# Run with JAR
java -jar target/StaticSquashServer.jar --port 8080 --archive path/to/archive.tar.gz

# Run with native binary
./static-squash-server --port 8080 --archive path/to/archive.tar.gz
```

### Command-Line Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--port` | 8080 | Server port |
| `--archive` | (required) | Path to the archive file |

### Supported Archive Formats

- `.zip`;
- `.tar.gz` (`.tgz`);
- `.tar.xz`.

## Project Background

This project was generated with assistance from neural networks:

- **Primary generation**: [Qwen3.5-122B](https://huggingface.co/unsloth/Qwen3.5-122B-A10B-GGUF);
- **Inference Engine**: [llama.cpp](https://github.com/ggml-org/llama.cpp);
- **Orchestration Tool**: [opencode](https://github.com/anomalyco/opencode);
- **Additional assistance**: Google AI Mode.

All prompts used during development are available in the `ai/` directory for transparency and reproducibility.

## Architecture Highlights

- **Stream-based processing** — Large files are handled via streams without loading entirely into memory;
- **Lazy evaluation** — Computations are deferred until necessary;
- **Immutable objects** — Used throughout where memory copying is not impacted;
- **Request queuing** — Duplicate requests are merged into a single processing queue;
- **Non-blocking archive traversal** — Archive scanning does not block the HTTP server.

## Testing

```bash
# Run tests
mvn test

# Generate Allure report
mvn allure:report
```

## License

This project is open-source and uses a combined licensing model:

* **Software Code**: All Java source code and build scripts are licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](./LICENSE);
* **Prompts and Data**: All AI prompts, instructions, and datasets located in the `/ai` directory are licensed under the [Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)](./LICENSE-PROMPTS).

## Contributing

As this is a young project, the architecture may evolve. Please review the code and task descriptions in the `ai/` directory to understand the development context.

## Acknowledgements

- **Alibaba Cloud** — for developing and open-sourcing the Qwen series of large language models;
- **Unsloth Team** — for Unsloth Dynamic 2.0 Quants;
- **Helidon Team** — for the excellent Java microservices framework;
- **Apache Software Foundation** — for Commons Compress and other foundational libraries;
- **Google** for Google AI Mode.

## Skills sources

- [context7 and java-engineer](https://github.com/JetBrains/junie-extensions);
- [java-patterns](https://github.com/projectious-work/processkit/tree/main/src/context/skills/engineering/java-patterns);
- [api-design, architecture-decision-records, iterative-retrieval, java-coding-standards, search-first, strategic-compact, tdd-workflow, verification-loop](https://github.com/RogerioSobrinho/codeme-copilot/);
- [junit](https://github.com/partme-ai/full-stack-skills/tree/main/skills/testing-skills/junit).

---

*Built with care for efficient archive inspection.*
