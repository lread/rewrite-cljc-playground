(ns ^:no-doc rewrite-cljc.zip.find
  (:refer-clojure :exclude [find])
  (:require [rewrite-cljc.zip.findz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
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
