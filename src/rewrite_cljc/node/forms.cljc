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
  (tag [_n]
    :forms)
  (printable-only? [_n]
    false)
  (sexpr [_n]
    (forms-sexpr children {}))
  (sexpr [_n opts]
    (forms-sexpr children opts))
  (length [_n]
    (node/sum-lengths children))
  (string [_n]
    (node/concat-strings children))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [n children']
    (assoc n :children children'))
  (leader-length [_n]
    0)

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! FormsNode)

;; ## Constructor

(defn forms-node
  "Create top-level node wrapping multiple `children`
   (equivalent to an implicit `do` at the top-level)."
  [children]
  (->FormsNode children))
