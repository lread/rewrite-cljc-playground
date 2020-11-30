(ns ^:no-doc rewrite-cljc.node.seq
  (:require [rewrite-cljc.interop :as interop]
            [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defrecord SeqNode [tag
                    format-string
                    wrap-length
                    seq-fn
                    children]
  node/Node
  (tag [this]
    tag)
  (printable-only? [_] false)
  (sexpr [this] (.sexpr this {}))
  (sexpr [_this opts]
    (seq-fn (node/sexprs children opts)))
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

(defn- resolve-prefix [auto-resolved? prefix {:keys [auto-resolve]}]
  (let [auto-resolve (or auto-resolve node/default-auto-resolve)]
    (cond
      (and auto-resolved? prefix)
      (auto-resolve (symbol prefix))

      auto-resolved?
      (auto-resolve :current)

      :else prefix)))

(defn- qualify-children [m auto-resolved? prefix opts]
  (let [resolved-prefix (delay (resolve-prefix auto-resolved? prefix opts))]
    (->> (map (fn [[k v]]
                [(cond (not (keyword? k)) k
                       (= (namespace k) "_") (keyword (name k))
                       (namespace k) k
                       :else (keyword (str @resolved-prefix) (name k)))
                 v])
              m)
         (into {}))))

(defn qualified-map? [n]
  (and (= :map (node/tag n))
       (or (:auto-resolved? n) (:prefix n))))

(defrecord MapNode [children auto-resolved? prefix prefix-trailing-whitespace]
  node/Node
  (tag [this] :map)
  (printable-only? [_] false)
  (sexpr [this] (.sexpr this {}))
  (sexpr [this opts]
    (let [m (apply hash-map (node/sexprs children opts))]
      (if (qualified-map? this)
        (qualify-children m auto-resolved? prefix opts)
        m)))
  (length [this]
    (+ 2
       (if (qualified-map? this) 2 0)
       (if auto-resolved? 1 0)
       (count prefix)
       (node/sum-lengths prefix-trailing-whitespace)
       (node/sum-lengths children)))
  (string [this]
    (str (when (qualified-map? this) "#:")
         (when auto-resolved? ":")
         prefix
         (node/concat-strings prefix-trailing-whitespace)
         "{"
         (node/concat-strings children)
         "}"))

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    (dec 2))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! MapNode)
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
  "Create a node representing a map with `children`.

  Optionally specify `opts` for namespaced (aka qualified) maps:
  * `{:prefix \"my-prefix\"}` - qualified
  * `{:auto-resolved? true}` - auto-resolved to current namespace
  * `{:auto-resolved? true :prefix \"ns-alias\"}` - auto-resolved to namespace with alias ns-alias

  `opts` can also specify prefix trailing whitespace nodes via `:prefix-trailing-whitespace`"
  ([children]
   (map-node children {}))
  ([children opts]
   (->MapNode children (:auto-resolved? opts) (:prefix opts) (:prefix-trailing-whitespace opts))))

(comment (reduce + '(2 nil 3)))
