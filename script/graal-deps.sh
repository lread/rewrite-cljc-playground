#!/usr/bin/env bash

set -eou pipefail

# constants
CLJ_GRAAL_DOCS_SHA=5b25cc3b39039ddc2604907b5c2092716955cf68

is-patch-installed() {
    (set +e; mvn -q dependency:get -Dartifact=org.clojure:clojure:1.10.1-patch_38bafca9_clj_1472_3 -o)
}

echo "--validate maven installed--"
mvn --version

echo "--checking if patched clojure installed--"
if is-patch-installed; then
   echo "- patched clojure found"
else
    echo "- patched clojure not found - installing"
    bash <(curl -s "https://raw.githubusercontent.com/lread/clj-graal-docs/${CLJ_GRAAL_DOCS_SHA}/CLJ-1472/build-clojure-with-1472-patch.sh")
fi
