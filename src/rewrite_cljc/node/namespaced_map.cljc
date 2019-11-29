(ns ^:no-doc rewrite-cljc.node.namespaced-map
  (:require
   [clojure.tools.reader :refer [*alias-map*]]
   [rewrite-cljc.node.protocols :as node]))

;; ## Node

(defn- assert-namespaced-map-children
  [children]
  (node/assert-sexpr-count children 2)
  (let [printables (node/without-whitespace children)]
    ;; TODO: check for keyword
    (assert (= :token (node/tag (first printables)))
            (str "first form in namespaced map needs to be a token keyword." (node/tag (first printables))))
    (assert (= :map (node/tag (second printables)))
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
  "Create a node representing a namespaced map. There are 3 types of namespaced maps:

   1. prefix namespaced map
   The prefix is a keyword which specifies to a namespace.
   Example: `#:my.name.space{:a 1}`

   2. auto-resolve alias namespaced map
   The prefix is an auto-resolve keyword specifies a namespace alias.
   Example: `#::ns-alias{:b 3}`

   3. auto-resolve namespaced map
   The prefix is `::` which specifies the current namespace.
   Example: `#::{:c 4}`

  First child is the prefix, followed by optional whitespace then map node.
  TODO: this still seems hacky to me.
  Prefix must be a token-node with a keyword value. Use (keyword ':') for auto-resolve."
  [children]
  (assert-namespaced-map-children children)
  (->NamespacedMapNode children))
