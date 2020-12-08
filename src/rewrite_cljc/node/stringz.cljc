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
  (tag [_n]
    (if (next lines)
      :multi-line
      :token))
  (printable-only? [_n]
    false)
  (sexpr [_n]
    (string-sexpr lines))
  (sexpr [_n _opts]
    (string-sexpr lines))
  (length [_n]
    (+ 2 (reduce + (map count lines))))
  (string [_n]
    (wrap-string (join-lines lines)))

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! StringNode)

;; ## Constructors

(defn string-node
  "Create node representing a string value where `lines`
   can be a sequence of strings or a single string."
  [lines]
  (if (string? lines)
    (->StringNode [lines])
    (->StringNode lines)))
