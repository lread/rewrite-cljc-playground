(ns rewrite-cljc.node.integer-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop #?@(:cljs [:include-macros true])]
            [clojure.tools.reader.edn :as edn]
            [rewrite-cljc.node :as node]
            [rewrite-cljc.node.generators :as g]))

(defspec t-all-integer-nodes-produce-readable-strings 100
  (prop/for-all [node g/integer-node]
                (edn/read-string (node/string node))))
