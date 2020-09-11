(ns ^:no-doc rewrite-cljc.zip.find
  (:refer-clojure :exclude [find])
  (:require [rewrite-cljc.potemkin.clojure :refer [import-vars]]
            [rewrite-cljc.zip.findz]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-cljc.zip.findz
  find
  find-last-by-pos
  find-depth-first
  find-next
  find-next-depth-first
  find-tag
  find-next-tag
  find-tag-by-pos
  find-token
  find-next-token
  find-value
  find-next-value])
