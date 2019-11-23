#!/usr/bin/env bash

set -eou pipefail

NATIVE_IMAGE_XMX="16g"

echo "--[check GraalVM clojure deps]--"
./script/graal-deps.sh

echo "--[running Clojure tests natively compiled via GraalVM]--"

GRAAL_NATIVE_IMAGE="$(script/graal-find-native-image.sh)"
echo "GraalVM native-image program: ${GRAAL_NATIVE_IMAGE}"

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
        -m clj-graal.gen-test-runner \
        --dest-dir "${TARGET_RUNNER_DIR}" \
        test-by-var

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

# shellcheck disable=SC2086
${TIME_CMD} \
    ${GRAAL_NATIVE_IMAGE} "${native_image_args[@]}"
echo "--running tests compiled under graal--"

ls -lh ${TARGET_EXE}

${TARGET_EXE}
