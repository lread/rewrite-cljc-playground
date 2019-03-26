(ns rewrite-clj.zip
  (:require [rewrite-clj.impl.zip.subedit]
            [rewrite-clj.impl.potemkin :refer [import-vars]]))

(import-vars
 [rewrite-clj.impl.zip.subedit
  edit-> edit->>
  subedit-> subedit->>])
