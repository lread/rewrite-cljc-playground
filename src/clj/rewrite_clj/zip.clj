(ns rewrite-clj.zip
  (:require [rewrite-clj.zip.subedit]
            [rewrite-clj.potemkin :refer [import-vars]]))

;; TODO: try aliasing macros here, might work? haven't figured out how or if I can do this in cljs
(import-vars
 [rewrite-clj.zip.subedit
  edit-> edit->>
  subedit-> subedit->>])
