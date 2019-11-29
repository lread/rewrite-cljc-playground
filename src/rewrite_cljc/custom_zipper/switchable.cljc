(ns ^:no-doc rewrite-cljc.custom-zipper.switchable)


(defn ^:no-doc custom-zipper?
  [value]
  (:rewrite-cljc.custom-zipper.core/custom? value))

(defmacro defn-switchable
  [sym docstring params & body]
  (let [placeholders (repeatedly (count params) gensym)
        arglists (list params)]
    `(defn ~sym
       ~docstring
       {:arglists '~arglists}
       [~@placeholders]
       (if (custom-zipper? ~(first placeholders))
         (let [~@(interleave params placeholders)]
           ~@body)
         (~(symbol "clojure.zip" (name sym)) ~@placeholders)))))
