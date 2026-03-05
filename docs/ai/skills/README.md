# AI Skills Catalog

This directory stores reusable AI skills for engineering workflows.

Each skill is a self-contained set of **documentation**, **templates**, and **checklists**
that an AI agent (or a human) can apply to a target project.

## Available skills

| Skill                                                     | Description                                                                            | Version |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------|---------|
| [`gradle-release-system`](gradle-release-system/SKILL.md) | Setup and operate release automation for Gradle projects (library, webapp, Docker app) | 2.0.0   |
| [`gradle-docker-build`](gradle-docker-build/SKILL.md)     | Build optimised Docker images for Gradle/JVM servers (JVM fat JAR or GraalVM native)   | 1.0.0   |

## Skill structure convention

```
<skill-name>/
  SKILL.md              # machine-readable metadata + goals + process
  USAGE.md              # step-by-step human/AI instructions
  checklists/           # verification checklists
  templates/            # copy-paste-ready files for target projects
    <profile>/          # one folder per project profile
    shared/             # files common to all profiles
  examples/             # (optional) reference snippets & configs
```

## How to use

1. Read `SKILL.md` to understand what the skill does.
2. Follow `USAGE.md` to apply it to a target project.
3. Run through the relevant checklist before going live.
4. Keep project-specific overrides in the target project, not here.

## Governance

- Version skill docs with semantic versioning (`skillVersion` field in SKILL.md frontmatter).
- Keep templates, checklists, and documentation in sync — change them in one PR.
- Every skill must have at least `SKILL.md`, `USAGE.md`, and one checklist.
