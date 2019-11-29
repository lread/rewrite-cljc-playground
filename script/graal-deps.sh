#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

# constants
CLJ_GRAAL_DOCS_SHA=08c911d8fb688b259ba14c001d21277f2e62c50a

is-patch-installed() {
    (set +e; mvn --batch-mode -q dependency:get -Dartifact=org.clojure:clojure:1.10.1-patch_38bafca9_clj_1472_3 -o)
}

status-line info "validate maven installed"
mvn --version

status-line info "checking if patched clojure installed"
if is-patch-installed; then
   echo "- patched clojure found"
else
    echo "- patched clojure not found - installing"
    bash <(curl -s "https://raw.githubusercontent.com/lread/clj-graal-docs/${CLJ_GRAAL_DOCS_SHA}/CLJ-1472/build-clojure-with-1472-patch.sh")
fi
