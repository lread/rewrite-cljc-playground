(ns ^:no-doc rewrite-cljc.parser.whitespace
  (:require [rewrite-cljc.node :as node]
            [rewrite-cljc.reader :as reader]))

#?(:clj (set! *warn-on-reflection* true))

(defn parse-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [#?(:cljs ^not-native reader :default reader)]
  (let [c (reader/peek reader)]
    (cond (reader/linebreak? c)
          (node/newline-node
           (reader/read-while reader reader/linebreak?))

          (reader/comma? c)
          (node/comma-node
           (reader/read-while reader reader/comma?))

          :else
          (node/whitespace-node
           (reader/read-while reader reader/space?)))))
