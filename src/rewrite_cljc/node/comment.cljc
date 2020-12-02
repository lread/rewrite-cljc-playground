(ns ^:no-doc rewrite-cljc.node.comment
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- comment-sexpr []
  (throw (ex-info "unsupported operation" {})))

(defrecord CommentNode [s]
  node/Node
  (tag [_] :comment)
  (printable-only? [_] true)
  (sexpr [_this] (comment-sexpr))
  (sexpr [_this _opts] (comment-sexpr))
  (length [_]
    (+ 1 (count s)))
  (string [_]
    (str ";" s))

  Object
  (toString [this]
    (node/string this)))

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
