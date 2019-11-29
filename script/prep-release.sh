#!/usr/bin/env bash

set -euo pipefail

status-line() {
    script/status-line "$1" "$2"
}

status-line info "Updating tag and version in pom.xml"
TAG=$(git rev-parse HEAD)
VERSION=$(script/get-version.sh)

status-line info "reflecting deps.edn to pom.xml"
clojure -Spom
status-line info "setting pom.xml tag to ${TAG}"
mvn versions:set-scm-tag -DnewTag="${TAG}" -DgenerateBackupPoms=false
status-line info "setting pom.xml version to ${VERSION}"
mvn versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
