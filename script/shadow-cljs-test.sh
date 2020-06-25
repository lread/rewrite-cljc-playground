#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

# Just one sanity test for now
status-line info "testing ClojureScript source with Shadow CLJS, node, optimizations: none"
npx shadow-cljs compile test
