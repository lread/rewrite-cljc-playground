#!/usr/bin/env bb

(ns shadow-cljs-test
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.status :as status])

(def shadow-cljs-cfg {:deps true
                      :builds {:test {:target :node-test
                                      :output-to "target/shadow-cljs/node-test.js"
                                      :compiler-options {:warnings
                                                         ;; clj-kondo handles deprecation warnings for us
                                                         {:fn-deprecated false}}
                                      :autorun true}}})

;; Just one sanity test for now
(env/assert-min-clojure-version)
(status/line :info "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
(try
  (spit "shadow-cljs.edn" shadow-cljs-cfg)
  (shell/command ["clojure" "-M:cljs:test-common:shadow-cljs-test" "compile" "test"])
  (finally
    (fs/delete-file-recursively "shadow-cljs.edn" true)))
