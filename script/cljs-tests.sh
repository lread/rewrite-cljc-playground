#!/usr/bin/env bash

set -eou pipefail

# TODO: figure out parms for this script
# optimization
# env
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash

if [ $# -eq 1 ]; then
    TEST_ENV=$1
else
    TEST_ENV=node
fi

if [ ! ${TEST_ENV} == "node" ] && [ ! ${TEST_ENV} == "chrome-headless" ]; then
    echo "Usage $0 <env>"
    echo ""
    echo "Where <env> is one of:"
    echo " node"
    echo " chrome-headless"
    echo "Defaults to node"
    exit 1
fi

echo "--[testing ClojureScript source under ${TEST_ENV}]--"

OUT_DIR=target/cljsbuild/test/${TEST_ENV}

mkdir -p ${OUT_DIR}
# TODO: transcribed from lein, not sure what options are really useful/needed
CLJS_OPTS_FILENAME=target/cljsbuild/test/${TEST_ENV}-cljs-opts.edn
if [ ${TEST_ENV} == "node" ]; then
    TARGET_OPT=":target :nodejs"
else
    # TODO: nil might be wrong, :browser might be correct?
    TARGET_OPT=":target nil"
fi
cat <<EOF > ${CLJS_OPTS_FILENAME}
{:warnings {:fn-deprecated false}
 ${TARGET_OPT}
 :optimizations :none
 :pretty-print true
 :output-dir "${OUT_DIR}/out"
 :output-to "${OUT_DIR}/compiled.js"
 :source-map true}
EOF


# junit reporting is shared with our ci server, is ignored for node testing
DOO_OPTS_FILENAME=target/cljsbuild/test/${TEST_ENV}-doo-opts.edn
cat <<EOF > ${DOO_OPTS_FILENAME}
{:karma {:config {"plugins" ["karma-junit-reporter"]
                  "reporters" ["progress" "junit"]
                  "junitReporter" {"outputDir" "target/out/test-results/cljs-${TEST_ENV}"}}}}
EOF

clojure -A:test-common:cljs-test \
        --out ${OUT_DIR} \
        --env ${TEST_ENV} \
        --compile-opts ${CLJS_OPTS_FILENAME} \
        --doo-opts ${DOO_OPTS_FILENAME}

# TODO: test with advanced optimization
# TODO: test self-hosted (would lumo or planck do the trick for these?)
