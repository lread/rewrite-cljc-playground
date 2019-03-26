(ns ^:no-doc rewrite-clj.internal.custom-zipper.core)

(defmacro defn-switchable
  [sym docstring params & body]
  (let [placeholders (repeatedly (count params) gensym)]
    `(do
       (defn ~sym
         ~docstring
         [~@placeholders]
         (if (custom-zipper? ~(first placeholders))
           (let [~@(interleave params placeholders)]
             ~@body)
           (~(symbol "clojure.zip" (name sym)) ~@placeholders)))
       (alter-meta! (var ~sym) assoc :arglists '(~params)))))
