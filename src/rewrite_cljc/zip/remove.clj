(ns ^:no-doc rewrite-cljc.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [rewrite-cljc.potemkin.clojure :refer [import-vars]]
            [rewrite-cljc.zip.removez]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-cljc.zip.removez
  remove
  remove-preserve-newline])
