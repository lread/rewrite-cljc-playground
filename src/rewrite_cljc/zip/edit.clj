(ns ^:no-doc rewrite-cljc.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [rewrite-cljc.zip.editz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-cljc.zip.editz
  replace
  edit
  splice
  prefix
  suffix])
