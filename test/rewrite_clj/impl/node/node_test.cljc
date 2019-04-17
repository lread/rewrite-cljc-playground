(ns rewrite-clj.impl.node.node-test
  (:require [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test :refer [deftest is are testing run-tests]]
            [clojure.test.check.clojure-test #?(:clj :refer :cljs :refer-macros) [defspec]]
            [clojure.test.check :refer [quick-check]]
            [rewrite-clj.node :as node]
            [rewrite-clj.impl.node.generators :as g]))

(defspec t-nodes-with-children-report-accurate-leader-lengths
  (prop/for-all [node (g/node g/container-node-types)]
                (let [node-str (node/string node)
                      children-str (apply str (map node/string (node/children node)))
                      leader (node/leader-length node)]
                  (= (subs node-str leader (+ leader (count children-str)))
                     children-str))))
