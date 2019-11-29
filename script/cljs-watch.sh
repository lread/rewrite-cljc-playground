#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

status-line info "launching figwheel main clojurescript sources"
echo "after initialized, point your browser at: http://localhost:9500/figwheel-extra-main/auto-testing"
clojure -A:test-common:fig-test
