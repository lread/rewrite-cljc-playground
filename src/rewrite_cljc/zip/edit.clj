(ns ^:no-doc rewrite-cljc.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [rewrite-cljc.zip.editz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-cljc.zip.editz
  replace
  edit
  splice
  prefix
  suffix])
