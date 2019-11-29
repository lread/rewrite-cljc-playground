(ns ^:no-doc rewrite-cljc.parser.namespaced-map
  (:require [rewrite-cljc.node :as node]
            [rewrite-cljc.reader :as reader]
            [rewrite-cljc.parser.utils :as u]))

(defn- specifies-aliased?
  [reader]
  (case (reader/peek reader)
    nil (u/throw-reader reader "Unexpected EOF.")
    \:  (do (reader/ignore reader) true)
    false))

(defn- includes-keyword?
  [reader]
  (let [c (reader/peek reader)]
    (and (not (reader/whitespace? c))
         (not (= \{ c)))))

(defn- parse-keyword
  [reader read-next aliased?]
  (let [k (read-next reader)]
    (cond
      (nil? k)
      (u/throw-reader reader "Unexpected EOF.")

      (not= :token (node/tag k))
      (if aliased?
        (u/throw-reader reader ":namespaced-map expected namespace alias or map")
        (u/throw-reader reader ":namespaced-map expected namespace prefix"))
      :else (node/token-node (keyword (str (when aliased? ":") (node/string k)))))))

(defn- parse-upto-printable
  [reader read-next]
  (loop [vs []]
    (if (and
         (seq vs)
         (not (node/printable-only? (last vs))))
      vs
      (if-let [v (read-next reader)]
        (recur (conj vs v))
        nil))))

(defn- parse-for-map
  [reader read-next]
  (let [m (parse-upto-printable reader read-next)]
    (cond
      (nil? m)
      (u/throw-reader reader "Unexpected EOF.")

      (not= :map (node/tag (last m)))
      (u/throw-reader reader ":namespaced-map expects a map")
      :else m)))

(defn parse-namespaced-map
  [reader read-next]
  (reader/ignore reader)
  (let [aliased? (specifies-aliased? reader)]
    (if (includes-keyword? reader)
      (node/namespaced-map-node
       (into [(parse-keyword reader read-next aliased?)]
             (parse-for-map reader read-next)))
      (if aliased?
        (node/namespaced-map-node
         (into [(node/token-node (keyword ":"))]
               (parse-for-map reader read-next)))
        (u/throw-reader reader ":namespaced-map expected namespace prefix")))))
