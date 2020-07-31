(ns ^:no-doc rewrite-cljc.node.regex
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord RegexNode [pattern]
  node/Node
  (tag [_] :regex)
  (printable-only? [_] false)
  (sexpr [_] (list 're-pattern pattern))
  (length [_] 1)
  (string [_] (str "#\"" pattern "\"")))

(node/make-printable! RegexNode)

;; ## Constructor

(defn regex-node
  "Create node representing a regex with `pattern-string`"
  [pattern-string]
  (->RegexNode pattern-string))
