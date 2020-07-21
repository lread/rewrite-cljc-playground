#!/usr/bin/env bb

(ns shadow-cljs-test
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.env :as env]
         '[helper.shell :as shell])

;; Just one sanity test for now
(status/line :info "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
(shell/command [(if (= :win (env/get-os)) "npx.cmd" "npx")
                "shadow-cljs" "compile" "test"])
