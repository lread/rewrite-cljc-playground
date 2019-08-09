#!/usr/bin/env bash

set -euo pipefail

echo "--[Updating tag and version in pom.xml]--"
TAG=$(git rev-parse HEAD)
VERSION=$(script/get-version.sh)

echo "reflecting deps.edn to pom.xml"
clojure -Spom
echo "setting pom.xml tag to ${TAG}"
mvn versions:set-scm-tag -DnewTag=${TAG} -DgenerateBackupPoms=false
echo "setting pom.xml version to ${VERSION}"
mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
