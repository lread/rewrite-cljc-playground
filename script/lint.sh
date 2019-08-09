#!/usr/bin/env bash

# lint using clj-kondo
# takes care of creating clj-kondo caches if they don't already exist
# if you want to force refreshing your clj-kondo caches, delete .clj-kondo/.cache before
# running this script.

set -eou pipefail

function lint() {
    local lint_args
    if [ ! -d .clj-kondo/.cache ]; then
        echo "--[linting and building cache]--"
        # classpath with tests paths
        local classpath="$(clojure -R:test -C:test -Spath) script"
        lint_args="$classpath --cache"
    else
        echo "--[linting]--"
        lint_args="src test script"
    fi
    set +e
    clojure -A:clj-kondo --lint ${lint_args}
    local exit_code=$?
    set -e
    if [ ${exit_code} -ne 0 ] && [ ${exit_code} -ne 2 ] && [ ${exit_code} -ne 3 ]; then
        echo "** clj-kondo exited with unexpected exit code: ${exit_code}"
    fi
    exit ${exit_code}
}

lint
