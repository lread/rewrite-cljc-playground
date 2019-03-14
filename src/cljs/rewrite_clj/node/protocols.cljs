(ns rewrite-clj.node.protocols
  (:require [clojure.string :as string]
            [goog.string :as gstring]
             goog.string.format))

(defprotocol Node
  "Protocol for EDN/Clojure nodes."
  (tag [_]
    "Keyword representing the type of the node.")
  (printable-only? [_]
    "Return true if the node cannot be converted to an s-expression
     element.")
  (sexpr [_]
    "Convert node to s-expression.")
  (length [_]
    "Get number of characters for the string version of this node.")
  (string [_]
    "Convert node to printable string."))


(extend-protocol Node
  object
  (tag [_] :unknown)
  (printable-only? [_] false)
  (sexpr [this] this)
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexprs
  "Given a seq of nodes, convert those that represent s-expressions
   to the respective forms."
  [nodes]
  (->> nodes
       (remove printable-only?)
       (map sexpr)))

(defn sum-lengths
  "Sum up lengths of the given nodes."
  [nodes]
  (reduce + (map length nodes)))

(defn concat-strings
  "Convert nodes to strings and concatenate them."
  [nodes]
  (reduce str (map string nodes)))


(defprotocol InnerNode
  "Protocol for non-leaf EDN/Clojure nodes."
  (inner? [_]
    "Check whether the node can contain children.")
  (children [_]
    "Get child nodes.")
  (replace-children [_ children]
    "Replace the node's children.")
  (leader-length [_]
    "How many characters appear before children?"))

(extend-protocol InnerNode
  object
  (inner? [_] false)
  (children [_]
    (throw (js/Error. "UnsupportedOperationException")))
  (replace-children [_ _]
    (throw (js/Error. "UnsupportedOperationException")))
  (leader-length [_]
    (throw (js/Error. "UnsupportedOperationException"))))

(defn child-sexprs
  "Get all child s-expressions for the given node."
  [node]
  (if (inner? node)
    (sexprs (children node))))

(defprotocol NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [_]))

;; ## Print helper

(defn- ^:no-doc node->string
  ^String
  [node]
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (gstring.format "\n  %s\n"))
             (str " " n))]
    (gstring.format "<%s:%s>" (name (tag node)) n')))


(defn ^:no-doc write-node
  [^java.io.Writer writer node]
  (pr (node->string node))
  #_(.write writer (node->string node)))

(defn make-printable! [obj]
  (extend-protocol IPrintWithWriter
    obj
    (-pr-writer [o writer _opts]
      (-write writer (node->string o)))))

;; ## Helpers

(defn assert-sexpr-count
  [nodes c]
  (assert
    (= (count (remove printable-only? nodes)) c)
   (str "can only contain" c " non-whitespace form(s).")))

(defn assert-single-sexpr
  [nodes]
  (assert-sexpr-count nodes 1))

(defn ^:no-doc extent
  "A node's extent is how far it moves the \"cursor\".
  Rows are simple - if we have x newlines in the string representation, we
  will always move the \"cursor\" x rows.
  Columns are strange.  If we have *any* newlines at all in the textual
  representation of a node, following nodes' column positions are not
  affected by our startting column position at all.  So the second number
  in the pair we return is interpreted as a relative column adjustment
  when the first number in the pair (rows) is zero, and as an absolute
  column position when rows is non-zero."
  [node]
  (let [{:keys [row col next-row next-col]} (meta node)]
    (if (and row col next-row next-col)
      [(- next-row row)
       (if (= row next-row row)
         (- next-col col)
         next-col)]
      (let [s (string node)
            rows (->> s (filter (partial = \newline)) count)
            cols (if (zero? rows)
                   (count s)
                   (->> s
                     reverse
                     (take-while (complement (partial = \newline)))
                     count
                     inc))]
        [rows cols]))))

(defn ^:no-doc +extent
  [[row col] [row-extent col-extent]]
  [(+ row row-extent)
   (cond-> col-extent (zero? row-extent) (+ col))])
