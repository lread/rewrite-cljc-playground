(ns rewrite-clj.parser
  "API to injest Clojure/ClojureScript source code for zipping."
  (:require [rewrite-clj.internal.parser.core :as p]
            [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]))

;; ## Parser Core

(defn ^:no-doc parse
  "Parse next form from the given reader."
  [^not-native reader]
  (p/parse-next reader))

(defn ^:no-doc parse-all
  "Parse all forms from the given reader."
  [^not-native reader]
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

;; TODO: we omit the clj file readers for cljs
#_(defn parse-file
  "Parse first form from the given file."
  [f]
  (let [r (reader/file-reader f)]
    (with-open [_ ^java.io.Closeable (.-rdr r)]
      (parse r))))

#_(defn parse-file-all
  "Parse all forms from the given file."
  [f]
  (let [r (reader/file-reader f)]
    (with-open [_ ^java.io.Closeable (.-rdr r)]
      (parse-all r))))
