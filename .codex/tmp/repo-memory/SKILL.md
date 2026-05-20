---
name: repo-memory
description: Persist, reuse, and incrementally refresh a repo-specific memory for any software repository. Use when Codex works in an unfamiliar or evolving codebase and should avoid re-reading the whole project on every request, or when comparing the saved memory against current file changes and updating that memory from Git diff or a filesystem snapshot.
---

# Repo Memory

## Overview

Reuse an explicit on-disk project memory instead of relying on hidden model state.
Treat `.codex/project-memory/` in the current repository as the source of truth for remembered context.

Read [references/bootstrap-guidelines.md](references/bootstrap-guidelines.md) when bootstrapping or when the saved memory looks stale.
Read [references/memory-artifacts.md](references/memory-artifacts.md) when you need the meaning of each generated file.

## Workflow

### 1. Load the saved memory first

Start by reading:

- `.codex/project-memory/project-memory.md`
- `.codex/project-memory/last-delta.json` if it exists

If those files exist and the delta reports no relevant changes, answer from that saved context instead of re-reading the repo.

### 2. Detect what changed before deeper analysis

Run the bundled script from this skill directory:

```powershell
python <skill-dir>/scripts/sync_memory.py delta --repo-root .
```

Use the delta output to decide what to read:

- If `changed_files` is empty, trust the saved memory.
- If files changed, read only the changed files plus directly impacted neighbors.
- Prefer `git diff` information when Git is available.
- Fall back to the snapshot comparison when Git metadata is incomplete.

### 3. Bootstrap the memory if it does not exist

If `.codex/project-memory/project-memory.md` is missing:

1. Read the bootstrap sources listed in [references/bootstrap-guidelines.md](references/bootstrap-guidelines.md).
2. Read only the key implementation files needed to understand architecture and active flows.
3. Create `.codex/project-memory/project-memory.md` with:
   - project identity and runtime constraints
   - stack and dependency signals
   - package or module map
   - entry points and main flows
   - interfaces such as APIs, CLIs, jobs, queues, or UI routes
   - known risks, caveats, and active work
4. Save the baseline snapshot:

```powershell
python <skill-dir>/scripts/sync_memory.py save --repo-root .
```

### 4. Refresh the memory after analysis or edits

After you finish reading changed files or making edits:

1. Update `.codex/project-memory/project-memory.md` only in the impacted sections.
2. Keep the summary concrete and repository-specific.
3. Save the new baseline:

```powershell
python <skill-dir>/scripts/sync_memory.py save --repo-root .
```

Do not claim the memory was updated unless both the markdown summary and the baseline snapshot were refreshed.

## Memory Rules

- Treat the saved memory as an optimization, not as unquestionable truth.
- Prefer exact file paths, package names, module names, endpoints, commands, and configuration keys.
- Record only durable architectural facts and active caveats.
- Do not dump large code excerpts into the memory file.
- Do not re-read the whole repo unless the delta is broad enough that targeted reading is no longer reliable.
- When a user asks about a file listed in the delta, read that file even if the saved memory already mentions it.
- When the repository has no memory file yet, bootstrap it before giving deep architectural answers.

## Resources

### `scripts/sync_memory.py`

Compute a repo snapshot, compare it with the saved baseline, emit `last-delta.json`, and save a new baseline.

### `references/bootstrap-guidelines.md`

Guide for choosing which files to read first in common ecosystems.

### `references/memory-artifacts.md`

Reference for the memory folder layout and expected file semantics.
