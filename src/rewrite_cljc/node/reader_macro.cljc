(ns ^:no-doc rewrite-cljc.node.reader-macro
  (:require [rewrite-cljc.node.protocols :as node]
            [rewrite-cljc.node.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- reader-sexpr [sexpr-fn children opts]
  (if sexpr-fn
    (sexpr-fn (node/sexprs children opts))
    (throw (ex-info "unsupported operation" {}))))

(defrecord ReaderNode [tag prefix suffix
                       sexpr-fn sexpr-count
                       children]
  node/Node
  (tag [_n] tag)
  (node-type [_n] :reader)
  (printable-only? [_n]
    (not sexpr-fn))
  (sexpr [_n]
    (reader-sexpr sexpr-fn children {}))
  (sexpr [_n opts]
    (reader-sexpr sexpr-fn children opts))
  (length [_n]
    (-> (node/sum-lengths children)
        (+ 1 (count prefix) (count suffix))))
  (string [_n]
    (str "#" prefix (node/concat-strings children) suffix))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [n children']
    (when sexpr-count
      (node/assert-sexpr-count children' sexpr-count))
    (assoc n :children children'))
  (leader-length [_n]
    (inc (count prefix)))
  Object
  (toString [n]
    (node/string n)))

(defn- reader-macro-sexpr [node]
  (list 'read-string (node/string node)))

(defrecord ReaderMacroNode [children]
  node/Node
  (tag [_n] :reader-macro)
  (node-type [_n] :reader-macro)
  (printable-only?[_n] false)
  (sexpr [n]
    (reader-macro-sexpr n))
  (sexpr [n _opts]
    (reader-macro-sexpr n))
  (length [_n]
    (inc (node/sum-lengths children)))
  (string [_n]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [n children']
    (node/assert-sexpr-count children' 2)
    (assoc n :children children'))
  (leader-length [_n]
    1)
  Object
  (toString [n]
    (node/string n)))

(defn- deref-sexpr [children opts]
  (list* 'deref (node/sexprs children opts)))

(defrecord DerefNode [children]
  node/Node
  (tag [_n] :deref)
  (node-type [_n] :deref)
  (printable-only?[_n] false)
  (sexpr [_n]
    (deref-sexpr children {}))
  (sexpr [_n opts]
    (deref-sexpr children opts))
  (length [_n]
    (inc (node/sum-lengths children)))
  (string [_n]
    (str "@" (node/concat-strings children)))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [n children']
    (node/assert-sexpr-count children' 1)
    (assoc n :children children'))
  (leader-length [_]
    1)

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! ReaderNode)
(node/make-printable! ReaderMacroNode)
(node/make-printable! DerefNode)

;; ## Constructors

(defn- ->node
  [tag prefix suffix sexpr-fn sexpr-count children]
  (when sexpr-count
    (node/assert-sexpr-count children sexpr-count))
  (->ReaderNode
    tag prefix suffix
    sexpr-fn sexpr-count
    children))

(defn var-node
  "Create node representing a var
   where `children` is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node :var "'" "" #(list* 'var %) 1 children)
    (recur [children])))

(defn eval-node
  "Create node representing an inline evaluation (i.e. `#=...`)
   where `children` is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->node
      :eval "=" ""
      #(list 'eval (list* 'quote %))
      1 children)
    (recur [children])))

(defn reader-macro-node
  "Create node representing a reader macro (i.e. `#... ...`) with `children`. "
  ([children]
   (->ReaderMacroNode children))
  ([macro-node form-node]
   (->ReaderMacroNode [macro-node (ws/spaces 1) form-node])))

(defn deref-node
  "Create node representing the dereferencing of a form (i.e. `@...`)
   where `children` is either a sequence of nodes or a single node."
  [children]
  (if (sequential? children)
    (->DerefNode children)
    (->DerefNode [children])))
