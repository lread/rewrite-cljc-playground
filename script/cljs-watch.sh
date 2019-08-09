#!/usr/bin/env bash

set -eou pipefail

echo "--[launching figwheel main clojurescript sources]--"
echo "after initialized, point your browser at: http://localhost:9500/figwheel-extra-main/auto-testing"
clojure -A:test-common:fig-test
