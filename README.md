# Hex4WebFlux (hex4w)

App template for Spring WebFlux with Hexagonal Architecture (Ports & Adapters), reactive programming, and GraalJS script execution.

## Prerequisites

- Java 21+ (Temurin, Corretto, etc.)
- Gradle 8.7+ (wrapper included)

## Quick Start

```bash
# Clone
git clone https://github.com/kaesar/onmind-hex4w.git hex4w
cd hex4w

# Run (uses embedded H2 database)
./gradlew bootRun
```

App starts at `http://localhost:8080`. Health check: `GET /actuator/health`.

## Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/roles` | Create role |
| `GET` | `/api/v1/roles` | List all roles |
| `GET` | `/api/v1/roles/{id}` | Get role by ID |
| `GET` | `/api/v1/roles/search?name={pattern}` | Search roles |
| `POST` | `/api/v1/scripts/execute` | Execute whitelisted `.js` file (GraalJS) |
| `GET` | `/api/v1/store/items?bucket={name}` | List S3 bucket objects |

### Script Execution Example

```bash
curl -X POST http://localhost:8080/api/v1/scripts/execute \
  -H "Content-Type: application/json" \
  -d '{"script": "hello.js"}'
```

Allowed scripts: `hello.js`, `example.js` (defined in `AllowedScript` enum, stored in `src/main/resources/scripts/`).

## Architecture

```
domain/          # Pure business logic (models, services, exceptions)
application/     # Use cases, DTOs, mappers, ports (in/out)
infrastructure/  # Adapters: WebFlux handlers, R2DBC repos, GraalJS, S3
transverse/      # Cross-cutting: global error handler, reactive logging
```

- **Reactive end-to-end**: `Mono`/`Flux`, RouterFunction handlers, R2DBC
- **Hexagonal**: Domain isolated; infrastructure implements ports
- **Script sandbox**: GraalJS with no host/IO/thread access

## Testing

```bash
./gradlew test              # All tests
./gradlew jacocoTestReport  # Coverage report
```

## Documentation

- **English**: this file
- **Español**: [HELP.md](./HELP.md) — detailed architecture, API, testing, extension guide

## License

[MIT](./LICENSE)