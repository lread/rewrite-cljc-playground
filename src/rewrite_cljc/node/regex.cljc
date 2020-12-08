(ns ^:no-doc rewrite-cljc.node.regex
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- regex-sexpr [pattern]
  (list 're-pattern pattern))

(defrecord RegexNode [pattern]
  node/Node
  (tag [_n] :regex)
  (printable-only? [_n] false)
  (sexpr [_n]
    (regex-sexpr pattern))
  (sexpr [_n _opts]
    (regex-sexpr pattern))
  (length [_n] 1)
  (string [_n] (str "#\"" pattern "\"")))

(node/make-printable! RegexNode)

;; ## Constructor

(defn regex-node
  "Create node representing a regex with `pattern-string`"
  [pattern-string]
  (->RegexNode pattern-string))
