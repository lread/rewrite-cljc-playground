(ns ^:no-doc rewrite-cljc.node.meta
  (:require [rewrite-cljc.interop :as interop]
            [rewrite-cljc.node.protocols :as node]
            [rewrite-cljc.node.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- meta-sexpr [children opts]
  (let [[mta data] (node/sexprs children opts)]
    (assert (interop/meta-available? data)
            (str "cannot attach metadata to: " (pr-str data)))
    (vary-meta data merge (if (map? mta) mta {mta true}))))

(defrecord MetaNode [tag prefix children]
  node/Node
  (tag [_this] tag)
  (printable-only? [_] false)
  (sexpr [this]
    (meta-sexpr children {}))
  (sexpr [this opts]
    (meta-sexpr children opts))
  (length [_this]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_this]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_this] true)
  (children [_this] children)
  (replace-children [this children']
    (node/assert-sexpr-count children' 2)
    (assoc this :children children'))
  (leader-length [_this]
    (count prefix))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! MetaNode)

;; ## Constructor

(defn meta-node
  "Create node representing a form `data` and its `metadata`."
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta "^" children))
  ([metadata data]
   (meta-node [metadata (ws/spaces 1) data])))

(defn raw-meta-node
  "Create node representing a form `data` and its `metadata` using the
   `#^` prefix."
  ([children]
   (node/assert-sexpr-count children 2)
   (->MetaNode :meta* "#^" children))
  ([metadata data]
   (raw-meta-node [metadata (ws/spaces 1) data])))
