---
name: spring-secure-api-memory
description: Persist, reuse, and incrementally refresh a repo-specific memory for the spring-secure-api project. Use when Codex works in this repository and needs to answer questions, review code, or implement changes without re-reading the whole project, or when comparing the saved memory against current file changes and updating that memory from Git diff or a filesystem snapshot.
---

# Spring Secure API Memory

## Overview

Reuse an explicit on-disk project memory instead of relying on hidden model state.
Treat `.codex/project-memory/` as the source of truth for remembered context about this repo.

Read [references/repo-context.md](references/repo-context.md) when bootstrapping or when the saved memory looks stale.
Read [references/memory-artifacts.md](references/memory-artifacts.md) when you need the meaning of each generated file.

## Workflow

### 1. Load the saved memory first

Start by reading:

- `.codex/project-memory/project-memory.md`
- `.codex/project-memory/last-delta.json` if it exists

If those files exist and the delta reports no relevant changes, answer from that saved context instead of re-reading the repo.

### 2. Detect what changed before deeper analysis

Run:

```powershell
python .codex/skills/spring-secure-api-memory/scripts/sync_memory.py delta
```

Use the delta output to decide what to read:

- If `changed_files` is empty, trust the saved memory.
- If files changed, read only the changed files plus directly impacted neighbors.
- Prefer `git diff` information when Git is available.
- Fall back to the snapshot comparison when Git metadata is incomplete.

### 3. Bootstrap the memory if it does not exist

If `.codex/project-memory/project-memory.md` is missing:

1. Read `README.md`, `pom.xml`, `src/main/resources/application.properties`, and `references/repo-context.md`.
2. Read only the key implementation files needed to understand architecture and active flows.
3. Create `.codex/project-memory/project-memory.md` with:
   - stack and runtime constraints
   - package map
   - auth and security flow
   - endpoint surface
   - known risks or caveats
4. Save the baseline snapshot:

```powershell
python .codex/skills/spring-secure-api-memory/scripts/sync_memory.py save
```

### 4. Refresh the memory after analysis or edits

After you finish reading changed files or making edits:

1. Update `.codex/project-memory/project-memory.md` only in the impacted sections.
2. Keep the summary concrete and repo-specific.
3. Save the new baseline:

```powershell
python .codex/skills/spring-secure-api-memory/scripts/sync_memory.py save
```

Do not claim the memory was updated unless both the markdown summary and the baseline snapshot were refreshed.

## Memory Rules

- Treat the saved memory as an optimization, not as unquestionable truth.
- Prefer exact file paths, package names, endpoints, and configuration keys.
- Record only durable architectural facts and active caveats.
- Do not dump large code excerpts into the memory file.
- Do not re-read the whole repo unless the delta is broad enough that targeted reading is no longer reliable.
- When a user asks about a file listed in the delta, read that file even if the saved memory already mentions it.

## Resources

### `scripts/sync_memory.py`

Compute a repo snapshot, compare it with the saved baseline, emit `last-delta.json`, and save a new baseline.

### `references/repo-context.md`

Seed context for this specific Spring Boot project.

### `references/memory-artifacts.md`

Reference for the memory folder layout and expected file semantics.
