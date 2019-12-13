#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

function usage() {
    echo "Usage: $0 <options>"
    echo ""
    echo "-e --env             JavaScript environment, specify one of:"
    echo "                      node            (default)"
    echo "                      chrome-headless"
    echo "                      planck"
    echo "-o --optimizations   ClojureScript compiler optimizations, specify one of:"
    echo "                      none            (default)"
    echo "                      advanced"
    echo "-g --run-granularity Troubleshoot by running separate runs for each..., specify one of:"
    echo "                      all (default)"
    echo "                      namespace"
    echo "                      test"
    echo "-h --help            Print usage and exit"
    echo ""
}

DEP_ALIASES=:test-common:cljs-test
# default options
TEST_ENV=node
CLJS_OPTIMIZATIONS=none
RUN_GRANULARITY=all

while [[ "$#" -gt 0 ]]
do case $1 in
       -e|--env) TEST_ENV="$2"; shift;;
       -o|--optimizations) CLJS_OPTIMIZATIONS="$2"; shift;;
       -r|--run-granularity) RUN_GRANULARITY="$2"; shift;;
       -h|--help) usage; exit 0;;
       *) status-line error "invalid option: $1"; usage; exit 1;;
   esac; shift; done

if [[ ! "${TEST_ENV}" =~ ^(node|chrome-headless|planck)$ ]]; then
    status-line error "invalid env: ${TEST_ENV}"
    usage
    exit 1
fi

if [[ ! "${CLJS_OPTIMIZATIONS}" =~ ^(none|advanced)$ ]]; then
    status-line error "invalid optimizations: ${CLJS_OPTIMIZATIONS}"
    usage
    exit 1
fi

if [[ ! "${RUN_GRANULARITY}" =~ ^(all|namespace|test)$ ]]; then
    status-line error "invalid run-granularity: ${RUN_GRANULARITY}"
    usage
    exit 1
fi

status-line info "testing ClojureScript source under ${TEST_ENV}, cljs optimizations: ${CLJS_OPTIMIZATIONS}"
TEST_COMBO=${TEST_ENV}-${CLJS_OPTIMIZATIONS}
OUT_DIR=target/cljsbuild/test/${TEST_COMBO}

mkdir -p ${OUT_DIR}
CLJS_OPTS_FILENAME=${OUT_DIR}-cljs-opts.edn
if [ ${TEST_ENV} == "node" ]; then
    TARGET=":nodejs"
else
    # TODO: nil might be wrong, :browser might be correct?
    TARGET="nil"
fi

if [ ${TEST_ENV} == "planck" ]; then
    DEP_ALIASES=${DEP_ALIASES}:planck-test
fi

if [ ${CLJS_OPTIMIZATIONS} == "none" ]; then
    PRETTY_PRINT="true"
    SOURCE_MAP="true"
else
    PRETTY_PRINT="false"
    SOURCE_MAP="false"
fi

cat <<EOF > ${CLJS_OPTS_FILENAME}
{:warnings {:fn-deprecated false}
 :target ${TARGET}
 :optimizations :${CLJS_OPTIMIZATIONS}
 :pretty-print ${PRETTY_PRINT}
 :output-dir "${OUT_DIR}/out"
 :output-to "${OUT_DIR}/compiled.js"
 :source-map ${SOURCE_MAP}}
EOF

# junit reporting is shared with our ci server, is ignored for node testing
DOO_OPTS_FILENAME=${OUT_DIR}-doo-opts.edn
cat <<EOF > ${DOO_OPTS_FILENAME}
{:karma {:config {"plugins" ["karma-junit-reporter"]
                  "reporters" ["progress" "junit"]
                  "junitReporter" {"outputDir" "target/out/test-results/cljs-${TEST_COMBO}"}}}}
EOF

case $RUN_GRANULARITY in
    all)
        status-line info "one run for entire set of tests"
        clojure -A${DEP_ALIASES} \
                --out ${OUT_DIR} \
                --env ${TEST_ENV} \
                --compile-opts ${CLJS_OPTS_FILENAME} \
                --doo-opts ${DOO_OPTS_FILENAME};;
    namespace)
        status-line info "one run for each namespace"
        NSES=$(clojure -A:test-common:code-info -m code-info.ns-lister --lang cljs find-all-namespaces)
        TOTAL_NSES=$(echo "${NSES}" | wc -w | tr -d "[:blank:]")
        NS_NDX=0
        for ns in $(clojure -A:test-common:code-info -m code-info.ns-lister --lang cljs find-all-namespaces); do
            ((NS_NDX++))
            status-line info "${NS_NDX} of ${TOTAL_NSES}) running tests for namespace: $ns"
            clojure -A${DEP_ALIASES} \
                    --namespace ${ns} \
                    --out ${OUT_DIR} \
                    --env ${TEST_ENV} \
                    --compile-opts ${CLJS_OPTS_FILENAME} \
                    --doo-opts ${DOO_OPTS_FILENAME}
        done;;
    test)
        status-line info "one run for each individual test"
        status-line error "no implemented"
        exit 32;;
esac
