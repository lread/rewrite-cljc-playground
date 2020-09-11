#!/usr/bin/env bb

(ns cljs-watch
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell]
         '[helper.status :as status])

(status/line :info "launching figwheel main clojurescript sources")
(status/line :detail "compiling code, then opening repl, afterwich your web browser will automatically open to figwheel test run summary")
(shell/command ["clojure" "-A:test-common:cljs:fig-test"])
