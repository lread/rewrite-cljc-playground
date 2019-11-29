(ns rewrite-cljc.parser
  "Parse Clojure/ClojureScript/EDN source code to nodes.

  After parsing, the typical next step is [[rewrite-cljc.zip/edn]] to create zipper.

  Alternatively consider parsing and zipping in one step from [[rewrite-cljc.zip/of-string]] or [[rewrite-cljc.zip/of-file]]."
  (:require [rewrite-cljc.parser.core :as p]
            [rewrite-cljc.node :as node]
            [rewrite-cljc.reader :as reader]))

;; ## Parser Core

(defn ^:no-doc parse
  "Parse next form from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (p/parse-next reader))

(defn ^:no-doc parse-all
  "Parse all forms from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (let [nodes (->> (repeatedly #(parse reader))
                   (take-while identity)
                   (doall))]
    (with-meta
      (node/forms-node nodes)
      (meta (first nodes)))))

;; ## Specialized Parsers

(defn parse-string
  "Parse first form in the given string."
  [s]
  (parse (reader/string-reader s)))

(defn parse-string-all
  "Parse all forms in the given string."
  [s]
  (parse-all (reader/string-reader s)))

#?(:clj
   (defn parse-file
     "Parse first form from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse r)))))

#?(:clj
   (defn parse-file-all
     "Parse all forms from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse-all r)))))
