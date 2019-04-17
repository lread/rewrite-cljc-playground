(ns ^:no-doc rewrite-clj.node.string
  (:require [rewrite-clj.node.stringz]
            [rewrite-clj.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-clj.node.stringz
  string-node])
