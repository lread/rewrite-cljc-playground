(ns ^:no-doc rewrite-cljc.node.comment
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- comment-sexpr []
  (throw (ex-info "unsupported operation" {})))

(defrecord CommentNode [s]
  node/Node
  (tag [_n] :comment)
  (node-type [_n] :comment)
  (printable-only? [_n] true)
  (sexpr [_n] (comment-sexpr))
  (sexpr [_n _opts] (comment-sexpr))
  (length [_n]
    (+ 1 (count s)))
  (string [_n]
    (str ";" s))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! CommentNode)

;; ## Constructor

(defn comment-node
  "Create node representing a comment with text `s`."
  [s]
  {:pre [(re-matches #"[^\r\n]*[\r\n]?" s)]}
  (->CommentNode s))

(defn comment?
  "Returns true if `node` is a comment."
  [node]
  (= (node/tag node) :comment))
