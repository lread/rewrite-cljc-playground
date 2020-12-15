;; TODO: The parse never parses to integer nodes
(ns ^:no-doc rewrite-cljc.node.integer
  (:require [rewrite-cljc.interop :as interop]
            [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord IntNode [value base]
  node/Node
  (tag [_n] :token)
  (node-type [_n] :int)
  (printable-only? [_n] false)
  (sexpr [_n] value)
  (sexpr [_n _opts] value)
  (length [n]
    (count (node/string n)))
  (string [_n]
    (let [sign (when (< value 0)
                 "-")
          abs-value (cond-> value (< value 0) -)
          s (interop/int->str abs-value base)
          prefix (case (long base)
                   8  "0"
                   10 ""
                   16 "0x"
                   (str base "r"))]
      (str sign prefix s)))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! IntNode)

;; ## Constructor

(defn integer-node
  "Create node representing an integer `value` in `base`.

  `base` defaults to 10."
  ([value]
   (integer-node value 10))
  ([value base]
   {:pre [(integer? value)
          (integer? base)
          (< 1 base 37)]}
   (->IntNode value base)))
