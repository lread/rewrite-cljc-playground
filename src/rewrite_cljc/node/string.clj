(ns ^:no-doc rewrite-cljc.node.string
  (:require [rewrite-cljc.node.stringz]
            [rewrite-cljc.potemkin.clojure :refer [import-vars]]))

(set! *warn-on-reflection* true)

(import-vars
 [rewrite-cljc.node.stringz
  string-node])
