(ns ^:no-doc rewrite-cljc.node.coercer
  (:require
   #?@(:clj
       [[rewrite-cljc.node.comment]
        [rewrite-cljc.node.forms]
        [rewrite-cljc.node.integer]
        [rewrite-cljc.node.keyword]
        [rewrite-cljc.node.meta :refer [meta-node]]
        [rewrite-cljc.node.protocols :as node :refer [NodeCoerceable coerce]]
        [rewrite-cljc.node.quote]
        [rewrite-cljc.node.reader-macro :refer [reader-macro-node var-node]]
        [rewrite-cljc.node.seq :refer [vector-node list-node set-node map-node]]
        [rewrite-cljc.node.string]
        [rewrite-cljc.node.token :refer [token-node]]
        [rewrite-cljc.node.uneval]
        [rewrite-cljc.node.whitespace :as ws]]
       :cljs
       [[clojure.string :as string]
        [rewrite-cljc.node.comment :refer [CommentNode]]
        [rewrite-cljc.node.forms :refer [FormsNode]]
        [rewrite-cljc.node.integer :refer [IntNode]]
        [rewrite-cljc.node.keyword :refer [KeywordNode]]
        [rewrite-cljc.node.meta :refer [MetaNode meta-node]]
        [rewrite-cljc.node.protocols :as node :refer [NodeCoerceable coerce]]
        [rewrite-cljc.node.quote :refer [QuoteNode]]
        [rewrite-cljc.node.reader-macro :refer [ReaderNode ReaderMacroNode DerefNode reader-macro-node var-node]]
        [rewrite-cljc.node.seq :refer [SeqNode vector-node list-node set-node map-node]]
        [rewrite-cljc.node.stringz :refer [StringNode]]
        [rewrite-cljc.node.token :refer [TokenNode token-node]]
        [rewrite-cljc.node.uneval :refer [UnevalNode]]
        [rewrite-cljc.node.whitespace :refer [WhitespaceNode NewlineNode] :as ws]]))
   #?(:clj
      (:import [rewrite_cljc.node.comment CommentNode]
               [rewrite_cljc.node.forms FormsNode]
               [rewrite_cljc.node.integer IntNode]
               [rewrite_cljc.node.keyword KeywordNode]
               [rewrite_cljc.node.meta MetaNode]
               [rewrite_cljc.node.quote QuoteNode]
               [rewrite_cljc.node.reader_macro ReaderNode ReaderMacroNode DerefNode]
               [rewrite_cljc.node.seq SeqNode]
               [rewrite_cljc.node.stringz StringNode]
               [rewrite_cljc.node.token TokenNode]
               [rewrite_cljc.node.uneval UnevalNode]
               [rewrite_cljc.node.whitespace WhitespaceNode NewlineNode])))

#?(:clj (set! *warn-on-reflection* true))

;; ## Helpers

(defn node-with-meta
  [n value]
  (if #?(:clj (instance? clojure.lang.IMeta value)
         :cljs (satisfies? IWithMeta value))
    (let [mta (node/form-meta value)]
      (if (empty? mta)
        n
        (meta-node (coerce mta) n)))
    n))

(let [comma (ws/whitespace-nodes ", ")
      space (ws/whitespace-node " ")]
  (defn- map->children
    [m]
    (->> (mapcat
          (fn [[k v]]
            (list* (coerce k) space (coerce v) comma))
          m)
         (drop-last (count comma))
         (vec))))

(defn- record-node
  [m]
  (reader-macro-node
   [(token-node #?(:clj (symbol (.getName ^Class (class m)))
                   :cljs ;; this is a bit hacky, but is one way of preserving original name
                         ;; under advanced cljs optimizations
                   (let [s (pr-str m)]
                     (symbol (subs s 1 (clojure.string/index-of s "{"))))))
    (map-node (map->children m))]))

;; ## Tokens

#?(:clj
   (extend-protocol NodeCoerceable
     #?(:clj Object :cljs default)
     (coerce [v]
       (node-with-meta
        (token-node v)
        v)))
   :cljs
   (extend-protocol NodeCoerceable
     #?(:clj Object :cljs default)
     (coerce [v]
       (node-with-meta
        ;; in cljs, this is where we check for a record, in clj it happens under map handling
        ;; TODO: Check if this can't be done by coercing an IRecord instead
        (if (record? v)
          (record-node v)
          (token-node v))
        v))))

(extend-protocol NodeCoerceable
  nil
  (coerce [v]
    (token-node nil)))

;; ## Seqs

(defn- seq-node
  [f sq]
  (node-with-meta
    (->> (map coerce sq)
         (ws/space-separated)
         (vec)
         (f))
    sq))

(extend-protocol NodeCoerceable
  #?(:clj clojure.lang.IPersistentVector :cljs PersistentVector)
  (coerce [sq]
    (seq-node vector-node sq))
  #?(:clj clojure.lang.IPersistentList :cljs List)
  (coerce [sq]
    (seq-node list-node sq))
  #?(:clj clojure.lang.IPersistentSet :cljs PersistentHashSet)
  (coerce [sq]
    (seq-node set-node sq)))

#?(:cljs
   (extend-protocol NodeCoerceable
     EmptyList
     (coerce [sq]
       (seq-node list-node sq))))

;; ## Maps

#?(:clj
   (extend-protocol NodeCoerceable
     clojure.lang.IPersistentMap
     (coerce [m]
       (node-with-meta
        ;; in clj a record is a persistent map
        (if (record? m)
          (record-node m)
          (map-node (map->children m)))
        m)))
   :cljs
   (let [create-map-node (fn [m]
                           (node-with-meta
                            (map-node (map->children m))
                            m))]
     (extend-protocol NodeCoerceable
       PersistentHashMap
       (coerce [m] (create-map-node m)))
     (extend-protocol NodeCoerceable
       PersistentArrayMap
       (coerce [m] (create-map-node m)))))

;; ## Vars

(extend-protocol NodeCoerceable
  #?(:clj clojure.lang.Var :cljs Var)
  (coerce [v]
    (-> (str v)
        (subs 2)
        (symbol)
        (token-node)
        (vector)
        (var-node))))

;; ## rewrite-cljc nodes coerce to themselves

(extend-protocol NodeCoerceable
  CommentNode     (coerce [v] v)
  FormsNode       (coerce [v] v)
  IntNode         (coerce [v] v)
  KeywordNode     (coerce [v] v)
  MetaNode        (coerce [v] v)
  QuoteNode       (coerce [v] v)
  ReaderNode      (coerce [v] v)
  ReaderMacroNode (coerce [v] v)
  DerefNode       (coerce [v] v)
  StringNode      (coerce [v] v)
  UnevalNode      (coerce [v] v)
  NewlineNode     (coerce [v] v)
  SeqNode         (coerce [v] v)
  TokenNode       (coerce [v] v)
  WhitespaceNode  (coerce [v] v))
