#!/bin/bash
#
# release.sh — drives maven-release-plugin against the root reactor.
#
# Performs `release:prepare` followed by `release:perform`, signing the
# generated artifacts with GPG and deploying them to whatever target is
# configured in <distributionManagement> + ~/.m2/settings.xml (typically
# Artifactory or Maven Central staging).
#
# Usage:
#   ./release.sh --password <pgp passphrase>
#   ./release.sh                           # prompts for the passphrase
#
# The companion wrapper newRelease.sh bakes the passphrase in for local
# convenience; it is gitignored and must never be committed.
#
# GitHub release creation and release-notes generation are intentionally
# left out — do those manually after the artifacts are published.
#
set -e

PROPERTY_FILE=./release.properties

function help() {
  cat <<'USAGE'
Usage: release.sh [OPTIONS]
  Creates a signed release of the root reactor.
Options:
  --help                      Show this message
  --password <pgp password>   PGP passphrase used to sign release artifacts
USAGE
}

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  --password)
    password="$2"
    shift
    shift
    ;;
  --help)
    help
    exit
    ;;
  *) # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift              # past argument
    ;;
  esac
done

set -- "${POSITIONAL[@]}" # restore positional parameters

if [[ "x${password}" == "x" ]]; then
  echo "Type pgp password:"
  read -s password
  echo
fi

if [[ "x${password}" == "x" ]]; then
  echo "Password is empty"
  exit 1
fi

# prepare release (computes the next tag/version, commits the bumped poms,
# tags the repo, advances to the next SNAPSHOT, writes release.properties)
mvn clean release:prepare -Psign \
  -Darguments=-Dgpg.passphrase=${password} \
  -Dresume=false -DskipTests

# get release tag name and versions for the post-release summary
tagName=`cat $PROPERTY_FILE | grep "scm.tag" | grep -i -v -E "scm.tagNameFormat" | cut -d'=' -f2`
tagVersion=`cat $PROPERTY_FILE | grep "project.rel.com.github.vzakharchenko..atlassian-runtime-bridge"  | cut -d'=' -f2`
tagDevVersion=`cat $PROPERTY_FILE | grep "project.dev.com.github.vzakharchenko..atlassian-runtime-bridge"  | cut -d'=' -f2`

if [[ "x${tagVersion}" == "x" ]]; then
  echo "tagVersion is empty"
  exit 1
fi

if [[ "x${tagName}" == "x" ]]; then
  echo "tagName is empty"
  exit 1
fi

# perform release (checks out the tagged revision and deploys the
# already-prepared release artifacts, GPG-signed)
mvn -Psign clean release:perform \
  -Darguments=-Dgpg.passphrase=${password} \
  -DskipTests

git pull

echo
echo "Released ${tagName} (${tagVersion}); next development iteration: ${tagDevVersion}"
echo "Artifacts are now signed and deployed."
echo "Create the GitHub release for ${tagName} manually if needed."
