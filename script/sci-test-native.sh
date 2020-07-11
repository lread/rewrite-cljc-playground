#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

if [ ! -f target/sci-test-rewrite-cljc ]; then
    status-line error "native image not found, did you run script/sci_test_gen_native_image.clj yet?"
    exit 1
fi

status-line info "Interpreting tests with sci using natively compiled binary"
target/sci-test-rewrite-cljc --file script/sci_test_runner.clj --classpath test
