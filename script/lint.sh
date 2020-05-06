#!/usr/bin/env bash

# lint using clj-kondo
# takes care of creating clj-kondo caches if they don't already exist
# if you want to force refreshing your clj-kondo caches, delete .clj-kondo/.cache before
# running this script.

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

function lint() {
    local lint_args
    if [ ! -d .clj-kondo/.cache ]; then
        status-line info "linting and building cache"
        # classpath with tests paths
        local classpath;classpath="$(clojure -R:test -C:test -Spath) script"
        lint_args="$classpath --cache"
    else
        status-line info "linting"
        lint_args="src test script"
    fi
    set +e
    # shellcheck disable=SC2086
    clojure -A:clj-kondo \
            --lint ${lint_args} \
            --config '{:output {:include-files ["^src" "^test" "^script"]}}'
    local exit_code=$?
    set -e
    if [ ${exit_code} -ne 0 ] && [ ${exit_code} -ne 2 ] && [ ${exit_code} -ne 3 ]; then
        status-line error "clj-kondo exited with unexpected exit code: ${exit_code}"
    fi
    exit ${exit_code}
}

lint
