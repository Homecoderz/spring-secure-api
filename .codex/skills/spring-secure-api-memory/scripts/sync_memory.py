from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


IGNORED_DIR_NAMES = {".git", ".idea", "target"}
IGNORED_RELATIVE_PREFIXES = (".codex/project-memory/",)
TRACKED_SUFFIXES = {
    ".cmd",
    ".java",
    ".json",
    ".md",
    ".properties",
    ".ps1",
    ".sh",
    ".sql",
    ".txt",
    ".xml",
    ".yaml",
    ".yml",
}
TRACKED_FILENAMES = {
    ".gitattributes",
    ".gitignore",
    "Dockerfile",
    "mvnw",
    "mvnw.cmd",
    "pom.xml",
}


@dataclass
class Paths:
    repo_root: Path
    memory_dir: Path
    project_memory: Path
    state_file: Path
    delta_file: Path


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def should_track(rel_path: str) -> bool:
    if any(rel_path.startswith(prefix) for prefix in IGNORED_RELATIVE_PREFIXES):
        return False

    parts = rel_path.split("/")
    if any(part in IGNORED_DIR_NAMES for part in parts[:-1]):
        return False

    path = Path(rel_path)
    return path.name in TRACKED_FILENAMES or path.suffix.lower() in TRACKED_SUFFIXES


def build_snapshot(repo_root: Path) -> dict[str, dict[str, int | str]]:
    files: dict[str, dict[str, int | str]] = {}

    for path in repo_root.rglob("*"):
        if not path.is_file():
            continue

        rel_path = path.relative_to(repo_root).as_posix()
        if not should_track(rel_path):
            continue

        stat = path.stat()
        files[rel_path] = {
            "sha256": sha256_file(path),
            "size": stat.st_size,
            "mtime_ns": stat.st_mtime_ns,
        }

    return files


def run_git(repo_root: Path, *args: str) -> tuple[bool, str]:
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=repo_root,
            capture_output=True,
            text=True,
            check=False,
        )
    except FileNotFoundError:
        return False, "git executable not found"

    if result.returncode != 0:
        return False, (result.stderr or result.stdout).strip()
    return True, result.stdout.strip()


def git_metadata(repo_root: Path) -> dict[str, object]:
    inside, _ = run_git(repo_root, "rev-parse", "--is-inside-work-tree")
    if not inside:
        return {"available": False}

    head_ok, head = run_git(repo_root, "rev-parse", "HEAD")
    status_ok, status = run_git(repo_root, "status", "--short", "--untracked-files=all")
    diff_ok, diff = run_git(repo_root, "diff", "--name-status", "HEAD", "--")

    status_lines = status.splitlines() if status_ok and status else []
    diff_lines = diff.splitlines() if diff_ok and diff else []

    return {
        "available": True,
        "head": head if head_ok else None,
        "status": status_lines,
        "diff_name_status": diff_lines,
    }


def load_json(path: Path) -> dict | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def compare_snapshots(previous: dict[str, dict[str, int | str]] | None, current: dict[str, dict[str, int | str]]) -> dict[str, list[str] | int]:
    previous = previous or {}

    previous_paths = set(previous)
    current_paths = set(current)

    added = sorted(current_paths - previous_paths)
    removed = sorted(previous_paths - current_paths)
    modified = sorted(
        path
        for path in (current_paths & previous_paths)
        if current[path]["sha256"] != previous[path]["sha256"]
    )

    return {
        "added": added,
        "removed": removed,
        "modified": modified,
        "changed_count": len(added) + len(removed) + len(modified),
        "tracked_file_count": len(current),
    }


def memory_paths(repo_root: Path, memory_dir: Path | None) -> Paths:
    resolved_root = repo_root.resolve()
    resolved_memory_dir = (memory_dir or (resolved_root / ".codex" / "project-memory")).resolve()
    return Paths(
        repo_root=resolved_root,
        memory_dir=resolved_memory_dir,
        project_memory=resolved_memory_dir / "project-memory.md",
        state_file=resolved_memory_dir / "state.json",
        delta_file=resolved_memory_dir / "last-delta.json",
    )


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def command_delta(paths: Paths) -> int:
    previous_state = load_json(paths.state_file)
    current_files = build_snapshot(paths.repo_root)
    comparison = compare_snapshots(previous_state["files"] if previous_state else None, current_files)
    git_info = git_metadata(paths.repo_root)

    payload = {
        "generated_at": now_iso(),
        "repo_root": str(paths.repo_root),
        "memory_dir": str(paths.memory_dir),
        "memory_exists": paths.project_memory.exists(),
        "baseline_exists": previous_state is not None,
        "baseline_generated_at": previous_state.get("generated_at") if previous_state else None,
        "git": git_info,
        "changed_files": {
            "added": comparison["added"],
            "modified": comparison["modified"],
            "removed": comparison["removed"],
        },
        "changed_count": comparison["changed_count"],
        "tracked_file_count": comparison["tracked_file_count"],
    }

    write_json(paths.delta_file, payload)
    json.dump(payload, sys.stdout, indent=2, ensure_ascii=True)
    sys.stdout.write("\n")
    return 0


def command_save(paths: Paths) -> int:
    current_files = build_snapshot(paths.repo_root)
    payload = {
        "generated_at": now_iso(),
        "repo_root": str(paths.repo_root),
        "git": git_metadata(paths.repo_root),
        "files": current_files,
    }

    paths.memory_dir.mkdir(parents=True, exist_ok=True)
    write_json(paths.state_file, payload)
    json.dump(
        {
            "saved": True,
            "generated_at": payload["generated_at"],
            "state_file": str(paths.state_file),
            "tracked_file_count": len(current_files),
        },
        sys.stdout,
        indent=2,
        ensure_ascii=True,
    )
    sys.stdout.write("\n")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Persist and compare project memory snapshots.")
    parser.add_argument(
        "command",
        choices=("delta", "save"),
        help="Emit the current delta or save the current repo state as the baseline.",
    )
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Repository root. Defaults to the current working directory.",
    )
    parser.add_argument(
        "--memory-dir",
        default=None,
        help="Memory directory. Defaults to <repo-root>/.codex/project-memory.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    paths = memory_paths(Path(args.repo_root), Path(args.memory_dir) if args.memory_dir else None)

    if args.command == "delta":
        return command_delta(paths)
    if args.command == "save":
        return command_save(paths)

    raise ValueError(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    raise SystemExit(main())
