(ns ^:no-doc rewrite-cljc.zip.findz
  (:refer-clojure :exclude [find])
  (:require [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.move :as m]
            [rewrite-cljc.custom-zipper.core :as z]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Helpers

(defn- tag-predicate
  [t & [additional]]
  (if additional
    (fn [node]
      (and (= (base/tag node) t)
           (additional node)))
    #(= (base/tag %) t)))

(defn- position-in-range? [zloc pos]
  (let [[r c] (if (map? pos) [(:row pos) (:col pos)] pos)]
    (when (or (<= r 0) (<= c 0))
      (throw (ex-info "zipper row and col positions are ones-based" {:pos pos})))
    (let [[[zstart-row zstart-col][zend-row zend-col]] (z/position-span zloc)]
      (and (>= r zstart-row)
           (<= r zend-row)
           (if (= r zstart-row) (>= c zstart-col) true)
           (if (= r zend-row) (< c zend-col) true)))))

;; ## Find Operations

(defn find
  "Return the first node satisfying predicate `p?` seaching from the current node
   in `zloc` traversing by function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc p?]
   (find zloc m/right p?))
  ([zloc f p?]
   (->> zloc
        (iterate f)
        (take-while identity)
        (take-while (complement m/end?))
        (drop-while (complement p?))
        (first))))

(defn find-last-by-pos
  "Return the last node spanning position `pos` that satisfies predicate `p?`
   searching depth-first from the current node in `zloc`.

  NOTE: Does not ignore whitespace/comment nodes."
  ([zloc pos] (find-last-by-pos zloc pos (constantly true)))
  ([zloc pos p?]
   (->> zloc
        (iterate z/next)
        (take-while identity)
        (take-while (complement m/end?))
        (filter #(and (p? %)
                      (position-in-range? % pos)))
        last)))

(defn find-depth-first
  "Return first node satisfying predicate `p?` searching depth-first from
   the current node in `zloc`."
  [zloc p?]
  (find zloc m/next p?))

(defn find-next
  "Return the first node satisfying predicate `p?` searching one movement `f` from the current
   node in `zloc` traversing by function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc p?]
   (find-next zloc m/right p?))
  ([zloc f p?]
   (some-> zloc f (find f p?))))

(defn find-next-depth-first
  "Return the first node satisfying predicate `p?` searching depth-first from one
   node after the current node in `zloc`"
  [zloc p?]
  (find-next zloc m/next p?))

(defn find-tag
  "Return the first node with tag `t` searching from the current node in `zloc` traversing by
   function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc t]
   (find-tag zloc m/right t))
  ([zloc f t]
   (find zloc f #(= (base/tag %) t))))

(defn find-next-tag
  "Return the first node with tag `t` searching one movement `f` from the current
   node in `zloc` traversing by function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc t]
   (find-next-tag zloc m/right t))
  ([zloc f t]
   (->> (tag-predicate t)
        (find-next zloc f))))

(defn find-tag-by-pos
  "Return the last node spanning position `pos` with tag `t` searching depth-first from the current node in `zloc`."
  ([zloc pos t]
   (find-last-by-pos zloc pos #(= (base/tag %) t))))

(defn find-token
  "Return the first token node satisfying predicate `p?` searching from the current node in `zloc` traversing
   by function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc p?]
   (find-token zloc m/right p?))
  ([zloc f p?]
   (->> (tag-predicate :token p?)
        (find zloc f))))

(defn find-next-token
  "Return the first token node satisfying predicate `p?` searching from the current node in `zloc` traversing
   by function `f`.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc p?]
   (find-next-token zloc m/right p?))
  ([zloc f p?]
   (find-token (f zloc) f p?)))

(defn find-value
  "Return the first token node with value `v` searching one movement `f` from the current node in `zloc` traversing
   by function `f`.

   `v` can be a single value or a set. When `v` is a set matches on any value in set.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc v]
   (find-value zloc m/right v))
  ([zloc f v]
   (let [p? (if (set? v)
              (comp v base/sexpr)
              #(= (base/sexpr %) v))]
     (find-token zloc f p?))))

(defn find-next-value
  "Return the first token node with value `v` searching one movement `f` from the current node in `zloc` traversing
   by function `f`.

   `v` can be a single value or a set. When `v` is a set matches on any value in set.

   `f` defaults to [[rewrite-cljc.zip/right]]"
  ([zloc v]
   (find-next-value zloc m/right v))
  ([zloc f v]
   (find-value (f zloc) f v)))
