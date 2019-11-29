#!/usr/bin/env bash

set -eou pipefail

status-line() {
    script/status-line "$1" "$2"
}

if [ $# -eq 1 ]; then
    CLOJURE_VERSION=$1
else
    CLOJURE_VERSION=1.10
fi

if [ ! $CLOJURE_VERSION == "1.9" ] && [ ! ${CLOJURE_VERSION} == "1.10" ]; then
    status-line error "usage"
    echo "Usage $0 <clojure version>"
    echo ""
    echo "Where <clojure version> is one of:"
    echo " 1.9"
    echo " 1.10"
    echo "Defaults to 1.10"
    exit 1
fi

status-line info "testing clojure source against clojure v${CLOJURE_VERSION}"

clojure -A:test-common:kaocha:${CLOJURE_VERSION} \
        --reporter documentation \
        --plugin kaocha.plugin/junit-xml \
        --junit-xml-file target/out/test-results/clj-v${CLOJURE_VERSION}/results.xml
