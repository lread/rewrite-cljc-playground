(ns ^:no-doc rewrite-cljc.node.stringz
  (:require [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [rewrite-cljc.node.protocols :as node] ))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- wrap-string
  [s]
  (str "\"" s "\""))

(defn- join-lines
  [lines]
  (string/join "\n" lines))

(defn- string-sexpr [lines]
  (join-lines
   (map
    (comp edn/read-string wrap-string)
    lines)) )

(defrecord StringNode [lines]
  node/Node
  (tag [_]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_]
    false)
  (sexpr [this]
    (string-sexpr lines))
  (sexpr [_this _opts]
    (string-sexpr lines))
  (length [_]
    (+ 2 (reduce + (map count lines))))
  (string [_]
    (wrap-string (join-lines lines)))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! StringNode)

;; ## Constructors

(defn string-node
  "Create node representing a string value where `lines`
   can be a sequence of strings or a single string."
  [lines]
  (if (string? lines)
    (->StringNode [lines])
    (->StringNode lines)))
