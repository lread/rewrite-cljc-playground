(ns ^:no-doc rewrite-clj.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [rewrite-clj.zip.editz]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-clj.zip.editz
  replace
  edit
  splice
  prefix
  suffix])
