(ns ^:no-doc rewrite-cljc.node.keyword
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- resolve-nsmap-prefix [map-qualifier auto-resolve-fn]
  (cond
    (and (:auto-resolved? map-qualifier) (:prefix map-qualifier))
    (auto-resolve-fn (symbol (:prefix map-qualifier)))

    (:auto-resolved? map-qualifier)
    (auto-resolve-fn :current)

    :else (:prefix map-qualifier)))

(defn- bare-nsmap-keyword? [k]
  (= "_" (namespace k)))

(defn- keyword-sexpr-in-isolation [k auto-resolved? auto-resolve-fn]
  (if auto-resolved?
    (keyword
     (str (if-let [ns-alias (namespace k)]
            (auto-resolve-fn (symbol ns-alias))
            (auto-resolve-fn :current)))
     (name k))
    k))

(defn- keyword-sexpr-in-nsmap-context [resolved-k map-qualifier auto-resolve-fn]
  (cond
    (bare-nsmap-keyword? resolved-k)
    (keyword (name resolved-k))

    (qualified-keyword? resolved-k)
    resolved-k

    :else
    (keyword
     (str (resolve-nsmap-prefix map-qualifier auto-resolve-fn))
     (name resolved-k))))

(defn- keyword-sexpr [k auto-resolved? map-qualifier {:keys [auto-resolve]}]
  (let [auto-resolve-fn (or auto-resolve node/default-auto-resolve)
        resolved-k (keyword-sexpr-in-isolation k auto-resolved? auto-resolve-fn)]
    (if map-qualifier
      (keyword-sexpr-in-nsmap-context resolved-k
                                      map-qualifier
                                      auto-resolve-fn)
      resolved-k)))

(comment
  ;; usage seems obtuse to me! can I make it easier to reason about?
  (keyword-sexpr :boo false nil nil)
  ;; => :boo
  (keyword-sexpr :boo true nil nil)
  ;; => :user/boo
  (keyword-sexpr :moo/boo true nil nil)
  ;; => :moo-unresolved/boo
  (keyword-sexpr :moo/boo false nil nil )
  ;; => :moo/boo
  (keyword-sexpr :_/boo false nil nil )
  ;; => :_/boo
  (keyword-sexpr :_/boo false {:prefix "my.current.ns"} nil)
  ;; => :boo
  (keyword-sexpr :boo false {:prefix "my.current.ns"} nil)
  ;; => :my.current.ns/boo
  (keyword-sexpr :boo false {:auto-resolved? true} nil)
  ;; => :user/boo
  (keyword-sexpr :boo false {:auto-resolved? true :prefix "myalias"} nil)
  ;; => :myalias-unresolved/boo
  (keyword-sexpr :moo/boo false {:auto-resolved? true :prefix "myalias"} nil)
  ;; => :moo/boo
  )

(defrecord KeywordNode [k auto-resolved? map-qualifier]
  node/Node
  (tag [_this] :token)
  (printable-only? [_] false)
  (sexpr [this]
    (keyword-sexpr k auto-resolved? map-qualifier {}))
  (sexpr [_this opts]
    (keyword-sexpr k auto-resolved? map-qualifier opts))
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

;; TODO: hacky - refine this maybe add type to complement tag? type would be :keyword
(defn keyword-node? [n]
  (and (= :token (node/tag n))
       (:k n)))

;; TODO: add support for clearing nsmap qualifier context?

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
  (->KeywordNode k auto-resolved? nil))
