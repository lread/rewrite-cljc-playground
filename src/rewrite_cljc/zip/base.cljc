(ns ^:no-doc rewrite-cljc.zip.base
  (:refer-clojure :exclude [print])
  (:require [rewrite-cljc.custom-zipper.core :as z]
            [rewrite-cljc.node :as node]
            [rewrite-cljc.parser :as p]
            [rewrite-cljc.zip.whitespace :as ws]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Zipper
(defn edn*
  "Create and return zipper from Clojure/ClojureScript/EDN `node` (likely parsed by [[rewrite-cljc.parse]]).

   Set `:track-position?` in `options` to enable ones-based row/column tracking.
   See [[rewrite-cljc.zip/position]].

   NOTE: when position tracking is enabled, `clojure.zip` is not interchangeable with `rewrite-cljc.zip`, you must use `rewrite-cljc.zip`."
  ([node]
   (edn* node {}))
  ([node {:keys [track-position?]}]
   (if track-position?
     (z/custom-zipper node)
     (z/zipper node))))

(defn edn
  "Create and return zipper from Clojure/ClojureScript/EDN `node` (likely parsed by [[rewrite-cljc.parse]])
   and move to the first non-whitespace/non-comment child.

   Set `:track-position?` in `options` to enable ones-based row/column tracking.
   See [[rewrite-cljc.zip/position]].

   NOTE: when position tracking is enabled, `clojure.zip` is not interchangeable with `rewrite-cljc.zip`, you must use `rewrite-cljc.zip`."
  ([node] (edn node {}))
  ([node options]
   (if (= (node/tag node) :forms)
     (let [top (edn* node options)]
       (or (-> top z/down ws/skip-whitespace)
           top))
     (recur (node/forms-node [node]) options))))

;; ## Inspection

(defn tag
  "Return tag of current node in `zloc`."
  [zloc]
  (some-> zloc z/node node/tag))

(defn sexpr
  "Return s-expression of current node in `zloc`."
  [zloc]
  (some-> zloc z/node node/sexpr))

(defn ^{:added "0.4.4"} child-sexprs
  "Return s-expression of children of current node in `zloc`."
  [zloc]
  (some-> zloc z/node node/child-sexprs))

(defn length
  "Return length of printable string of current node in `zloc`."
  [zloc]
  (or (some-> zloc z/node node/length) 0))

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED. Return a tag/s-expression pair for inner nodes, or
   the s-expression itself for leaves."
  [zloc]
  (some-> zloc z/node node/value))

;; ## Read
(defn of-string
  "Create and return zipper from all forms in Clojure/ClojureScript/EDN string `s`.

   Set `:track-position?` in `options` to enable ones-based row/column tracking.
   See [[rewrite-cljc.zip/position]].

   NOTE: when position tracking is enabled, `clojure.zip` is not interchangeable with `rewrite-cljc.zip`, you must use `rewrite-cljc.zip`."
  ([s] (of-string s {}))
  ([s options]
   (some-> s p/parse-string-all (edn options))))

#?(:clj
   (defn of-file
     "Create and return zipper from all forms in Clojure/ClojureScript/EDN File `f`.

      Set `:track-position?` in `options` to enable ones-based row/column tracking.
      See [[rewrite-cljc.zip/position]].

      NOTE: when position tracking is enabled, `clojure.zip` is not interchangeable with `rewrite-cljc.zip`, you must use `rewrite-cljc.zip`."
     ([f] (of-file f {}))
     ([f options]
      (some-> f p/parse-file-all (edn options)))))

;; ## Write

(defn ^{:added "0.4.0"} string
  "Return string representing the current node in `zloc`."
  [zloc]
  (some-> zloc z/node node/string))

(defn ^{:added "0.4.0"} root-string
  "Return string representing the zipped-up `zloc` zipper."
  [zloc]
  (some-> zloc z/root node/string))

#?(:clj
   (defn- print! [^String s writer]
     (if writer
       (.write ^java.io.Writer writer s)
       (recur s *out*)))
   :cljs
   (defn- print! [s _writer]
     (string-print s)))

(defn print
  "Print current node in `zloc`.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  [zloc & [writer]]
  (some-> zloc
          string
          (print! writer)))

(defn print-root
  "Zip up and print `zloc` from root node.

   NOTE: Optional `writer` is currently ignored for ClojureScript."
  [zloc & [writer]]
  (some-> zloc
          root-string
          (print! writer)))
