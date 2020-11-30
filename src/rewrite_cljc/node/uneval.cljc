(ns ^:no-doc rewrite-cljc.node.uneval
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord UnevalNode [children]
  node/Node
  (tag [_node] :uneval)
  (printable-only? [_node] true)
  (sexpr [this] (.sexpr this {}))
  (sexpr [_node _opts]
    (throw (ex-info "unsupported operation for uneval-node" {})))
  (length [_node]
    (+ 2 (node/sum-lengths children)))
  (string [_node]
    (str "#_" (node/concat-strings children)))

  node/InnerNode
  (inner? [_node] true)
  (children [_node] children)
  (replace-children [node children']
    (node/assert-single-sexpr children')
    (assoc node :children children'))
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
