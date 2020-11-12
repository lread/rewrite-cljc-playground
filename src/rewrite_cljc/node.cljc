(ns rewrite-cljc.node
  "Create and evaluate nodes.

  All nodes represent Clojure/ClojureScript/EDN."
  ^{:added "0.4.0"}
  (:refer-clojure :exclude [string coerce])
  (:require [rewrite-cljc.node.coercer]
            [rewrite-cljc.node.comment]
            [rewrite-cljc.node.fn]
            [rewrite-cljc.node.forms]
            [rewrite-cljc.node.integer]
            [rewrite-cljc.node.keyword]
            [rewrite-cljc.node.meta]
            [rewrite-cljc.node.namespaced-map]
            [rewrite-cljc.node.protocols :as np]
            [rewrite-cljc.node.quote]
            [rewrite-cljc.node.reader-macro]
            [rewrite-cljc.node.regex]
            [rewrite-cljc.node.seq]
            [rewrite-cljc.node.stringz]
            [rewrite-cljc.node.token]
            [rewrite-cljc.node.uneval]
            [rewrite-cljc.node.whitespace]
            #?(:clj [rewrite-cljc.potemkin.clojure :refer [import-vars]]))
  #?(:cljs (:require-macros [rewrite-cljc.potemkin.cljs :refer [import-vars]])))

#?(:clj (set! *warn-on-reflection* true))

(import-vars
 [rewrite-cljc.node.protocols
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
  string
  tag]

 [rewrite-cljc.node.comment
  comment-node
  comment?]

 [rewrite-cljc.node.fn
  fn-node]

 [rewrite-cljc.node.forms
  forms-node]

 [rewrite-cljc.node.integer
  integer-node]

 [rewrite-cljc.node.keyword
  keyword-node]

 [rewrite-cljc.node.meta
  meta-node
  raw-meta-node]

 [rewrite-cljc.node.namespaced-map
  namespaced-map-node]

 [rewrite-cljc.node.regex
  regex-node]

 [rewrite-cljc.node.reader-macro
  deref-node
  eval-node
  reader-macro-node
  var-node]

 [rewrite-cljc.node.seq
  list-node
  map-node
  set-node
  vector-node]

 [rewrite-cljc.node.stringz
  string-node]

 [rewrite-cljc.node.quote
  quote-node
  syntax-quote-node
  unquote-node
  unquote-splicing-node]

 [rewrite-cljc.node.token
  token-node]

 [rewrite-cljc.node.uneval
  uneval-node]

 [rewrite-cljc.node.whitespace
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
  (or (rewrite-cljc.node.whitespace/whitespace? node)
      (rewrite-cljc.node.comment/comment? node)))

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
