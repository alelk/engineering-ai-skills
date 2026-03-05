# Production Readiness Checklist

## Dockerfile

- [ ] Multi-stage build: builder stage and runtime stage are separate.
- [ ] Base images pinned to specific major+minor versions (no `latest`).
- [ ] Dependency resolution files copied before source code (layer caching).
- [ ] Final image uses a minimal base (`-alpine` or `-slim`).
- [ ] `.dockerignore` excludes `.git/`, `build/`, `.gradle/`, `.idea/`, `*.md`.
- [ ] No secrets baked into the image (use build-args for build-time, env vars for runtime).
- [ ] Image size verified: < 200 MB (JVM) or < 100 MB (native).

## Security

- [ ] Container runs as non-root user (`appuser`).
- [ ] No `sudo` or `setuid` binaries in the final image.
- [ ] Build-args with secrets are only in the builder stage (discarded in final image).
- [ ] OCI labels set (`org.opencontainers.image.source`, `.description`, `.licenses`).

## Health & observability

- [ ] `HEALTHCHECK` instruction present in Dockerfile.
- [ ] `/health` endpoint returns `200 OK` with JSON body.
- [ ] `/health/ready` endpoint verifies downstream dependencies (DB, cache).
- [ ] `/health/live` endpoint is a simple liveness probe.
- [ ] Structured logging configured (JSON format recommended for production).

## JVM tuning (JVM strategy)

- [ ] `-XX:+UseContainerSupport` enabled.
- [ ] `-XX:MaxRAMPercentage` set (default 75%).
- [ ] GC algorithm chosen (`G1GC` for general purpose, `ZGC` for low-latency).
- [ ] `JAVA_OPTS` exposed as an environment variable for runtime override.

## GraalVM native (native strategy)

- [ ] `native-image` build completes without errors.
- [ ] Reflection configuration provided for all reflection-heavy libraries.
- [ ] Serialisation works correctly (test with actual requests).
- [ ] Binary runs on the target architecture (linux/amd64).

## Gradle build

- [ ] Shadow JAR / fat JAR plugin configured.
- [ ] `mergeServiceFiles` enabled for SPI-based libraries (Flyway, Ktor, SLF4J).
- [ ] `mainClass` set correctly.
- [ ] `app.version` wired as project version source of truth.
- [ ] `build` task depends on `shadowJar` (or equivalent).

## docker-compose (local development)

- [ ] App service depends on DB with `condition: service_healthy`.
- [ ] DB has a `healthcheck` with `pg_isready` (or equivalent).
- [ ] Named volume used for DB data persistence.
- [ ] Bridge network isolates services.
- [ ] Environment variables follow 12-factor conventions.
- [ ] `restart: unless-stopped` set for resilience.
- [ ] Ports do not conflict with host services.

## Integration test

- [ ] `docker compose up --build -d` starts without errors.
- [ ] `curl http://localhost:8080/health` returns 200.
- [ ] `docker compose exec app whoami` returns `appuser`.
- [ ] `docker compose down && docker compose up -d` (restart) works cleanly.
- [ ] Logs are readable: `docker compose logs app`.

