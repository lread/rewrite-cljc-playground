(ns ^:no-doc rewrite-clj.zip.seqz
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.zip.base :as base]
            [rewrite-clj.zip.editz :as e]
            [rewrite-clj.zip.findz :as f]
            [rewrite-clj.zip.insert :as i]
            [rewrite-clj.zip.move :as m]
            [rewrite-clj.custom-zipper.core :as z]))

;; ## Predicates

(defn seq?
  "Returns true if node at current zipper location in `zloc` is a sequence."
  [zloc]
  (contains?
    #{:forms :list :vector :set :map}
    (base/tag zloc)))

(defn list?
  "Returns true if node at current zipper location in `zloc` is a list."
  [zloc]
  (= (base/tag zloc) :list))

(defn vector?
  "Returns true if node at current zipper location in `zloc` is a vector."
  [zloc]
  (= (base/tag zloc) :vector))

(defn set?
  "Returns true if node at current zipper location in `zloc` is a set."
  [zloc]
  (= (base/tag zloc) :set))

(defn map?
  "Returns true if node at current zipper location in `zloc` is a map."
  [zloc]
  (= (base/tag zloc) :map))

;; ## Map Operations

(defn- map-seq
  [f zloc]
  {:pre [(seq? zloc)]}
  (if-let [n0 (m/down zloc)]
    (some->> (f n0)
             (iterate
               (fn [loc]
                 (if-let [n (m/right loc)]
                   (f n))))
             (take-while identity)
             (last)
             (m/up))
    zloc))

(defn map-vals
  "Returns zipper with function `f` applied to all value nodes of the map node at the current zipper location in `zloc`."
  [f zloc]
  {:pre [(map? zloc)]}
  (loop [loc (m/down zloc)
         parent zloc]
    (if-not (and loc (z/node loc))
      parent
      (if-let [v0 (m/right loc)]
        (if-let [v (f v0)]
          (recur (m/right v) (m/up v))
          (recur (m/right v0) parent))
        parent))))

(defn map-keys
  "Returns zipper with function `f` applied to all key nodes of the map node at the current zipper location in `zloc`."
  [f zloc]
  {:pre [(map? zloc)]}
  (loop [loc (m/down zloc)
         parent zloc]
    (if-not (and loc (z/node loc))
      parent
      (if-let [v (f loc)]
        (recur (m/right (m/right v)) (m/up v))
        (recur (m/right (m/right loc)) parent)))))

(defn map
  "Returns zipper with function `f` applied to all value nodes of the seq node at the current zipper location in `zloc`.

   Iterates over:
   - value nodes of maps
   - each element of a seq"
  [f zloc]
  {:pre [(seq? zloc)]}
  (if (map? zloc)
    (map-vals f zloc)
    (map-seq f zloc)))

;; ## Get/Assoc

(defn get
  "Returns value node mapped to key `k` when current zipper location in `zloc` is a map node.
   Returns nth `k` value node when current zipper location in `zloc` is a seq node."
  [zloc k]
  {:pre [(or (map? zloc) (and (seq? zloc) (integer? k)))]}
  (if (map? zloc)
    (some-> zloc m/down (f/find-value k) m/right)
    (nth
      (some->> (m/down zloc)
               (iterate m/right)
               (take-while identity))
      k)))

(defn assoc
  "Returns zipper with key `k` set to value `v` when current zipper location in `zloc` is a map node.
   Returns zipper with index `k` set to value `v` when current zipper location in `zloc` is a sequence."
  [zloc k v]
  (if-let [vloc (get zloc k)]
    (-> vloc (e/replace v) m/up)
    (if (map? zloc)
      (-> zloc
          (i/append-child k)
          (i/append-child v))
      (throw
        (ex-info (str "index out of bounds: " k) {})))))
