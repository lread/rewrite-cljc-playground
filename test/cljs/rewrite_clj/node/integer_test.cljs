(ns rewrite-clj.node.integer-test
  (:require [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test :refer [deftest is are testing run-tests]]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.generators :as g]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.edn :as edn]))


(defspec t-all-integer-nodes-produce-readable-strings 100
  (prop/for-all [node g/integer-node]
                (edn/read-string (node/string node))))
