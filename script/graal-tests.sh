#!/usr/bin/env bash

set -eoux pipefail

NATIVE_IMAGE_XMX="16g"

echo "--[running Clojure tests natively compiled via GraalVM]--"

# support both sdkman installation (graalvm on path) and simple download (GRAALVM_HOME set)
if (set +xu; test -n "${GRAALVM_HOME}"); then
    echo "GRAALVM_HOME is: ${GRAALVM_HOME}"
elif (set +x; hash gu > /dev/null 2>&1); then
    GRAALVM_HOME=$(dirname "$(dirname "$(command -v gu)")")
    echo "assuming GRAALVM_HOME is: ${GRAALVM_HOME}"
else
    echo "* error: either set GRAALVM_HOME or ensure it is on path"
    exit 1
fi

NATIVE_IMAGE="$GRAALVM_HOME/bin/native-image"
if ! [ -x "${NATIVE_IMAGE}" ]; then
    "$GRAALVM_HOME/bin/gu" install native-image
fi

./script/graal-deps.sh

TARGET_EXE=target/native-test-runner

TARGET_RUNNER_DIR=target/clj-graal/generated
ALIAS="-A:graal:test-common"

rm -rf .cpcache
rm -rf classes
mkdir -p classes
mkdir -p target

echo "--generate test runner--"
rm -rf "${TARGET_RUNNER_DIR}"
mkdir -p "${TARGET_RUNNER_DIR}"
clojure "${ALIAS}" \
        -m clj-graal.gen-by-discovery-test-runner "${TARGET_RUNNER_DIR}"

echo "--aot compile tests--"
java -cp "$(clojure ${ALIAS} -Spath):${TARGET_RUNNER_DIR}" \
     clojure.main \
     -e "(compile 'clj-graal.test-runner)"

echo "--native compile tests--"
rm -f "${TARGET_EXE}"

# an array for args makes fiddling with args easier (can comment out a line during testing)
native_image_args=(
    "-H:Name=${TARGET_EXE}"
    --no-server
    --no-fallback
    -cp "$(clojure ${ALIAS} -Spath):classes"
    --initialize-at-build-time
    --report-unsupported-elements-at-runtime
    -H:+ReportExceptionStackTraces
    --verbose
#    -H:+PrintAnalysisCallTree
    "-J-Xmx${NATIVE_IMAGE_XMX}"
    clj_graal.test_runner
)

if [[ "$OSTYPE" == "darwin"* ]]; then
    TIME_CMD="command time -l"
else
    TIME_CMD="command time -v"
fi

${TIME_CMD} ${NATIVE_IMAGE} "${native_image_args[@]}"
echo "--running tests compiled under graal--"

ls -lh ${TARGET_EXE}

${TARGET_EXE}
