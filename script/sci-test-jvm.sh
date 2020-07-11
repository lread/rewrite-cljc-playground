#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

status-line info "Exposing rewrite-cljc API to sci"
clojure -A:sci-test-gen-publics

status-line info "Interpreting tests with sci from using JVM"
clojure -A:sci-test -m sci-test.main --file script/sci_test_runner.clj --classpath test

