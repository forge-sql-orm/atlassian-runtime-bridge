#!/usr/bin/env bash
# Point this repository at versioned hooks under .githooks/ (requires Git 2.9+).
# Invoked from Maven (initialize) on the root reactor, or manually.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

if [[ "${GIT_HOOKS_INSTALL:-}" == "0" ]]; then
  echo "install-git-hooks: GIT_HOOKS_INSTALL=0 — skipping."
  exit 0
fi

if [[ -n "${CI:-}" ]]; then
  echo "install-git-hooks: CI is set — skipping git hook registration."
  exit 0
fi

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "install-git-hooks: not a Git checkout — skipping."
  exit 0
fi

chmod +x .githooks/pre-commit 2>/dev/null || true

git config core.hooksPath .githooks

echo "Configured: git config core.hooksPath=.githooks"
echo "pre-commit will run: mvn spotless:apply && mvn clean install (root) && mvn jacoco:report (common+forge) && mvn clean install (example)"
echo "Bypass once: SKIP_HOOKS=1 git commit ..."
echo "Disable auto-install from Maven: -DinstallGitHooks.skip=true"
