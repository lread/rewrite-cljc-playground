(ns ^:no-doc rewrite-clj.impl.node.namespaced-map
  (:require
   [clojure.tools.reader :refer [*alias-map*]]
   [rewrite-clj.impl.node.protocols :as node]))

;; ## Node

;; TODO: avoid internal usage of sexpr
(defn- assert-namespaced-map-children
  [children]
  (let [exs (node/sexprs children)]
    (assert (= (count exs) 2)
            "can only contain 2 non-whitespace forms.")
    (assert (keyword? (first exs))
            "first form in namespaced map needs to be keyword.")
    (assert (map? (second exs))
            "second form in namespaced map needs to be map.")))

;; TODO: consider using own *alias-map* instead of reusing reader's version
;; TODO: consider allow *alias-map* to also be a function
(defn- namespace-aliases
  "We take inspiration from clojure tools reader by looking at bound *alias-map* to support
  ClojureScript. Clojure tools reader also allows this in clojure to override ns-aliases so
  we do that too."
  []
  #?(:clj (or *alias-map* (ns-aliases *ns*))
     :cljs *alias-map*))

(defn- resolve-namespace[nspace-keyword]
  (let [nspace (str nspace-keyword)
        rnspace (if (= "::" nspace)
                  (str (ns-name *ns*))
                  (if (.startsWith nspace "::")
                    (some-> (or (namespace-aliases)
                                (throw
                                 (ex-info (str ":namespaced-map could not resolve namespace alias for auto resolve " nspace-keyword
                                               #?(:cljs " - for ClojureScript you must bind an *alias-map*")) {})))
                            (get (symbol (subs nspace 2)))
                            (ns-name)
                            str)
                    (subs nspace 1)))]
    (assert rnspace (str ":namespaced-map could not resolve namespace " nspace-keyword))
    rnspace))

(defrecord NamespacedMapNode [children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    ;; TODO: avoid internal usage of sexpr
    (let [[nspace-keyword m] (node/sexprs children)]
      (->> (for [[k v] m
                 :let [k' (cond (not (keyword? k))     k
                                (= (namespace k) "_")  (keyword (name k))
                                (namespace k)          k
                                :else (keyword (resolve-namespace nspace-keyword) (name k)))]]
             [k' v])
           (into {}))))
  (length [_]
    (+ 1 (node/sum-lengths children)))
  (string [this]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_]
    children)
  (replace-children [this children']
    (assert-namespaced-map-children children')
    (assoc this :children children'))
  (leader-length [_]
    1)

  Object
  (toString [this]node/string this))

(node/make-printable! NamespacedMapNode)

;; ## Constructors

(defn namespaced-map-node
  "Create a node representing a namespaced map.
  `#:prefix{a: 1}`  prefix namespaced map
  `#::{a: 1}`       auto-resolve namespaced map
  `#::alias{a: 1}`  auto-resolve alias namespaced map
  First arg is delivered as token node with keyword, second arg is map node.
  When first arg is :: keyword can be contrived via (keyword \":\")"
  [children]
  (assert-namespaced-map-children children)
  (->NamespacedMapNode children))
