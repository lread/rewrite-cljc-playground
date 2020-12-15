(ns ^:no-doc rewrite-cljc.node.uneval
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- uneval-sexpr []
  (throw (ex-info "unsupported operation for uneval-node" {})) )

(defrecord UnevalNode [children]
  node/Node
  (tag [_n] :uneval)
  (node-type [_n] :uneval)
  (printable-only? [_n] true)
  (sexpr [_n]
    (uneval-sexpr))
  (sexpr [_n _opts]
    (uneval-sexpr))
  (length [_n]
    (+ 2 (node/sum-lengths children)))
  (string [_n]
    (str "#_" (node/concat-strings children)))

  node/InnerNode
  (inner? [_n] true)
  (children [_n] children)
  (replace-children [n children']
    (node/assert-single-sexpr children')
    (assoc n :children children'))
  (leader-length [_n]
    2)

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! UnevalNode)

;; ## Constructor

(defn uneval-node
  "Create node representing an uneval `#_` form with `children`."
  [children]
  (if (sequential? children)
    (do
      (node/assert-single-sexpr children)
      (->UnevalNode children))
    (recur [children])))
