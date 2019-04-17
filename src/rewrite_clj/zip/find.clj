(ns ^:no-doc rewrite-clj.zip.find
  (:refer-clojure :exclude [find])
  (:require [rewrite-clj.zip.findz]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-clj.zip.findz
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
