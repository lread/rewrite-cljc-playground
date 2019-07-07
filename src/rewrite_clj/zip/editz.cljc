(ns ^:no-doc rewrite-clj.zip.editz
  (:refer-clojure :exclude [replace])
  (:require [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.zip.removez :as r]
            [rewrite-clj.zip.whitespace :as ws]
            [rewrite-clj.node :as node]
            [rewrite-clj.custom-zipper.core :as z]
            [rewrite-clj.custom-zipper.utils :as u]))

;; ## In-Place Modification

(defn replace
  "Return zipper with the node at the current zipper location in `zloc` replaced with one representing
   the given `value`. The `value` will be coerced to a node if possible."
  [zloc value]
  (z/replace zloc (node/coerce value)))

(defn- edit-node
  "Create s-expression from node, apply the function and create
   node from the result."
  [node f]
  (-> (node/sexpr node)
      (f)
      (node/coerce)))

(defn edit
  "Return zipper with the node at the current zipper location in `zloc` replaced with the result of
   (`f` (s-expression node) `args`).
   The result will be coerced to a node if possible."
  [zloc f & args]
  (z/edit zloc edit-node #(apply f % args)))

;; ## Splice

(defn splice
  "Return zipper with the node at the current zipper location in `zloc` with its children merged into itself.
   (akin to Clojure's `unquote-splicing` macro: `~@...`).
   - if the node is not one that can have children, no modification will
     be performed.
   - if the node has no or only whitespace children, it will be removed.
   - otherwise, splicing will be performed, moving the zipper to the first
     non-whitespace child afterwards."
  [zloc]
  (if (z/branch? zloc)
    (if-let [children (->> (z/children zloc)
                           (drop-while node/whitespace?)
                           (reverse)
                           (drop-while node/whitespace?)
                           (seq))]
      (let [loc (->> (reduce z/insert-right zloc children)
                     (u/remove-and-move-right))]
        (or (ws/skip-whitespace loc) loc))
      (r/remove zloc))
    zloc))

;; ## Prefix/Suffix

(defn- edit-token
  [zloc str-fn]
  (let [e (base/sexpr zloc)
        e' (cond (string? e) (str-fn e)
                 (keyword? e) (keyword (namespace e) (str-fn (name e)))
                 (symbol? e) (symbol (namespace e) (str-fn (name e))))]
    (z/replace zloc (node/token-node e'))))

(defn- edit-multi-line
  [zloc line-fn]
  (let [n (-> (z/node zloc)
              (update-in [:lines] (comp line-fn vec)))]
    (z/replace zloc n)))

(defn prefix
  "Return zipper with the token node at the current zipper location `zloc` prefixed with string `s`.
  If node is multi-line prefix all lines."
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str s %))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (update-in lines [0] #(str s %))))
                     (edit-multi-line zloc ))))

(defn suffix
  "Return zipper with the token node at the current zipper location `zloc` suffixed with string `s`.
  If node is multi-line suffix all lines."
  [zloc s]
  (case (base/tag zloc)
    :token      (edit-token zloc #(str % s))
    :multi-line (->> (fn [lines]
                       (if (empty? lines)
                         [s]
                         (concat (butlast lines) (str (last lines) s))))
                     (edit-multi-line zloc))))
