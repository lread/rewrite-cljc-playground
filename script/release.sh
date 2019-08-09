#!/usr/bin/env bash

# inspired by https://github.com/lambdaisland/kaocha/blob/master/bin/release

set -euo pipefail

if [[ ! -z "$(git status --porcelain)" ]]; then
    echo "Repo is not clean."
    exit 1
fi

bin/prep_release

VERSION=$(script/get-version)

echo "--[tagging git repo]--"
git tag v${VERSION}
git push --tags

echo "Deploying to clojars"
mvn deploy

echo "--[updating pom.xml in git repo]--"
git add pom.xml
git commit -m "Update versions in pom.xml"
git push

echo "--[triggering build on cljdoc]--"
GROUP_ID=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
curl -X POST -d project=${GROUP_ID}/${ARTIFACT_ID} -d version=${VERSION} https://cljdoc.org/api/request-build2
