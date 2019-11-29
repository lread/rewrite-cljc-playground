#!/usr/bin/env bash

set -eou pipefail

# Depending on how GraalVM was installed an setup it can be a bit of a challenge to find native-image
# This script will make an effort and even make an attempt to install it via GraalVM gu

status-line() {
    script/status-line "$1" "$2"
}

find-graal-prog() {
    local prog_name=$1
    if (set +x; hash "$prog_name" > /dev/null 2>&1); then
        command -v "$prog_name"
    elif (set +xu; test -f "${JAVA_HOME}/bin/${prog_name}"); then
        echo "${JAVA_HOME}/bin/${prog_name}"
    elif (set +xu; test -f "${GRAALVM_HOME}/bin/${prog_name}"); then
        echo "${GRAALVM_HOME}/bin/${prog_name}"
    else
        echo "!not-found!"
    fi
}

GRAAL_NATIVE_IMAGE=$(find-graal-prog native-image)
if [ "${GRAAL_NATIVE_IMAGE}" == "!not-found!" ]; then
    GRAAL_GU=$(find-graal-prog gu)
    if [ "$GRAAL_GU" == "!not-found!" ]; then
        >&2 status-line error "did not find GraalVM native-image nor GraalVM gu to install it. Ensure progs are on PATH or set GRAALVM_HOME / JAVA_HOME"
        exit 1
    fi
    >&2 ${GRAAL_GU} install native-image
    GRAAL_NATIVE_IMAGE=$(find-graal-prog native-image)
    if [ "${GRAAL_NATIVE_IMAGE}" == "!not-found!" ]; then
        >&2 status-line error "odd, I just installed GraalVM native-image but cannot find it."
        exit 1
    fi
fi

echo "${GRAAL_NATIVE_IMAGE}"
