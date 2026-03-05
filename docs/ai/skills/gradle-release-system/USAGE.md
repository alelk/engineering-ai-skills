# Usage — Gradle Release System

## 1. Select profile

| Profile | When to use |
|---------|------------|
| `backend-library` | JVM/KMP libraries published to a package registry (e.g. GitHub Packages) |
| `frontend-webapp` | Web applications with release-asset upload to GitHub Releases |
| `docker-app` | Containerised applications with Docker image published to GHCR |

## 2. Copy templates to target project

### Common steps (all profiles)

```bash
# Copy shared files
cp templates/shared/app.version.example <target>/app.version

# Apply the build.gradle.kts snippet to the root build file
# (merge manually — see templates/shared/build.gradle.kts.snippet)
```

### Per-profile files

```bash
# backend-library
cp templates/backend-library/ci.yml        <target>/.github/workflows/ci.yml
cp templates/backend-library/prepare.sh    <target>/prepare.sh
cp templates/backend-library/publish.sh    <target>/publish.sh
cp templates/shared/.releaserc.yaml        <target>/.releaserc.yaml

# frontend-webapp
cp templates/frontend-webapp/ci.yml        <target>/.github/workflows/ci.yml
cp templates/frontend-webapp/deploy.yml    <target>/.github/workflows/deploy.yml
cp templates/frontend-webapp/prepare.sh    <target>/prepare.sh
cp templates/frontend-webapp/publish.sh    <target>/publish.sh
cp templates/shared/.releaserc.yaml        <target>/.releaserc.yaml

# docker-app
cp templates/docker-app/ci.yml             <target>/.github/workflows/ci.yml
cp templates/docker-app/docker-publish.yml <target>/.github/workflows/docker-publish.yml
cp templates/docker-app/prepare.sh         <target>/prepare.sh
cp templates/docker-app/publish.sh         <target>/publish.sh
cp templates/docker-app/Dockerfile         <target>/Dockerfile
cp templates/shared/.releaserc.yaml        <target>/.releaserc.yaml
```

## 3. Customise templates

1. **`.releaserc.yaml`** — adjust branch names if your release branch is not `main`.
2. **`ci.yml`** — set `java-version`, `node-version`, and Gradle tasks for your project.
3. **`publish.sh`** — set the actual Gradle publish task or archive path.
4. *(docker-app)* **`Dockerfile`** — adjust build stages, module paths, JDK version, and labels.
5. *(docker-app)* **`docker-publish.yml`** — adjust `IMAGE_NAME` and build-args if needed.

## 4. Make scripts executable

```bash
chmod +x prepare.sh publish.sh
```

## 5. Validate before first publish

```bash
# Run tests
./gradlew check

# Dry-run semantic-release (requires Node.js)
npx semantic-release --dry-run
```

## 6. Enable publish

- Ensure GitHub Actions workflow permissions include `contents: write` and `packages: write`.
- Merge a Conventional Commit (`feat:`, `fix:`, etc.) to `main` or `next`.
- Observe that:
  - A GitHub Release is created.
  - `CHANGELOG.md` and `app.version` are committed back.
  - Artifacts are published to the expected destination.

## Notes

- Keep branch names synchronised across workflow and `.releaserc.yaml`.
- If your default branch is `master`, update both templates.
- For frontend assets, adjust `path:` in `.releaserc.yaml` to the real artifact path.
- For Docker apps, the `docker-publish.yml` workflow is triggered manually (`workflow_dispatch`); wire it to automation as needed.
