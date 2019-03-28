(ns ^:no-doc rewrite-clj.impl.potemkin-cljs
  (:require [clojure.string :as string]
            [cljs.analyzer.api :as analyzer]
            [cljs.util :as util]))

(defmacro import-vars [& imports]
  `(do ~@(for [[from-ns# & defs#] imports
               from-sym# defs#]
           (let [r# (analyzer/ns-resolve from-ns# from-sym#)]
             ;;(util/debug-prn (pr-str r#))
             `(def ~(with-meta from-sym# (:meta r#)) ~(:name r#))))))

;; TODO: consider genaralizing import-vars to optionally take customization fns like these basedef ones

(defn- basedef-sym-fn [orig-sym]
  (symbol (str orig-sym "*")))

(defn- basedef-sym-meta-fn [orig-sym resolved-target]
  (update (:meta resolved-target)
          :doc
          (fn [orig-doc]
            (-> "Call zipper '@@sym@@' function directly.\n\n@@doc@@"
                (string/replace #"@@sym@@" (str orig-sym))
                (string/replace #"@@doc@@" (or orig-doc ""))))))

(defmacro import-vars-basedef [& imports]
  (let [defs# (for [[from-ns# & sym-list#] imports
                    from-sym# sym-list#]
                (let [resolved-target# (analyzer/ns-resolve from-ns# from-sym#)]
                  `(def
                     ~(with-meta
                        (basedef-sym-fn from-sym#)
                        (basedef-sym-meta-fn from-sym# resolved-target#))
                     ~(:name resolved-target#))))]
    `(do ~@defs#)))
