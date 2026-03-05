# Usage — Gradle Docker Build

## 1. Choose runtime strategy

| Strategy           | Pros                                                     | Cons                                                                   |
|--------------------|----------------------------------------------------------|------------------------------------------------------------------------|
| **JVM (fat JAR)**  | Full ecosystem support, easy debugging, JIT optimisation | Larger image (~180 MB), slower cold start                              |
| **GraalVM Native** | Tiny image (~50 MB), instant startup, low memory         | Long build (~5 min), reflection config needed, limited library support |

> **Recommendation:** start with **JVM**. Switch to native only if startup time
> or memory footprint is a hard requirement.

## 2. Copy templates to the target project

### JVM strategy

```bash
cp templates/jvm/Dockerfile              <target>/Dockerfile
cp templates/shared/docker-compose.yaml  <target>/docker-compose.yaml
cp templates/shared/.dockerignore        <target>/.dockerignore
```

### GraalVM Native strategy

```bash
cp templates/native/Dockerfile.native    <target>/Dockerfile
cp templates/shared/docker-compose.yaml  <target>/docker-compose.yaml
cp templates/shared/.dockerignore        <target>/.dockerignore
```

### Apply Gradle snippets

Merge the relevant snippet into your `build.gradle.kts`:

- `templates/jvm/build.gradle.kts.snippet` — Shadow JAR configuration.
- `templates/native/build.gradle.kts.snippet` — GraalVM native-image plugin.

### Add health endpoints

Copy `examples/ktor/HealthRoutes.kt` into your transport/routes package and
wire it in the application module:

```kotlin
fun Application.module() {
    healthRoutes()
    // ... other routes
}
```

## 3. Customise for your project

1. **Dockerfile** — adjust `COPY` lines for your sub-module layout, set `LABEL` fields.
2. **docker-compose.yaml** — set environment variables, ports, DB credentials.
3. **build.gradle.kts** — set `mainClass`, adjust `mergeServiceFiles` includes.

## 4. Build & verify locally

```bash
# Build and start everything
docker compose up --build -d

# Wait for health check
sleep 10

# Verify
curl -sf http://localhost:8080/health
# → {"status":"ok","service":"my-server"}

# Check image size
docker images | grep my-app

# Verify non-root user
docker compose exec app whoami
# → appuser

# Tear down
docker compose down
```

## 5. Integrate with CI

The Dockerfile is designed to work standalone in CI:

```bash
docker build \
  --build-arg GITHUB_USER=ci \
  --build-arg GITHUB_TOKEN=${{ secrets.GITHUB_TOKEN }} \
  -t ghcr.io/owner/repo:latest .
```

See the `gradle-release-system` skill (`docker-app` profile) for full CI/CD integration
with semantic-release and GHCR publishing.

## Performance tuning

### JVM flags (set via `JAVA_OPTS` env var)

| Flag                          | Purpose                                  |
|-------------------------------|------------------------------------------|
| `-XX:+UseContainerSupport`    | Respect container memory/CPU limits      |
| `-XX:MaxRAMPercentage=75.0`   | Use 75% of container memory for heap     |
| `-XX:+UseG1GC`                | Low-latency garbage collector            |
| `-XX:+UseStringDeduplication` | Reduce memory for string-heavy apps      |
| `-XX:+OptimizeStringConcat`   | Faster string concatenation              |
| `-Xss512k`                    | Reduce thread stack size if many threads |

### GraalVM native flags

| Flag                             | Purpose                              |
|----------------------------------|--------------------------------------|
| `--enable-http`                  | Enable HTTP client support           |
| `--enable-https`                 | Enable HTTPS support                 |
| `-H:+ReportExceptionStackTraces` | Better error reporting               |
| `--initialize-at-build-time`     | Pre-initialise classes at build time |
| `-march=native`                  | Optimise for the build machine CPU   |

## Notes

- The `docker-compose.yaml` is for **local development only**. Production deployments
  should use `docker-compose.prod.yaml` or an orchestrator (Kubernetes, etc.).
- If the project uses Testcontainers for integration tests, Docker must be available
  in CI — see the `docker-app` CI template in `gradle-release-system`.
- For private dependencies (GitHub Packages), pass `GITHUB_USER` / `GITHUB_TOKEN`
  as build-args — they are only used in the builder stage and not baked into the final image.

## Kotlin/JS and Compose Multiplatform

If the project includes Kotlin/JS or Compose Multiplatform modules:

1. **Use Debian-based builder image** — `gradle:8.14-jdk21` (not `-alpine`).
   Kotlin/JS downloads Node.js which requires glibc.

2. **Add `--platform=linux/amd64`** to the builder stage if building on ARM64 hosts.

3. **Use `jsBrowserDistribution`** instead of `jsBrowserProductionWebpack`.
   The `jsBrowserDistribution` task produces a complete dist at
   `build/dist/js/productionExecutable/` with index.html, resources, and Skiko WASM.

4. **For the frontend nginx image**, remove the default nginx page:
   ```dockerfile
   RUN rm -rf /usr/share/nginx/html/*
   COPY --from=builder /build/.../build/dist/js/productionExecutable/ /usr/share/nginx/html/
   ```

## Dockerfile.ci (runtime-only)

For CI/CD pipelines, use `Dockerfile.ci` which accepts pre-built artifacts:

```bash
# Server
docker build -f Dockerfile.ci --build-arg JAR_PATH=my-app.jar -t my-server:1.0 .

# Webapp (from tar.gz)
docker build -f webapp/Dockerfile.ci --build-arg DIST_ARCHIVE=webapp.tar.gz -t my-webapp:1.0 .
```

This avoids rebuilding everything from source when publishing Docker images.

## docker-compose inline configs

Use `configs` with `content` to store server configuration directly in docker-compose:

```yaml
services:
  app:
    configs:
      - source: app-config
        target: /app/config/application.yaml
    environment:
      APP_CONFIG: /app/config/application.yaml

configs:
  app-config:
    content: |
      server:
        port: 8080
      db:
        url: "jdbc:postgresql://db:5432/myapp"
```

The application loads the config via `addFileSource(System.getenv("APP_CONFIG"))`.
Secrets are passed as env vars and resolved by the config library (e.g. Hoplite `${ENV_VAR}`).

