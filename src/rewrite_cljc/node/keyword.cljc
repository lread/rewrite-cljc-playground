(ns ^:no-doc rewrite-cljc.node.keyword
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- keyword-sexpr [k auto-resolved? opts]
  (if auto-resolved?
    (let [auto-resolve (or (:auto-resolve opts) node/default-auto-resolve)]
      (keyword
       (str (if-let [ns-alias (namespace k)]
              (auto-resolve (symbol ns-alias))
              (auto-resolve :current)))
       (name k)))
    k))

(defrecord KeywordNode [k auto-resolved?]
  node/Node
  (tag [_this] :token)
  (printable-only? [_] false)
  (sexpr [this]
    (keyword-sexpr k auto-resolved? {}))
  (sexpr [_this opts]
    (keyword-sexpr k auto-resolved? opts))
  (length [this]
    (let [c (inc (count (name k)))]
      (if auto-resolved?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_this]
    (str (when auto-resolved? ":")
         (pr-str k)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! KeywordNode)

;; ## Constructor

(defn keyword-node
  "Create a node representing a keyword `k`.

  Examples usages:
  * `(keyword-node :kw)` - unqualified
  * `(keyword-node :my-prefix/kw)` - qualified

  You can pass `true` for the optional `auto-resolved?` parameter:
  * `(keyword-node :kw true)` - auto-resolved to current ns, equivalent to code `::kw`
  * `(keyword-node :ns-alias/kw true)` - auto-resolved to namespace with alias ns-alias, equivalent to code `::ns-alias/kw`"
  [k & [auto-resolved?]]
  {:pre [(keyword? k)]}
  (->KeywordNode k auto-resolved?))
