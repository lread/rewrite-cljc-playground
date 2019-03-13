(ns rewrite-clj.node
  "Facade for node related namespaces."
  (:require [rewrite-clj.node.coercer]
            [rewrite-clj.node.comment]
            [rewrite-clj.node.fn]
            [rewrite-clj.node.forms]
            [rewrite-clj.node.keyword]
            [rewrite-clj.node.meta]
            [rewrite-clj.node.protocols]
            [rewrite-clj.node.quote]
            [rewrite-clj.node.reader-macro]
            [rewrite-clj.node.regex]
            [rewrite-clj.node.seq]
            [rewrite-clj.node.stringz]
            [rewrite-clj.node.token]
            [rewrite-clj.node.uneval]
            [rewrite-clj.node.whitespace])
  (:require-macros [rewrite-clj.potemkin-cljs :refer [import-vars import-def]]))

(import-vars
 [rewrite-clj.node.protocols
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

 [rewrite-clj.node.comment
  comment-node
  comment?]

 [rewrite-clj.node.fn
  fn-node]

 [rewrite-clj.node.forms
  forms-node]

 ;; TODO: do we have integet nodes in cljs?
 ;;[rewrite-clj.node.integer
 ;; integer-node]

 [rewrite-clj.node.keyword
  keyword-node]

 [rewrite-clj.node.meta
  meta-node
  raw-meta-node]

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
  namespaced-map-node
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
  token-node]

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
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))
