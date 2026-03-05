# ============================================
# Multi-stage Dockerfile — JVM fat JAR
# Optimised for minimal image size and fast builds
# ============================================
# TODO: adjust module paths, labels, EXPOSE port, and HEALTHCHECK URL.

# ---- Stage 1: Build ----
# linux/amd64 — required when Kotlin/JS or KMP modules are present.
# The Kotlin Gradle plugin downloads Node.js for the build platform;
# on ARM64 hosts (Apple Silicon) the downloaded Node binary needs glibc
# which is not available in Alpine. Using Debian-based image + amd64 fixes this.
# If the project is purely JVM (no JS/KMP), you can remove --platform and use alpine.
FROM --platform=linux/amd64 gradle:8.14-jdk21 AS builder

# Build arguments for private registry authentication (e.g. GitHub Packages)
ARG GITHUB_USER
ARG GITHUB_TOKEN

ENV GITHUB_USER=${GITHUB_USER}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

WORKDIR /build

# ── Layer 1: dependency resolution (cached) ──
# Copy only the files Gradle needs to resolve dependencies.
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY app.version ./

# TODO: list every sub-project build file for optimal caching
# COPY app/build.gradle.kts   ./app/
# COPY infra/build.gradle.kts ./infra/

RUN gradle dependencies --no-daemon || true

# ── Layer 2: source code + build ──
# TODO: list every sub-project source directory
# COPY app/src   ./app/src
# COPY infra/src ./infra/src

# Build fat JAR via Shadow plugin (merges META-INF/services for SPI)
# TODO: adjust the Gradle task to your application module
RUN gradle :app:shadowJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# OCI image labels
# TODO: update source URL, description, and license
LABEL org.opencontainers.image.source="https://github.com/OWNER/REPO"
LABEL org.opencontainers.image.description="Application Server"
LABEL org.opencontainers.image.licenses="MIT"

# Non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser  -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy fat JAR from builder
# TODO: adjust the path to your Shadow JAR output
COPY --from=builder /build/app/build/libs/app-all.jar ./app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

# Health check — Docker & orchestrators use this to determine container health
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# ── JVM container-aware flags ──
# UseContainerSupport  — respect cgroup memory/CPU limits
# MaxRAMPercentage     — use 75 % of container memory for heap
# UseG1GC              — low-latency garbage collector (good default)
# UseStringDeduplication — save memory for string-heavy workloads
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

