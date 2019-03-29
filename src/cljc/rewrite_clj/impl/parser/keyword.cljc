(ns ^:no-doc rewrite-clj.impl.parser.keyword
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.impl.parser.utils :as u]
            [clojure.tools.reader.reader-types :as r]
            [clojure.tools.reader :as edn]))

(defn parse-keyword
  [#?(:cljs ^not-native reader :clj reader)]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (node/keyword-node
       (edn/read reader)
       true)
      (do
        (r/unread reader \:)
        (node/keyword-node (edn/read reader))))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))
