(ns rewrite-clj.node
  "Facade for node related namespaces."
  (:refer-clojure :exclude [string coerce])
  (:require [rewrite-clj.internal.node.coerce]
            [rewrite-clj.internal.node.comment]
            [rewrite-clj.internal.node.fn]
            [rewrite-clj.internal.node.forms]
            [rewrite-clj.internal.node.integer]
            [rewrite-clj.internal.node.keyword]
            [rewrite-clj.internal.node.meta]
            [rewrite-clj.internal.node.protocols]
            [rewrite-clj.internal.node.quote]
            [rewrite-clj.internal.node.reader-macro]
            [rewrite-clj.internal.node.regex]
            [rewrite-clj.internal.node.seq]
            [rewrite-clj.internal.node.string]
            [rewrite-clj.internal.node.token]
            [rewrite-clj.internal.node.uneval]
            [rewrite-clj.internal.node.whitespace])
  (:require-macros [rewrite-clj.internal.potemkin-cljs :refer [import-vars import-def]]))

(import-vars
 [rewrite-clj.internal.node.protocols
  coerce
  children
  child-sexprs
  concat-strings
  inner?
  leader-length
  length
  printable-only?
  replace-children
  sexpr
  sexprs
  string
  tag]

 [rewrite-clj.internal.node.comment
  comment-node
  comment?]

 [rewrite-clj.internal.node.fn
  fn-node]

 [rewrite-clj.internal.node.forms
  forms-node]

 [rewrite-clj.internal.node.integer
  integer-node]

 [rewrite-clj.internal.node.keyword
  keyword-node]

 [rewrite-clj.internal.node.meta
  meta-node
  raw-meta-node]

 [rewrite-clj.internal.node.regex
  regex-node]

 [rewrite-clj.internal.node.reader-macro
  deref-node
  eval-node
  reader-macro-node
  var-node]

 [rewrite-clj.internal.node.seq
  list-node
  map-node
  namespaced-map-node
  set-node
  vector-node]

 [rewrite-clj.internal.node.string
  string-node]

 [rewrite-clj.internal.node.quote
  quote-node
  syntax-quote-node
  unquote-node
  unquote-splicing-node]

 [rewrite-clj.internal.node.token
  token-node]

 [rewrite-clj.internal.node.uneval
  uneval-node]

 [rewrite-clj.internal.node.whitespace
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
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
            (first)
            ((juxt tag sexpr)))
    (sexpr node)))
