(ns ^:no-doc rewrite-clj.impl.potemkin-helper
  (:require [clojure.string :as string]))

(defn alter-sym [orig-sym opts]
  (if-let [sym-pattern (:sym-to-pattern opts)]
    (symbol (string/replace sym-pattern #"@@orig-sym@@" (str orig-sym)))
    orig-sym))

(defn alter-meta [orig-meta opts]
  ;;(util/debug-prn "alter-meta... orig-meta" (pretty-str orig-meta) )
  ;;(util/debug-prn "alter-meta... opts" (pretty-str opts) )
  (if-let [doc-pattern (:doc-to-pattern opts)]
    (update orig-meta
            :doc
            (fn [orig-doc]
              (-> doc-pattern
                  (string/replace #"@@orig-sym@@" (str (:name orig-meta)))
                  (string/replace #"@@orig-doc@@" (or orig-doc "")))))
    orig-meta))

(defn unravel-syms [x]
  (loop [acc []
         r x
         cur-opts {}]
    (if-let [n (first r)]
      (cond
        (sequential? n)
        (recur (apply conj acc (map #(vector (symbol
                                              (str (first n)
                                                   (when-let [ns (namespace %)]
                                                     (str "." ns)))
                                              (name %))
                                             cur-opts)
                                    (rest n)))
               (rest r)
               cur-opts)

        (map? n)
        (recur acc (rest r) n)

        :else
        (recur (conj acc [n cur-opts]) (rest r) cur-opts))
      acc)))
