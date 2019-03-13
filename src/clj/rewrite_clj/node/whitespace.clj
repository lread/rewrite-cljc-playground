(ns ^:no-doc rewrite-clj.node.whitespace)

;; TODO: dynamics are currently duplicated from whitespace.cljs
(def ^:dynamic *newline-fn*
  "This function is applied to every newline string."
  identity)

(def ^:dynamic *count-fn*
  "This function is applied to every newline string and should produce
   the eventual character count."
  count)

(defmacro with-newline-fn
  [f & body]
  `(binding [*newline-fn* (comp *newline-fn* ~f)]
     ~@body))

(defmacro with-count-fn
  [f & body]
  `(binding [*count-fn* (comp *count-fn* ~f)]
     ~@body))
