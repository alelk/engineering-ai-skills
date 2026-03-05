---
name: gradle-release-system
skillVersion: 2.1.0
owners:
  - platform
  - devops
scope:
  - gradle
  - github-actions
  - semantic-release
  - docker
projectTypes:
  - backend-library
  - frontend-webapp
  - docker-app
---

# Gradle Release System Skill

## Goal

Set up a production-grade release pipeline for Gradle projects based on
**Conventional Commits** and **semantic-release**.

## Supported project profiles

| Profile | Description | Artifact |
|---------|-------------|----------|
| `backend-library` | JVM/KMP library published to a package registry | Maven package (e.g. GitHub Packages) |
| `frontend-webapp` | Web application with release asset upload | Archive attached to GitHub Release |
| `docker-app` | Containerised application with Docker image | OCI image pushed to GHCR |

## What this skill configures

- Branch-based releases (`main`) and prereleases (`next` → `-rc.N`).
- Changelog generation (`CHANGELOG.md`).
- Version handoff through `app.version` file.
- Release publication — profile-specific:
  - **backend-library** — package publication (e.g. GitHub Packages).
  - **frontend-webapp** — release attachment upload to GitHub Releases.
  - **docker-app** — Docker image build & push to GitHub Container Registry (GHCR).
- Test-result reporting via `mikepenz/action-junit-report`.
- (docker-app) Two-stage pipeline: CI builds artifacts → docker-publish uses pre-built artifacts.

## Required files in a target project

All profiles share:

| File | Purpose |
|------|---------|
| `.github/workflows/ci.yml` | Build, test, semantic-release |
| `.releaserc.yaml` | semantic-release configuration |
| `prepare.sh` | Called by `verifyReleaseCmd` — writes `app.version` |
| `publish.sh` | Called by `publishCmd` — publishes artifacts |
| `app.version` | Single source of truth for project version |
| `build.gradle.kts` | Version wired to `app.version` (see shared snippet) |

Additional files per profile:

| Profile | Extra files |
|---------|-------------|
| `docker-app` | `Dockerfile`, `Dockerfile.ci`, `.github/workflows/docker-publish.yml` |
| `frontend-webapp` | `.github/workflows/deploy.yml` (stub) |

## Inputs

| Parameter | Default | Notes |
|-----------|---------|-------|
| `PROJECT_TYPE` | — | `backend-library`, `frontend-webapp`, or `docker-app` |
| `RELEASE_BRANCH` | `main` | |
| `PRERELEASE_BRANCH` | `next` | |
| `VERSION_FILE` | `app.version` | |
| `JAVA_VERSION` | `21` | |
| `NODE_VERSION` | `20` | For semantic-release |
| `ARTIFACT_PATH` | — | Frontend: archive path |
| `GRADLE_PUBLISH_TASK` | — | Backend: publish task name |
| `DOCKER_REGISTRY` | `ghcr.io` | Docker-app only |
| `DOCKER_IMAGE_NAME` | `${{ github.repository }}` | Docker-app only |

## Process

1. **Determine version (dry-run)** — on release branches only. Runs `semantic-release --dry-run`
   which triggers `verifyReleaseCmd` → `prepare.sh` → writes correct version to `app.version`.
   This ensures Gradle builds artifacts with the final release version.
2. **Build & Test** — on every push and PR. On release branches, the version from step 1
   is baked into the JAR and other artifacts.
3. **Publish Test Results** — upload JUnit XML reports.
4. **Semantic Release** — on push to release branches only:
   a. `verifyReleaseCmd` writes `.nextRelease.txt`, then runs `prepare.sh`.
   b. `publishCmd` runs `publish.sh`.
   c. `@semantic-release/github` creates GitHub Release and **attaches build artifacts**
      (JAR, JS archive, etc.) listed in the `assets` config.
   d. `@semantic-release/git` commits `CHANGELOG.md` and `app.version` back.
5. *(docker-app)* **Docker Build & Publish** — separate `workflow_dispatch` workflow.
   Downloads pre-built artifacts from GitHub Release → builds runtime-only Docker image
   via `Dockerfile.ci` → pushes to GHCR.

## Deliverables

Use templates from:

- `templates/backend-library/` — library CI + publish scripts.
- `templates/frontend-webapp/` — webapp CI + deploy stub.
- `templates/docker-app/` — Docker-aware CI + docker-publish workflow + Dockerfile.
- `templates/shared/` — common files for all profiles.

## Safety rules

- **Never publish from PR jobs.** Release steps run only on push to release branches.
- Use `fetch-depth: 0` so semantic-release can read full git history.
- Keep workflow branch filters and `.releaserc.yaml` branches aligned.
- Require explicit confirmation before running actual publish commands manually.
- *(docker-app)* Pass registry credentials only via `secrets.*`, never hard-code.
- *(docker-app)* Run containers as a non-root user.
- *(docker-app)* Use Docker layer caching (`cache-from: type=gha`) for faster builds.

## Known issues and lessons learned

### semantic-release dry-run for version resolution

- `semantic-release --dry-run` executes `verifyReleaseCmd` (which calls `prepare.sh`),
  but does NOT execute `publishCmd`, `@semantic-release/git`, or `@semantic-release/github`.
- Use `|| true` after the dry-run — it exits with non-zero if there is no release pending.
- The full (non-dry-run) run later will re-execute `verifyReleaseCmd` and `prepare.sh`,
  then proceed with publish, git commit, and GitHub Release creation.

### Two-stage Docker publish (CI → docker-publish)

- Building Docker images in CI is expensive (10–20 min for KMP projects).
- Better approach: CI builds artifacts once and attaches them to the GitHub Release.
  The `docker-publish` workflow downloads artifacts and builds runtime-only images
  via `Dockerfile.ci` — this takes seconds, not minutes.
- Use `@semantic-release/github` `assets` config to attach build artifacts.
- `docker-publish` accepts `release_tag` input and uses `gh release download`.

### Compose Multiplatform JS

- The correct Gradle task for production JS distribution is `jsBrowserDistribution`
  (not `jsBrowserProductionWebpack`). The former produces a complete dist at
  `build/dist/js/productionExecutable/` including index.html, resources, and Skiko WASM.

## Done criteria

- [ ] Merge to `main` produces a proper release.
- [ ] Merge to `next` produces an `-rc` prerelease.
- [ ] `CHANGELOG.md` and `app.version` are updated and committed.
- [ ] Artifacts are attached to the GitHub Release (JAR, JS archive).
- [ ] *(docker-app)* Docker image is tagged with version and user-specified tag.
- [ ] *(docker-app)* Image is accessible in GHCR.
- [ ] *(docker-app)* `docker-publish` workflow uses pre-built artifacts (no Gradle rebuild).
