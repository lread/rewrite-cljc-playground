#!/usr/bin/env bb

(ns shadow-cljs-test
  (:require [babashka.classpath :as cp]
            [clojure.java.io :as io]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(def compiled-tests "target/shadow-cljs/node-test.js")

(def shadow-cljs-cfg {:deps true
                      :builds {:test {:target :node-test
                                      :output-to compiled-tests
                                      ;; shadow-cljs test runner unfortunately does not have exclude
                                      ;; nor metadata support for test selection
                                      :ns-regexp "(?<!^rewrite-cljc\\.parser-ns)-test$"
                                      :compiler-options {:warnings
                                                         ;; clj-kondo handles deprecation warnings for us
                                                         {:fn-deprecated false}}}}})

;; Just one sanity test for now
(env/assert-min-versions)
(status/line :info "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
(let [shadow-config-file (io/file "shadow-cljs.edn")]
  (.deleteOnExit shadow-config-file)
  (spit shadow-config-file shadow-cljs-cfg)
  (shell/command ["clojure" "-M:cljs:test-common:shadow-cljs-test" "compile" "test"])
  (shell/command ["node" compiled-tests])
  nil)
