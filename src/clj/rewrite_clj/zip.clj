(ns rewrite-clj.zip
  (:require [rewrite-clj.internal.zip.subedit]
            [rewrite-clj.internal.potemkin :refer [import-vars]]))

;; TODO: try aliasing macros here, might work? haven't figured out how or if I can do this in cljs
(import-vars
 [rewrite-clj.internal.zip.subedit
  edit-> edit->>
  subedit-> subedit->>])

(defmacro defbase
  [sym base]
  (let [{:keys [arglists]} (meta
                            (ns-resolve
                             (symbol (namespace base))
                             (symbol (name base))))
        sym (with-meta
              sym
              {:doc (format "Directly call '%s' on the given arguments." base)
               :arglists `(quote ~arglists)})]
    `(def ~sym ~base)))
