#!/usr/bin/env bash

# calculates version as number of commits since first major.minor
# if there is no major.minor yet, assume, but do not create, major.minor.0

set -eou pipefail

# Source of truth for major.minor - change them here manually when it makes sense
VERSION_MAJOR="1"
VERSION_MINOR="0"
QUALIFIER="-alpha"

function polite_grep() {
    set +e
    grep $@
    set -e
}

EARLIEST_TAG=$(git tag --sort=creatordate | polite_grep "v${VERSION_MAJOR}\.${VERSION_MINOR}\..*" | head -1)
if [ -z "${EARLIEST_TAG}" ]; then
    VERSION_PATCH="0"
else
    VERSION_PATCH=$(git rev-list ${EARLIEST_TAG}.. --count)
fi

echo "${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}${QUALIFIER}"
