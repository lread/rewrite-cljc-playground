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
  (tag [_n] tag)
  (node-type [_n] :meta)
  (printable-only? [_] false)
  (sexpr [_n]
    (meta-sexpr children {}))
  (sexpr [_n opts]
    (meta-sexpr children opts))
  (length [_n]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_n]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_n] true)
  (children [_n] children)
  (replace-children [n children']
    (node/assert-sexpr-count children' 2)
    (assoc n :children children'))
  (leader-length [_n]
    (count prefix))

  Object
  (toString [n]
    (node/string n)))

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
