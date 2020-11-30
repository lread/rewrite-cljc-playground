(ns ^:no-doc rewrite-cljc.zip.seqz
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-cljc.custom-zipper.core :as z]
            [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.editz :as e]
            [rewrite-cljc.zip.findz :as f]
            [rewrite-cljc.zip.insert :as i]
            [rewrite-cljc.zip.move :as m]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Predicates

(defn seq?
  "Returns true if current node in `zloc` is a sequence."
  [zloc]
  (contains?
    #{:forms :list :vector :set :map}
    (base/tag zloc)))

(defn list?
  "Returns true if current node in `zloc` is a list."
  [zloc]
  (= (base/tag zloc) :list))

(defn vector?
  "Returns true if current node in `zloc` is a vector."
  [zloc]
  (= (base/tag zloc) :vector))

(defn set?
  "Returns true if current node in `zloc` is a set."
  [zloc]
  (= (base/tag zloc) :set))

(defn map?
  "Returns true if current node in `zloc` is a map."
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
                 (when-let [n (m/right loc)]
                   (f n))))
             (take-while identity)
             (last)
             (m/up))
    zloc))

(defn map-vals
  "Returns `zloc` with function `f` applied to all value nodes of the current node.
   Current node must be map node."
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
  "Returns `zloc` with function `f` applied to all key nodes of the current node.
   Current node must be map node."
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
  "Returns `zloc` with function `f` applied to all nodes of the current node.
   Current node must be a sequence node.

   `f` is applied to:
   - value nodes of maps
   - each element of a seq"
  [f zloc]
  {:pre [(seq? zloc)]}
  (if (map? zloc)
    (map-vals f zloc)
    (map-seq f zloc)))

;; ## Get/Assoc

(defn get
  "Returns `zloc` located to current node's value node matching `k` else `nil`.

  `k` should be:
  - a key for maps
  - an index for sequences"
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
  "Returns `zloc` with current node's `k` set to value `v`.

  `k` should be:
  - a key for maps
  - an index for sequences, an exception is thrown if index is out of bounds"
  [zloc k v]
  (if-let [vloc (get zloc k)]
    (-> vloc (e/replace v) m/up)
    (if (map? zloc)
      (-> zloc
          (i/append-child k)
          (i/append-child v))
      (throw
        (ex-info (str "index out of bounds: " k) {})))))
