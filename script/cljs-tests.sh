#!/usr/bin/env bash

set -eou pipefail

function usage() {
    echo "Usage: $0 <options>"
    echo ""
    echo "-e --env           JavaScript environment, specify one of:"
    echo "                    node            (default)"
    echo "                    chrome-headless"
    echo "                    planck"
    echo "-o --optimizations clojurescript compiler optimizations, specify one of:"
    echo "                    none            (default)"
    echo "                    advanced"
    echo "-h --help          print usage and exit"
    echo ""
}

DEP_ALIASES=:test-common:cljs-test
# default options
TEST_ENV=node
CLJS_OPTIMIZATIONS=none

while [[ "$#" -gt 0 ]]
do case $1 in
       -e|--env) TEST_ENV="$2"; shift;;
       -o|--optimizations) CLJS_OPTIMIZATIONS="$2"; shift;;
       -h|--help) usage; exit 0;;
       *) echo -e "* invalid option: $1\n"; usage; exit 1;;
   esac; shift; done

if [[ ! "${TEST_ENV}" =~ ^(node|chrome-headless|planck)$ ]]; then
    echo -e "* invalid env: ${TEST_ENV}\n"
    usage
    exit 1
fi

if [[ ! "${CLJS_OPTIMIZATIONS}" =~ ^(none|advanced)$ ]]; then
    echo -e "* invalid optimizations: ${CLJS_OPTIMIZATIONS}\n"
    usage
    exit 1
fi

echo "--[testing ClojureScript source under ${TEST_ENV}, cljs optimizations: ${CLJS_OPTIMIZATIONS}]--"
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

clojure -A${DEP_ALIASES} \
        --out ${OUT_DIR} \
        --env ${TEST_ENV} \
        --compile-opts ${CLJS_OPTS_FILENAME} \
        --doo-opts ${DOO_OPTS_FILENAME}
