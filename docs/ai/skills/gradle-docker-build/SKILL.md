---
name: gradle-docker-build
skillVersion: 1.0.0
owners:
  - platform
  - backend
scope:
  - gradle
  - docker
  - ktor
  - graalvm
projectTypes:
  - ktor-server
  - spring-boot-server
  - generic-jvm-server
---

# Gradle Docker Build Skill

## Goal

Configure a Gradle-based JVM server project to produce an optimised, production-grade
Docker image and provide a local development environment via `docker-compose`.

The skill covers **two runtime strategies** — pick whichever fits the project:

| Strategy                 | Base image                          | Startup    | Memory   | When to use                                         |
|--------------------------|-------------------------------------|------------|----------|-----------------------------------------------------|
| **JVM (fat JAR)**        | `eclipse-temurin:21-jre-alpine`     | ~2–5 s     | Moderate | Most projects; full JVM ecosystem support           |
| **GraalVM Native Image** | `debian:bookworm-slim` or `scratch` | ~50–200 ms | Low      | Latency-critical services; no reflection-heavy libs |

## What this skill configures

### Build layer
- Shadow / Fat JAR plugin for single-artefact deployment.
- `mergeServiceFiles` for correct SPI behaviour (Flyway, Ktor plugins, etc.).
- `app.version` wired as the project version source of truth.

### Dockerfile
- **Multi-stage build** — builder stage compiles, runtime stage contains only the artefact.
- **Layer caching** — dependency-resolution files copied first; source code second.
- **Non-root user** (`appuser:appgroup`) for security.
- **HEALTHCHECK** instruction for orchestrator integration.
- **JVM container flags** — `UseContainerSupport`, `MaxRAMPercentage`, `UseG1GC`.
- *(native)* GraalVM `native-image` compilation in the builder stage.

### docker-compose
- Application service + PostgreSQL (or other DB) with health-check dependency.
- Named volume for DB persistence.
- Bridge network for service isolation.
- Environment-variable configuration (12-factor style).

### Health endpoints
- `/health` — basic liveness.
- `/health/ready` — readiness (can include DB ping).
- `/health/live` — liveness probe for Kubernetes / Docker HEALTHCHECK.

## Required files in a target project

| File                                  | Purpose                                    |
|---------------------------------------|--------------------------------------------|
| `Dockerfile`                          | Multi-stage image build                    |
| `docker-compose.yaml`                 | Local development environment              |
| `build.gradle.kts`                    | Shadow JAR configuration                   |
| `app.version`                         | Version source of truth                    |
| Health route (e.g. `HealthRoutes.kt`) | `/health`, `/health/ready`, `/health/live` |

Additional for GraalVM native:

| File                | Purpose                        |
|---------------------|--------------------------------|
| `Dockerfile.native` | Native-image multi-stage build |
| `build.gradle.kts`  | GraalVM plugin configuration   |

## Inputs

| Parameter          | Default              | Notes                                |
|--------------------|----------------------|--------------------------------------|
| `RUNTIME_STRATEGY` | `jvm`                | `jvm` or `native`                    |
| `JDK_VERSION`      | `21`                 |                                      |
| `GRADLE_VERSION`   | `8.11`               | Used in builder stage                |
| `APP_MODULE`       | `app`                | Gradle sub-project containing `main` |
| `MAIN_CLASS`       | —                    | Fully qualified main class           |
| `EXPOSED_PORT`     | `8080`               |                                      |
| `DB_IMAGE`         | `postgres:16-alpine` |                                      |
| `REGISTRY`         | `ghcr.io`            | For image push                       |

## Process

1. Apply `build.gradle.kts` snippet — Shadow JAR + version wiring.
2. Choose runtime strategy and copy the appropriate Dockerfile.
3. Add health endpoints to the application.
4. Create `docker-compose.yaml` for local development.
5. Verify: `docker compose up --build` starts cleanly, `/health` responds 200.

## Deliverables

| Directory           | Contents                                    |
|---------------------|---------------------------------------------|
| `templates/jvm/`    | Dockerfile, build.gradle.kts snippet        |
| `templates/native/` | Dockerfile.native, build.gradle.kts snippet |
| `templates/shared/` | docker-compose.yaml, .dockerignore          |
| `examples/ktor/`    | Health routes, application module snippet   |

## Safety rules

- Never bake secrets into the image — use build-args or runtime env vars.
- Always run as non-root in the runtime stage.
- Pin base image tags to major+minor (e.g. `temurin:21-jre-alpine`, not `latest`).
- Use `.dockerignore` to exclude `.git`, `build/`, `.gradle/`, `.idea/`.
- For native images — test thoroughly; reflection-based libraries may require explicit configuration.

## Done criteria

- [ ] `docker compose up --build` starts the app + DB without errors.
- [ ] `curl http://localhost:8080/health` returns `{"status":"ok"}`.
- [ ] Image size is under 200 MB (JVM) or under 100 MB (native).
- [ ] Container runs as non-root user.
- [ ] Docker HEALTHCHECK reports healthy within 30 s.
- [ ] `app.version` is baked into the image at build time.

