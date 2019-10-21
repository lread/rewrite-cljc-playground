#!/usr/bin/env bash

set -eou pipefail

echo "--[launching koacha watch on clojure sources]--"
clojure -A:test-common:kaocha --watch $@
