(ns ^:no-doc rewrite-clj.zip.remove
  (:refer-clojure :exclude [remove])
  (:require [rewrite-clj.zip.removez]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-clj.zip.removez
  remove
  remove-preserve-newline])
