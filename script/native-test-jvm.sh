#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

status-line info "Generating test runner"
clojure -A:script:test-common -m clj-graal.gen-test-runner \
        --dest-dir target/generated/graal test-by-namespace

status-line info "Executing test runner via JVM (sanity test)"
clojure -A:test-common:native-test -m clj-graal.test-runner
