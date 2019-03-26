(ns rewrite-clj.zip
  (:require [rewrite-clj.internal.zip.subedit]
            [rewrite-clj.internal.potemkin :refer [import-vars]]))

(import-vars
 [rewrite-clj.internal.zip.subedit
  edit-> edit->>
  subedit-> subedit->>])
