# Bootstrap Guidelines

Use this guide only when `.codex/project-memory/project-memory.md` is missing or clearly stale.

## Always Check First

Read whatever exists among:

- `README.md`
- `docs/`
- `.env.example`
- `Dockerfile`
- `docker-compose.yml`
- `.github/workflows/`
- `.gitignore`

Then read the main build or dependency files for the current ecosystem.

## Java And JVM

Read whichever exist:

- `pom.xml`
- `build.gradle`
- `build.gradle.kts`
- `settings.gradle`
- `settings.gradle.kts`
- `gradle.properties`
- `src/main/resources/`

Then inspect:

- application entry points
- framework configuration
- controllers, services, repositories
- test entry points when architecture is unclear

## JavaScript And TypeScript

Read whichever exist:

- `package.json`
- `package-lock.json`
- `pnpm-lock.yaml`
- `yarn.lock`
- `tsconfig.json`
- `vite.config.*`
- `next.config.*`
- `nuxt.config.*`

Then inspect:

- app entry points
- route definitions
- shared state or API clients
- server bootstrap files

## Python

Read whichever exist:

- `pyproject.toml`
- `requirements.txt`
- `requirements-dev.txt`
- `poetry.lock`
- `Pipfile`
- `Pipfile.lock`
- `setup.py`

Then inspect:

- package entry points
- framework bootstrap such as Django, FastAPI, Flask, Celery
- routers, services, models, tasks

## Go

Read whichever exist:

- `go.mod`
- `go.sum`
- `Makefile`

Then inspect:

- `cmd/`
- main packages
- internal services and handlers

## Rust

Read whichever exist:

- `Cargo.toml`
- `Cargo.lock`

Then inspect:

- `src/main.rs`
- `src/lib.rs`
- module boundaries
- binary targets

## PHP

Read whichever exist:

- `composer.json`
- `composer.lock`
- framework bootstrap files

Then inspect:

- routes
- controllers
- services
- models

## What To Put In `project-memory.md`

Capture:

- project identity
- stack and versions when visible
- runtime prerequisites
- major modules
- request or event flow
- persistence or integration points
- tests strategy if visible
- active caveats

Do not copy full code blocks into memory.
