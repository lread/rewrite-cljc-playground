#!/usr/bin/env bash
# This script is run by continuous integration server

set -eou pipefail

# Clean out all caches
rm -rf target .cpcache .shadow-cljs

./script/lint.sh
./script/clj-tests.sh 1.9
./script/clj-tests.sh 1.10

./script/cljs-tests.sh --env node --optimizations none
./script/cljs-tests.sh --env node --optimizations advanced
./script/cljs-tests.sh --env chrome-headless --optimizations none
# TODO re-enable after I figure out https://github.com/lread/rewrite-cljc-playground/issues/28
# ./script/cljs-tests.sh --env chrome-headless --optimizations advanced

./script/shadow-cljs-test.sh

if [[ ${OSTYPE} =~ ^darwin* ]] || [ "${OSTYPE}" == "linux-gnu" ];then
    ./script/cljs-tests.sh --env planck --optimizations none
else
    echo "* WARNING: skipping planck tests, they can only run on linux or macos"
fi
