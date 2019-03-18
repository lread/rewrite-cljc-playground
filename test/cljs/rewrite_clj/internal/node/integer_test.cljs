(ns rewrite-clj.internal.node.integer-test
  (:require [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test :refer [deftest is are testing run-tests]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.edn :as edn]
            [rewrite-clj.node :as node]
            [rewrite-clj.internal.node.generators :as g]))

(defspec t-all-integer-nodes-produce-readable-strings 100
  (prop/for-all [node g/integer-node]
                (edn/read-string (node/string node))))
