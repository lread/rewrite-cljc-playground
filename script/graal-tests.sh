#!/usr/bin/env bash

set -eou pipefail

NATIVE_IMAGE_XMX="16g"

echo "--[check GraalVM clojure deps]--"
./script/graal-deps.sh

echo "--[running Clojure tests natively compiled via GraalVM]--"

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
        echo "* error: did not find GraalVM native-image nor GraalVM gu to install it."
        echo "         ensure progs are on PATH or set GRAALVM_HOME / JAVA_HOME"
        exit 1
    fi
    ${GRAAL_GU} install native-image
    GRAAL_NATIVE_IMAGE=$(find-graal-prog native-image)
    if [ "${GRAAL_NATIVE_IMAGE}" == "!not-found!" ]; then
        echo "* error: odd, I just installed GraalVM native-image but cannot find it."
        exit 1
    fi
fi
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
        -m clj-graal.gen-vars-run-directly "${TARGET_RUNNER_DIR}"

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

${TIME_CMD} ${GRAAL_NATIVE_IMAGE} "${native_image_args[@]}"
echo "--running tests compiled under graal--"

ls -lh ${TARGET_EXE}

${TARGET_EXE}
