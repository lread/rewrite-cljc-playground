(ns ^:no-doc rewrite-cljc.node.fn
  (:require [clojure.string :as string]
            [clojure.walk :as w]
            [rewrite-cljc.interop :as interop]
            [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Conversion

(defn- construct-fn
  "Construct function form."
  [syms vararg body]
  (list
    'fn*
    (vec
      (concat
        syms
        (when vararg
          (list '& vararg))))
    body))

(defn- sym-index
  "Get index based on the substring following the parameter's `%`.
   Zero means vararg."
  [n]
  (cond (= n "&") 0
        (= n "") 1
        (re-matches #"\d+" n) (interop/str->int n)
        :else (throw (ex-info "arg literal must be %, %& or %integer." {}))))

(defn- symbol->gensym
  "If symbol starting with `%`, convert to respective gensym."
  [sym-seq vararg? max-n sym]
  (when (symbol? sym)
    (let [nm (name sym)]
      (when (string/starts-with? nm "%")
        (let [i (sym-index (subs nm 1))]
          (when (and (= i 0) (not @vararg?))
            (reset! vararg? true))
          (swap! max-n max i)
          (nth sym-seq i))))))

(defn- fn-walk
  "Walk the form and create an expand function form."
  [form]
  (let [syms (for [i (range)
                   :let [base (if (= i 0)
                                "rest__"
                                (str "p" i "__"))
                         s (name (gensym base))]]
               (symbol (str s "#")))
        vararg? (atom false)
        max-n (atom 0)
        body (w/prewalk
               #(or (symbol->gensym syms vararg? max-n %) %)
               form)]
    (construct-fn
      (take @max-n (rest syms))
      (when @vararg?
        (first syms))
      body)))

;; ## Node

(defn- fn-sexpr [children opts]
  (fn-walk (node/sexprs children opts)))

(defrecord FnNode [children]
  node/Node
  (tag [_n] :fn)
  (printable-only? [_n]
    false)
  (sexpr [_n] (fn-sexpr children {}))
  (sexpr [_n opts] (fn-sexpr children opts))
  (length [_n]
    (+ 3 (node/sum-lengths children)))
  (string [_n]
    (str "#(" (node/concat-strings children) ")"))

  node/InnerNode
  (inner? [_n]
    true)
  (children [_n]
    children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_n]
    2)

  Object
  (toString [n]
    (node/string n)))

(node/make-printable! FnNode)

;; ## Constructor

(defn fn-node
  "Create node representing an anonymous function with `children`."
  [children]
  (->FnNode children))
