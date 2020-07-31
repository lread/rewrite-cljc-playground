(ns ^:no-doc rewrite-cljc.node.uneval
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord UnevalNode [children]
  node/Node
  (tag [_] :uneval)
  (printable-only? [_] true)
  (sexpr [_]
    (throw (ex-info "unsupported operation for uneval-node" {})))
  (length [_]
    (+ 2 (node/sum-lengths children)))
  (string [_]
    (str "#_" (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (node/assert-single-sexpr children')
    (assoc this :children children'))
  (leader-length [_]
    2)

  Object
  (toString [this]
    (node/string this)))

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
