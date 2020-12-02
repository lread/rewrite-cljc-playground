(ns ^:no-doc rewrite-cljc.node.forms
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- forms-sexpr [children opts]
  (let [es (node/sexprs children opts)]
    (if (next es)
      (list* 'do es)
      (first es))))

(defrecord FormsNode [children]
  node/Node
  (tag [_]
    :forms)
  (printable-only? [_]
    false)
  (sexpr [this]
    (forms-sexpr children {}))
  (sexpr [_this opts]
    (forms-sexpr children opts))
  (length [_]
    (node/sum-lengths children))
  (string [_]
    (node/concat-strings children))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    0)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! FormsNode)

;; ## Constructor

(defn forms-node
  "Create top-level node wrapping multiple `children`
   (equivalent to an implicit `do` at the top-level)."
  [children]
  (->FormsNode children))
