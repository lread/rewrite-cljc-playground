(ns ^:no-doc rewrite-cljc.zip.edit
  (:refer-clojure :exclude [replace])
  (:require [rewrite-cljc.potemkin.clojure :refer [import-vars]]
            [rewrite-cljc.zip.editz]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-cljc.zip.editz
  replace
  edit
  splice
  prefix
  suffix])
