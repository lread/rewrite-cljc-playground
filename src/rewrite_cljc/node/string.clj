(ns ^:no-doc rewrite-cljc.node.string
  (:require [rewrite-cljc.node.stringz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

;; TODO: error prone, can we implement an import-publics, at least for clj version?
(import-vars
 [rewrite-cljc.node.stringz
  string-node])
