(ns ^:no-doc rewrite-cljc.parser.namespaced-map
  (:require [rewrite-cljc.node :as node]
            [rewrite-cljc.parser.utils :as u]
            [rewrite-cljc.reader :as reader] ))

#?(:clj (set! *warn-on-reflection* true))

(defn- parse-prefix
  [reader]
  (let [auto-resolved? (= ":" (reader/read-while reader (fn [c] (= \: c))))
        prefix (reader/read-until reader (fn [c] (or (reader/boundary? c)
                                                     (reader/whitespace? c))))]
    {:auto-resolved? auto-resolved?
     :prefix (when (seq prefix) prefix)}))

(defn- parse-to-next-elem [reader read-next]
  (loop [nodes []]
    (let [n (read-next reader)]
      (println "-NNNN->" n)
      (if (and n (= :whitespace (node/tag n)))
        (recur (conj nodes n))
        [nodes n]))))

(if (and nil (= 1 1)) "boo" "bah")

(defn parse-namespaced-map
  "The caller has parsed up to `#:` and delegates the details to us."
  [reader read-next]
  (reader/ignore reader)
  (let [opts (parse-prefix reader)]
    (println "-popts->" opts)
    (when (and (not (:auto-resolved? opts))
               (nil? (:prefix opts)))
      (u/throw-reader reader "namespaced map expects a namespace"))
    (let [[whitespace-nodes map-node] (parse-to-next-elem reader read-next)]
      (println "-wsn->" whitespace-nodes)
      (println "-mn->" map-node)
      (when (or (not map-node)
                (not= :map (node/tag map-node)))
        (u/throw-reader reader "namespaced map expects a map"))
      (node/map-node (:children map-node) (assoc opts :prefix-trailing-whitespace whitespace-nodes)))))
