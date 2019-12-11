(ns ^:no-doc rewrite-cljc.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [rewrite-cljc.zip.removez]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

(import-vars
 [rewrite-cljc.zip.removez
  remove
  remove-preserve-newline])
