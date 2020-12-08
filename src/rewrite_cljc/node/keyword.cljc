(ns ^:no-doc rewrite-cljc.node.keyword
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- choose-qualifier [map-qualifier kw-qualifier]
  (when (not (and map-qualifier (= "_" (:prefix kw-qualifier))))
    (or kw-qualifier map-qualifier)))

(defn kw-qualifier [k auto-resolved?]
  (when (or auto-resolved? (namespace k))
    {:auto-resolved? auto-resolved?
     :prefix (namespace k)}))

(defn keyword-sexpr [kw kw-auto-resolved? map-qualifier {:keys [auto-resolve]}]
  (let [q (choose-qualifier map-qualifier (kw-qualifier kw kw-auto-resolved?))]
    (keyword (some-> (if (:auto-resolved? q)
                       ((or auto-resolve node/default-auto-resolve)
                        (or (some-> (:prefix q) symbol)
                            :current))
                       (:prefix q))
                     str)
             (name kw))))

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
  (tag [_n] :token)
  (printable-only? [_n] false)
  (sexpr [_n]
    (keyword-sexpr k auto-resolved? map-qualifier {}))
  (sexpr [_n opts]
    (keyword-sexpr k auto-resolved? map-qualifier opts))
  (length [_n]
    (let [c (inc (count (name k)))]
      (if auto-resolved?
        (inc c)
        (if-let [nspace (namespace k)]
          (+ 1 c (count nspace))
          c))))
  (string [_n]
    (str (when auto-resolved? ":")
         (pr-str k)))

  Object
  (toString [n]
    (node/string n)))

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
