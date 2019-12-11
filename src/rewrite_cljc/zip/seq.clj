(ns ^:no-doc rewrite-cljc.zip.seq
  (:refer-clojure :exclude [map get assoc seq? vector? list? map? set?])
  (:require [rewrite-cljc.zip.seqz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

(import-vars
 [rewrite-cljc.zip.seqz
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
