(ns ^:no-doc rewrite-cljc.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [rewrite-cljc.zip.removez]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-cljc.zip.removez
  remove
  remove-preserve-newline])
