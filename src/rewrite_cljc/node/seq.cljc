(ns ^:no-doc rewrite-cljc.node.seq
  (:require [rewrite-cljc.interop :as interop]
            [rewrite-cljc.node.protocols :as node]))

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
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this] (seq-sexpr seq-fn children {}))
  (sexpr [this opts] (seq-sexpr seq-fn children opts))
  (length [_]
    (+ wrap-length (node/sum-lengths children)))
  (string [this]
    (->> (node/concat-strings children)
         (interop/simple-format format-string)))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    (dec wrap-length))

  Object
  (toString [this]
    (node/string this)))

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

;; TODO consider clearing any namespaced map qualifier context from children on create and reaplace for map-node
