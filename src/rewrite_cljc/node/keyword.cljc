(ns ^:no-doc rewrite-cljc.node.keyword
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- resolve-nsmap-prefix [nsmap-auto-resolved? nsmap-prefix auto-resolve-fn]
  (cond
    (and nsmap-auto-resolved? nsmap-prefix)
    (auto-resolve-fn (symbol nsmap-prefix))

    nsmap-auto-resolved?
    (auto-resolve-fn :current)

    :else nsmap-prefix))

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

(defn- keyword-sexpr-in-nsmap-context [resolved-k nsmap-autoresolved? nsmap-prefix auto-resolve-fn]
  (if (bare-nsmap-keyword? resolved-k)
    (keyword (name resolved-k))
    (if (qualified-keyword? resolved-k)
      resolved-k
      (keyword
       (str (resolve-nsmap-prefix nsmap-autoresolved? nsmap-prefix auto-resolve-fn))
       (name resolved-k)))))

(defn- keyword-sexpr [k auto-resolved? nsmap-autoresolved? nsmap-prefix {:keys [auto-resolve]}]
  (let [auto-resolve-fn (or auto-resolve node/default-auto-resolve)
        resolved-k (keyword-sexpr-in-isolation k auto-resolved? auto-resolve-fn)]
    (if (or nsmap-autoresolved? nsmap-prefix)
      (keyword-sexpr-in-nsmap-context resolved-k
                                      nsmap-autoresolved?
                                      nsmap-prefix
                                      auto-resolve-fn)
      resolved-k)))

(comment
  ;; usage seems obtuse to me! can I make it easier to reason about?
  (keyword-sexpr :boo false false nil nil)
  (keyword-sexpr :boo true false nil nil)
  (keyword-sexpr :moo/boo true false nil nil)
  (keyword-sexpr :moo/boo false false nil nil)

  (keyword-sexpr :_/boo false false nil nil)
  (keyword-sexpr :_/boo false false 'my.current.ns nil)
  (keyword-sexpr :boo false false 'my.current.ns nil)
  (keyword-sexpr :boo false true nil nil)
  (keyword-sexpr :boo false true 'myalias nil)
  (keyword-sexpr :moo/boo false true 'myalias nil)
  )

(defrecord KeywordNode [k auto-resolved? nsmap-autoresolved? nsmap-prefix]
  node/Node
  (tag [_this] :token)
  (printable-only? [_] false)
  (sexpr [this]
    (keyword-sexpr k auto-resolved? nsmap-autoresolved? nsmap-prefix {}))
  (sexpr [_this opts]
    (keyword-sexpr k auto-resolved? nsmap-autoresolved? nsmap-prefix opts))
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
  (->KeywordNode k auto-resolved? nil nil))
