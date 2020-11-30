(ns ^:no-doc rewrite-cljc.node.token
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord TokenNode [value string-value]
  node/Node
  (tag [_this] :token)
  (printable-only? [_this] false)
  (sexpr [this] (.sexpr this {}))
  (sexpr [_this _opts] value)
  (length [_this] (count string-value))
  (string [_this] string-value)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)

;; ## Constructor

(defn token-node
  "Create node for an unspecified token of `value`."
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
  (->TokenNode value string-value)))
