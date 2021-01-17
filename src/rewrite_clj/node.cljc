(ns rewrite-clj.node
  "Create and evaluate nodes.

  All nodes represent Clojure/ClojureScript/EDN."
  ^{:added "0.4.0"}
  (:refer-clojure :exclude [string coerce])
  (:require [rewrite-clj.node.coercer]
            [rewrite-clj.node.comment]
            [rewrite-clj.node.fn]
            [rewrite-clj.node.forms]
            [rewrite-clj.node.integer]
            [rewrite-clj.node.keyword]
            [rewrite-clj.node.meta]
            [rewrite-clj.node.namespaced-map]
            [rewrite-clj.node.protocols :as np]
            [rewrite-clj.node.quote]
            [rewrite-clj.node.reader-macro]
            [rewrite-clj.node.regex]
            [rewrite-clj.node.seq]
            [rewrite-clj.node.stringz]
            [rewrite-clj.node.token]
            [rewrite-clj.node.uneval]
            [rewrite-clj.node.whitespace]
            #?(:clj [rewrite-clj.potemkin.clojure :refer [import-vars]]))
  #?(:cljs (:require-macros [rewrite-clj.potemkin.cljs :refer [import-vars]])))

#?(:clj (set! *warn-on-reflection* true))

(import-vars
 [rewrite-clj.node.protocols
  *elide-metadata*
  coerce
  children
  child-sexprs
  concat-strings
  form-meta
  inner?
  leader-length
  length
  node?
  printable-only?
  replace-children
  sexpr
  sexprs
  map-context-apply
  map-context-clear
  string
  tag]

 [rewrite-clj.node.comment
  comment-node
  comment?]

 [rewrite-clj.node.fn
  fn-node]

 [rewrite-clj.node.forms
  forms-node]

 [rewrite-clj.node.integer
  integer-node]

 [rewrite-clj.node.keyword
  keyword-node
  keyword-node?]

 [rewrite-clj.node.meta
  meta-node
  raw-meta-node]

 [rewrite-clj.node.namespaced-map
  map-qualifier-node
  namespaced-map-node]

 [rewrite-clj.node.regex
  regex-node]

 [rewrite-clj.node.reader-macro
  deref-node
  eval-node
  reader-macro-node
  var-node]

 [rewrite-clj.node.seq
  list-node
  map-node
  set-node
  vector-node]

 [rewrite-clj.node.stringz
  string-node]

 [rewrite-clj.node.quote
  quote-node
  syntax-quote-node
  unquote-node
  unquote-splicing-node]

 [rewrite-clj.node.token
  token-node
  symbol-node?]

 [rewrite-clj.node.uneval
  uneval-node]

 [rewrite-clj.node.whitespace
  comma-separated
  line-separated
  linebreak?
  newlines
  newline-node
  spaces
  whitespace-node
  whitespace?
  comma-node
  comma?
  whitespace-nodes])

;; ## Predicates

(defn whitespace-or-comment?
  "Return true when `node` represents whitespace or comment."
  [node]
  (or (rewrite-clj.node.whitespace/whitespace? node)
      (rewrite-clj.node.comment/comment? node)))

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (np/inner? node)
    (some-> (np/children node)
            (first)
            ((juxt np/tag np/sexpr)))
    (np/sexpr node)))
