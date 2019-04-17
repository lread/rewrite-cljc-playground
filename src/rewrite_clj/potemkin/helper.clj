(ns ^:no-doc rewrite-clj.potemkin.helper
  (:require [clojure.string :as string]))

(defn new-name [orig-name opts]
  (if-let [sym-pattern (:sym-to-pattern opts)]
    (symbol (string/replace sym-pattern #"@@orig-name@@" (str orig-name)))
    orig-name))

(defn new-meta [orig-meta opts]
  (when-let [doc-pattern (:doc-to-pattern opts)]
    {:doc (-> doc-pattern
              (string/replace #"@@orig-name@@" (str (:name orig-meta)))
              (string/replace #"@@orig-doc@@" (or (:doc orig-meta) "")))}))


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
