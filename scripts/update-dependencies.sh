#!/usr/bin/env bash
#
# update-dependencies.sh
#
# Bumps Maven dependencies, plugins, parent POMs, and version properties to
# their latest available versions across:
#   * the library reactor at the repo root (pom.xml)
#   * the example reactor under examples/atlassian-connect-forge-spring-boot-sample
#
# Driven entirely by org.codehaus.mojo:versions-maven-plugin. Default profile:
#   * latest *releases* only (no SNAPSHOTs)
#   * minor + patch jumps only (no major-version bumps, since those usually
#     require code changes)
#   * removes the *.versionsBackup files the plugin would otherwise leave
#
# Usage:
#   scripts/update-dependencies.sh                # apply updates (default)
#   scripts/update-dependencies.sh --dry-run      # report only, no file changes
#   scripts/update-dependencies.sh --allow-major  # also allow major bumps
#   scripts/update-dependencies.sh --include-snapshots
#                                                 # consider SNAPSHOTs as candidates
#   scripts/update-dependencies.sh --skip-examples
#                                                 # only the library reactor
#
# After it finishes, review with `git diff` and run a full build:
#   mvn clean install
#   mvn -f examples/atlassian-connect-forge-spring-boot-sample/pom.xml clean install
#

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_POM="${REPO_ROOT}/pom.xml"
EXAMPLES_POM="${REPO_ROOT}/examples/atlassian-connect-forge-spring-boot-sample/pom.xml"

# Pin the plugin version so the script behaves the same on every machine.
VERSIONS_PLUGIN="org.codehaus.mojo:versions-maven-plugin:2.18.0"

DRY_RUN=false
ALLOW_MAJOR=false
INCLUDE_SNAPSHOTS=false
SKIP_EXAMPLES=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true ;;
    --allow-major) ALLOW_MAJOR=true ;;
    --include-snapshots) INCLUDE_SNAPSHOTS=true ;;
    --skip-examples) SKIP_EXAMPLES=true ;;
    -h|--help)
      awk 'NR==1{next} /^[^#]/{exit} {sub(/^# ?/, ""); print}' "$0"
      exit 0
      ;;
    *)
      echo "unknown option: $1" >&2
      echo "use --help to see supported flags" >&2
      exit 1
      ;;
  esac
  shift
done

MVN_FLAGS=(-B -ntp -DgenerateBackupPoms=false)
$ALLOW_MAJOR       || MVN_FLAGS+=(-DallowMajorUpdates=false)
$INCLUDE_SNAPSHOTS || MVN_FLAGS+=(-DallowSnapshots=false)

USE_GOAL="use-latest-releases"
$INCLUDE_SNAPSHOTS && USE_GOAL="use-latest-versions"

run_for_reactor() {
  local pom="$1"
  local label="$2"

  if [[ ! -f "$pom" ]]; then
    echo "Skipping $label — $pom not found"
    return
  fi

  echo
  echo "================================================================"
  echo "  $label"
  echo "  POM: $pom"
  echo "================================================================"

  echo "--- pre-flight report: available updates ---"
  mvn -f "$pom" "${MVN_FLAGS[@]}" \
    "${VERSIONS_PLUGIN}:display-dependency-updates" \
    "${VERSIONS_PLUGIN}:display-plugin-updates" \
    "${VERSIONS_PLUGIN}:display-property-updates"

  if $DRY_RUN; then
    echo "--- --dry-run set, skipping the apply step ---"
    return
  fi

  echo "--- applying updates ---"
  mvn -f "$pom" "${MVN_FLAGS[@]}" \
    "${VERSIONS_PLUGIN}:update-parent" \
    "${VERSIONS_PLUGIN}:${USE_GOAL}" \
    "${VERSIONS_PLUGIN}:update-properties"
}

run_for_reactor "$LIB_POM" "Library reactor"

if ! $SKIP_EXAMPLES; then
  run_for_reactor "$EXAMPLES_POM" "Examples reactor"
fi

echo
echo "Done."
if $DRY_RUN; then
  echo "Dry-run only — no files were modified."
else
  echo "Review the diff with 'git diff' and rebuild before committing:"
  echo "  mvn clean install"
  if ! $SKIP_EXAMPLES; then
    echo "  mvn -f ${EXAMPLES_POM#$REPO_ROOT/} clean install"
  fi
fi
