#!/usr/bin/env bash

set -eou pipefail

is-installed() {
    (set +e; mvn -q dependency:get -Dartifact=org.clojure:clojure:1.10.1-patch_38bafca9_clj_1472_3 -o)
}

echo "--checking if patched clojure installed--"
if is-installed; then
   echo "- patched clojure found"
else
    echo "- patched clojure not found - installing"
    bash <(curl -s https://raw.githubusercontent.com/lread/clj-graal-docs/5009e2302fb98bf71f03298e6aa08c682d996a24/CLJ-1472/build-clojure-with-1472-patch.sh)
fi
