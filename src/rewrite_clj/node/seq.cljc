(ns ^:no-doc rewrite-clj.node.seq
  (:require [rewrite-clj.interop :as interop]
            [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- seq-sexpr [seq-fn children opts]
  (seq-fn (node/sexprs children opts)))

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [_n] tag)
  (node-type [_n] :seq)
  (printable-only? [_n] false)
  (sexpr [_n] (seq-sexpr seq-fn children {}))
  (sexpr [_n opts] (seq-sexpr seq-fn children opts))
  (length [_n]
    (+ wrap-length (node/sum-lengths children)))
  (string [_n]
    (->> (node/concat-strings children)
         (interop/simple-format format-string)))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [n children']
    (assoc n :children children'))
  (leader-length [_n]
    (dec wrap-length))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! SeqNode)

;; ## Constructors

(defn list-node
  "Create a node representing a list with `children`."
  [children]
  (->SeqNode :list "(%s)" 2 #(apply list %) children))

(defn vector-node
  "Create a node representing a vector with `children`."
  [children]
  (->SeqNode :vector "[%s]" 2 vec children))

(defn set-node
  "Create a node representing a set with `children`."
  [children]
  (->SeqNode :set "#{%s}" 3 set children))

(defn map-node
  "Create a node representing an map with `children`."
  [children]
  (->SeqNode :map "{%s}" 2 #(apply hash-map %) children))
