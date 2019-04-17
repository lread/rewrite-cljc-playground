(ns ^:no-doc rewrite-clj.zip.seq
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-clj.zip.seqz]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-clj.zip.seqz
  seq?
  list?
  vector?
  set?
  map?
  map-seq
  map-vals
  map-keys
  map
  get
  assoc])
