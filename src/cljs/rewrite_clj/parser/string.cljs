(ns rewrite-clj.parser.string
  (:require [rewrite-clj.parser.utils :as u]
            [rewrite-clj.node :as node]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.reader.reader-types :as r]
            [goog.string :as gstring]
            [clojure.string :as string]))

(defn- flush-into
  "Flush buffer and add string to the given vector."
  [lines buf]
  (let [s (.toString buf)]
    (.set buf "")
    (conj lines s)))

(defn- read-string-data
  [^not-native reader]
  (u/ignore reader)
  (let [buf (gstring/StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (r/read-char reader)]
        (cond (and (not escape?) (identical? c \"))
              (flush-into lines buf)

              (identical? c \newline)
              (recur escape? (flush-into lines buf))

              :else
              (do
                (.append buf c)
                (recur (and (not escape?) (identical? c \\)) lines)))
        (u/throw-reader reader "Unexpected EOF while reading string.")))))

(defn parse-string
  [^not-native reader]
  (node/string-node (read-string-data reader)))

;; TODO: differs from clj... not sure why.  clj version has introduce regex node... but still a bit different.. might be due to
;; differences between clj and cljs?
#_(defn parse-regex
  [^not-native reader]
  (let [lines (read-string-data reader)
        regex (string/join "\n" lines)]
    (node/token-node (re-pattern regex) (str "#\"" regex "\""))))
;; let's try clj version
(defn parse-regex
  [reader]
  (let [h (read-string-data reader)]
    (string/join "\n" h)))
