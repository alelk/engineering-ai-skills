# Release Readiness Checklist

## Branches and permissions

- [ ] Release branches defined and protected (`main`, `next`).
- [ ] `GITHUB_TOKEN` has `contents: write`.
- [ ] `packages: write` enabled if backend package publication or Docker push is used.
- [ ] `id-token: write` enabled if OIDC-based authentication is used.

## CI and semantic-release

- [ ] Workflow uses full git history (`fetch-depth: 0`).
- [ ] semantic-release runs **only** on push to release branches (not PRs).
- [ ] `.releaserc.yaml` branch config matches workflow branch filters.
- [ ] Conventional Commits policy enforced in PR review.
- [ ] Node.js and semantic-release plugins are installed before the release step.
- [ ] `semantic-release --dry-run` passes on release branch context.

## Versioning and metadata

- [ ] `app.version` file exists and is tracked in git.
- [ ] `prepare.sh` correctly writes the expected version format.
- [ ] `publish.sh` reads `.nextRelease.txt` and handles release / prerelease / snapshot.
- [ ] `CHANGELOG.md` is generated and committed back by semantic-release.

## Artifact flow — backend-library

- [ ] Gradle publish task works with release and prerelease versions.
- [ ] `-Pversion=<value>` is passed correctly in `publish.sh`.
- [ ] Published package is accessible in the target registry.

## Artifact flow — frontend-webapp

- [ ] Frontend archive path exists before `@semantic-release/github` uploads it.
- [ ] Artifacts are reproducible in a clean CI environment.

## Artifact flow — docker-app

- [ ] `Dockerfile` builds successfully (`docker build .`).
- [ ] Multi-stage build produces a minimal runtime image.
- [ ] Container runs as a non-root user.
- [ ] Health check is configured (`HEALTHCHECK` instruction).
- [ ] JVM container flags are set (`-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage`).
- [ ] `docker-publish.yml` tags image with version, SHA, and custom tag.
- [ ] Docker layer caching (`cache-from: type=gha`) is enabled.
- [ ] Image is accessible in GHCR after a successful publish.
- [ ] Build-args (e.g. `GITHUB_USER`, `GITHUB_TOKEN`) are passed for private dependencies.

## Test reporting

- [ ] JUnit XML reports are published via `mikepenz/action-junit-report`.
- [ ] Test results appear in the GitHub Actions checks tab.

## Smoke checks

- [ ] `./gradlew check` passes.
- [ ] `npx semantic-release --dry-run` passes on release branch context.
- [ ] *(docker-app)* `docker build .` completes without errors.
