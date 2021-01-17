(ns ^:no-doc rewrite-clj.node.quote
  (:require [rewrite-clj.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- quote-sexpr [sym children opts]
  (list sym (first (node/sexprs children opts))))

(defrecord QuoteNode [tag prefix sym children]
  node/Node
  (tag [_n] tag)
  (node-type [_n] :quote)
  (printable-only? [_n] false)
  (sexpr [_n]
    (quote-sexpr sym children {}))
  (sexpr [_n opts]
    (quote-sexpr sym children opts))
  (length [_n]
    (+ (count prefix) (node/sum-lengths children)))
  (string [_n]
    (str prefix (node/concat-strings children)))

  node/InnerNode
  (inner? [_n] true)
  (children [_n] children)
  (replace-children [n children']
    (node/assert-single-sexpr children')
    (assoc n :children children'))
  (leader-length [_n]
    (count prefix))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! QuoteNode)

;; ## Constructors

(defn- ->node
  [t prefix sym children]
  (node/assert-single-sexpr children)
  (->QuoteNode t prefix sym children))

(defn quote-node
  "Create node representing a quoted form where `children`
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :quote "'" 'quote
      children)
    (recur [children])))

(defn syntax-quote-node
  "Create node representing a syntax-quoted form where `children`
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :syntax-quote "`" 'quote
      children)
    (recur [children])))

(defn unquote-node
  "Create node representing an unquoted form (i.e. `~...`) where `children`.
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :unquote "~" 'unquote
      children)
    (recur [children])))

(defn unquote-splicing-node
  "Create node representing an unquote-spliced form (i.e. `~@...`) where `children`.
   is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :unquote-splicing "~@" 'unquote-splicing
      children)
    (recur [children])))
