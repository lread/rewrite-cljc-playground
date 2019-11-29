#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

status-line info "launching koacha watch on clojure sources"
clojure -A:test-common:kaocha --watch $@
