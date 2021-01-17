(ns ^:no-doc rewrite-clj.node.namespaced-map
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; a map qualifier is not sexpressable on its own
(defrecord MapQualifierNode [auto-resolved? prefix]
  node/Node
  ;; TODO: tag is :token? that's probably not right!
  (tag [_n] :token)
  (node-type [_n] :map-qualifier)
  (printable-only? [_n] true)
  (sexpr [_n]
    (throw (ex-info "unsupported operation" {})))
  (sexpr [_n _opts]
    (throw (ex-info "unsupported operation" {})))
  (length [_n]
    (+ 2 ;; for #:
       (if auto-resolved? 1 0) ;; for extra :
       (count prefix)))
  (string [_n]
    (str "#:"
         (when auto-resolved? ":")
         prefix))
  Object
  (toString [n]
    (node/string n)))

(defn- edit-map-children
  "A map node's children are a list of nodes that can contain non-sexpr-able elements (ex. whitespace).

  Returns `children` with `f` applied sexpressable children.

  `f` is called with
  - `n` - node
  - `is-map-key?` true if the node is in keyword position
  and should return `n` or a new version of `n`."
  [children f]
  (loop [r children
         last-key nil
         new-children []]
    (if-let [n (first r)]
      (if (node/printable-only? n)
        (recur (rest r)
               last-key
               (conj new-children n))
        (if last-key
          (recur (rest r)
                 nil
                 (conj new-children (f n false)))
          (recur (rest r)
                 n
                 (conj new-children (f n true)))))
      new-children)))

(defn- apply-context-to-map
  "Apply the context of the qualified map to the keyword keys in the map.

  Strips context from keyword-nodes not in keyword position and adds context to keyword nodes in keyword position."
  [m-node q-node]
  (node/replace-children m-node
                         (edit-map-children (node/children m-node)
                                            (fn [n is-map-key?]
                                              (if (satisfies? node/MapQualifiable n)
                                                (if is-map-key?
                                                  (node/map-context-apply n q-node)
                                                  (node/map-context-clear n))
                                                n)))))

(defn- apply-context [children]
  (let [q-node (first children)
        m-node (last children)]
    (concat (drop-last children)
            [(apply-context-to-map m-node q-node)])))

(defn reapply-namespaced-map-context
  "Namespaced map qualifier context is automatically applied to keyword children of contained map automatically on:
  - [[node/namespaced-map-node]] creation (i.e. at parse time)
  - [[node/replace-children]]

  If you make changes outside these techniques, call this function to reapply the qualifier context.

  This is only necessary if you need `sexpr` on map keywords to reflect the namespaced map qualifier.

  Returns `n` if not a namespaced map node."
  [n]
  (if (= :namespaced-map (node/tag n))
    (node/replace-children n (apply-context (node/children n)))
    n))

(defn- namespaced-map-sexpr
  "Assumes that appropriate qualifier context has been applied to contained map."
  [children opts]
  (node/sexpr (last children) opts))

(defrecord NamespacedMapNode [children]
  node/Node
  (tag [_n] :namespaced-map)
  (node-type [_n] :namespaced-map)
  (printable-only? [_n] false)
  (sexpr [_n]
    (namespaced-map-sexpr children {}))
  (sexpr [_n opts]
    (namespaced-map-sexpr children opts))
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
    (assoc n :children (apply-context children')))
  (leader-length [_n]
    (dec 2))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! MapQualifierNode)
(node/make-printable! NamespacedMapNode)

;; ## Constructors

(defn map-qualifier-node
  "Create a map qualifier node.

  - `(map-qualifier-node false \"my-prefix\")` -> `#:my-prefix` - qualified
  - `(map-qualifier-node true \"my-ns-alias\")` -> `#::my-ns-alias` - auto-resolved namespace alias
  - `(map-qualifier-node true nil)` -> `#::` - auto-resolved current namespace

  The above are the only supported variations, use [[rewrite-clj.node/map-node]] for unqualified maps."
  [auto-resolved? prefix]
  (->MapQualifierNode auto-resolved? prefix))

(defn namespaced-map-node
  "Create a namespaced map node with `children`.

  - first child must be a map-qualifier node, see [[rewrite-clj.node/map-qualifier-node]]
  - optionally followed by whitespace node(s),
  - followed by a map node, see [[rewrite-clj.node/map-node]]"
  [children]
  (->NamespacedMapNode (apply-context children)))
